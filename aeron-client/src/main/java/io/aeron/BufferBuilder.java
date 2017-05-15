/*
 * Copyright 2014-2017 Real Logic Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.aeron;

import org.agrona.DirectBuffer;
import org.agrona.MutableDirectBuffer;
import org.agrona.concurrent.UnsafeBuffer;

import java.util.Arrays;

/**
 * Reusable Builder for appending a sequence of buffer fragments which grows internal capacity as needed.
 *
 * Similar in concept to {@link StringBuilder}.
 */
public class BufferBuilder
{
    /**
     * Maximum capacity to which the buffer can grow.
     */
    public static final int MAX_CAPACITY = Integer.MAX_VALUE - 8;

    /**
     * Initial capacity for the internal buffer.
     */
    public static final int INITIAL_CAPACITY = 4096;

    private final UnsafeBuffer buffer;
    private int limit = 0;

    /**
     * Construct a buffer builder with a default growth increment of {@link #INITIAL_CAPACITY}
     */
    public BufferBuilder()
    {
        this(INITIAL_CAPACITY);
    }

    /**
     * Construct a buffer builder with an initial capacity.
     *
     * @param initialCapacity at which the capacity will start.
     */
    public BufferBuilder(final int initialCapacity)
    {
        buffer = new UnsafeBuffer(new byte[initialCapacity]);
    }

    /**
     * The current capacity of the buffer.
     *
     * @return the current capacity of the buffer.
     */
    public int capacity()
    {
        return buffer.capacity();
    }

    /**
     * The current limit of the buffer that has been used by append operations.
     *
     * @return the current limit of the buffer that has been used by append operations.
     */
    public int limit()
    {
        return limit;
    }

    /**
     * Set this limit for this buffer as the position at which the next append operation will occur.
     *
     * @param limit to be the new value.
     */
    public void limit(final int limit)
    {
        if (limit < 0 || limit >= buffer.capacity())
        {
            throw new IllegalArgumentException(
                "Limit outside range: capacity=" + buffer.capacity() + " limit=" + limit);
        }

        this.limit = limit;
    }

    /**
     * The {@link MutableDirectBuffer} that encapsulates the internal buffer.
     *
     * @return the {@link MutableDirectBuffer} that encapsulates the internal buffer.
     */
    public MutableDirectBuffer buffer()
    {
        return buffer;
    }

    /**
     * Reset the builder to restart append operations. The internal buffer does not shrink.
     *
     * @return the builder for fluent API usage.
     */
    public BufferBuilder reset()
    {
        limit = 0;
        return this;
    }

    /**
     * Compact the buffer to reclaim unused space above the limit.
     *
     * @return the builder for fluent API usage.
     */
    public BufferBuilder compact()
    {
        resize(Math.max(INITIAL_CAPACITY, limit));

        return this;
    }

    /**
     * Append a source buffer to the end of the internal buffer, resizing the internal buffer as required.
     *
     * @param srcBuffer from which to copy.
     * @param srcOffset in the source buffer from which to copy.
     * @param length in bytes to copy from the source buffer.
     * @return the builder for fluent API usage.
     */
    public BufferBuilder append(final DirectBuffer srcBuffer, final int srcOffset, final int length)
    {
        ensureCapacity(length);

        buffer.putBytes(limit, srcBuffer, srcOffset, length);
        limit += length;

        return this;
    }

    private void ensureCapacity(final int additionalCapacity)
    {
        final long requiredCapacity = (long)limit + additionalCapacity;

        if (requiredCapacity > MAX_CAPACITY)
        {
            throw new IllegalStateException(
                "Max capacity exceeded: limit=" + limit + " required=" + requiredCapacity);
        }

        final int capacity = buffer.capacity();
        if (requiredCapacity > capacity)
        {
            resize(findSuitableCapacity(capacity, (int)requiredCapacity));
        }
    }

    private void resize(final int newCapacity)
    {
        buffer.wrap(Arrays.copyOf(buffer.byteArray(), newCapacity));
    }

    private static int findSuitableCapacity(final int currentCapacity, final int requiredCapacity)
    {
        int capacity = currentCapacity;

        do
        {
            final int newCapacity = capacity + (capacity >> 1);

            if (newCapacity < 0 || newCapacity > MAX_CAPACITY)
            {
                if (capacity == MAX_CAPACITY)
                {
                    throw new IllegalStateException("Max capacity reached: " + MAX_CAPACITY);
                }

                capacity = MAX_CAPACITY;
            }
            else
            {
                capacity = newCapacity;
            }
        }
        while (capacity < requiredCapacity);

        return capacity;
    }
}
