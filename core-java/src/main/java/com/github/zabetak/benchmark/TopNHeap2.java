package com.github.zabetak.benchmark;

/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to you under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.util.Comparator;
import java.util.function.Function;

/**
 * Implementation of a stable binary heap with a fetch and an offset. Stable
 * means that if two items are considered equal, they will appear in the same
 * order as they were offered to the heap.
 *
 * @param <TSource> type of the element that will be added to the heap
 * @param <TKey> type of the key, which the comparator will use for comparisons
 */
public class TopNHeap2<TSource, TKey> {
    static final int MAX_INIT_ARRAY_SIZE = 1024;
    static final int ROOT = 1;

    private final Function<TSource, TKey> keyFn;
    private final Comparator<TKey> cmp;
    private final int fetch;
    private final int offset;
    private final int maxSize;

    TKey headKey = null;
    int size = 0;
    int time = -Integer.MIN_VALUE;

    /** Heap with 1-based index, heap[0] is not used */
    TSource[] heap;
    /** Stores the order of arrival of the elements in the heap */
    int[] order;
    byte[] smallerChild;

    final byte LEFT_IS_SMALLER = 1;
    final byte RIGHT_IS_SMALLER = 2;
    final byte UNKNOWN = 0;

    public TopNHeap2(Function<TSource, TKey> keySelector, Comparator<TKey> comparator, int fetch, int offset) {
        this.keyFn = keySelector;
        this.cmp = comparator;
        long tmp = (long) fetch + offset;
        this.maxSize = tmp > Integer.MAX_VALUE - ROOT ? Integer.MAX_VALUE - ROOT : (int) tmp;

        //
        int arrayLen = this.maxSize + ROOT;
        if (arrayLen > 2 * MAX_INIT_ARRAY_SIZE) {
            arrayLen = MAX_INIT_ARRAY_SIZE;
        }

        @SuppressWarnings("unchecked")
        TSource[] t = (TSource[]) new Object[arrayLen];
        this.heap = t;
        this.order = new int[arrayLen];
        this.smallerChild = new byte[arrayLen];
        this.fetch = fetch;
        this.offset = offset;
    }

    /**
     * Offers a new item to the heap.
     */
    public void offer(TSource o) {
        // if size < maxSize
        if (this.size < this.maxSize) {
            this.size++;
            if (this.size >= this.heap.length) {
                this.growSize();
            }

            this.shiftUp(this.size, o, this.time++);
            return;
        }

        // check head
        TKey elKey = this.keyFn.apply(o);
        int c = this.cmp.compare(elKey, this.headKey);
        if (c >= 0) {
            return;
        }

        this.shiftDown(ROOT, o, this.time++, elKey);
    }

    public void debug() {
        System.out.println("\nHEAP:");
        for (int i = 0; i < heap.length; i++) {
            System.out.println(heap[i] + "\t" + order[i] + "\t" + smallerChild[i]);
        }
    }

    /**
     * Returns an array with at most 'fetch' entries, that contains the sorted
     * entries of the items added to the heap, while skipping the first 'offset'
     * elements.
     */
    public Object[] getResult() {
        int len = Math.min(this.size - this.offset, this.fetch);
        len = Math.max(0, len);
        @SuppressWarnings("unchecked")
        TSource[] result = (TSource[]) new Object[len];
        for (int i = result.length - 1; i >= 0; i--) {
            result[i] = this.poll();
        }
        return result;
    }

    // --------------------------------------------------------------------------------
    // --------------------------------------------------------------------------------

    private void growSize() {
        int newLen = (int) Math.min(this.maxSize, this.heap.length * 2L) + ROOT;
        if (newLen * 2 > this.maxSize) {
            newLen = this.maxSize + ROOT;
        }

        @SuppressWarnings("unchecked")
        TSource[] newHeap = (TSource[]) new Object[newLen];
        int[] newOrder = new int[newLen];
        byte[] newSmaller = new byte[newLen];
        System.arraycopy(this.heap, 0, newHeap, 0, this.heap.length);
        System.arraycopy(this.order, 0, newOrder, 0, this.order.length);
        System.arraycopy(this.smallerChild, 0, newSmaller, 0, this.smallerChild.length);
        this.heap = newHeap;
        this.order = newOrder;
        this.smallerChild = newSmaller;
    }

    private TSource poll() {
        if (this.size <= 0) {
            return null;
        }

        TSource result = this.heap[ROOT];
        int oldSize = this.size;
        // this.size--;
        if (oldSize > 1) {
            this.shiftDown(ROOT, null, Integer.MIN_VALUE, null);
        }
        // this.assign(oldSize, null, -1);
        return result;
    }

    private int compare(int a, int b) {
        TKey aKey = this.keyFn.apply(this.heap[a]);
        TKey bKey = this.keyFn.apply(this.heap[b]);

        int c = this.cmp.compare(aKey, bKey);
        if (c == 0) {
            return Integer.compare(this.order[a], this.order[b]);
        }

        return c;
    }

    private boolean lessThan(int a, int b) {
        return this.compare(a, b) > 0;
    }

    private void assign(int dst, int src) {
        this.assign(dst, this.heap[src], this.order[src]);
    }

    private void assign(int dst, TSource o, int time) {
        if (dst == ROOT) {
            this.headKey = o == null ? null : this.keyFn.apply(o);
        }
        this.heap[dst] = o;
        this.order[dst] = time;
    }

    private void shiftUp(int i, TSource toMove, int time) {
        if (i == ROOT) {
            this.assign(ROOT, toMove, time);
            return;
        }

        TKey movedKey = this.keyFn.apply(toMove);

        int j = i;
        do {
            int parent = j >>> 1;

            TKey parentKey = this.keyFn.apply(this.heap[parent]);

            int c = this.cmp.compare(movedKey, parentKey);
            if (c < 0 || (c == 0 && time < this.order[parent])) {
                break;
            }

            this.assign(j, parent);
            this.smallerChild[parent] = j % 2 == 0 ? LEFT_IS_SMALLER : RIGHT_IS_SMALLER;

            j = parent;
        } while (j > ROOT);
        this.assign(j, toMove, time);
        this.smallerChild[j >>> 1] = UNKNOWN;
    }

    private void shiftDown(int i, TSource toMove, int time, TKey toMoveKey) {
        if (toMoveKey == null && toMove != null) {
            toMoveKey = this.keyFn.apply(toMove);
        }

        int j = i;
        do {
            byte smaller;
            int leftChild = j << 1;
            if (leftChild > this.size)
                break;

            int rightChild = leftChild + 1;
            if (rightChild > this.size || this.heap[rightChild] == null)
                smaller = LEFT_IS_SMALLER;
            else if (this.heap[leftChild] == null)
                smaller = RIGHT_IS_SMALLER;
            else
                smaller = this.smallerChild[j];

            int smallerIdx;
            byte newSmaller = smaller;
            if (smaller == UNKNOWN) {
                if (this.lessThan(rightChild, leftChild)) {
                    smallerIdx = rightChild;
                    newSmaller = RIGHT_IS_SMALLER;
                } else {
                    smallerIdx = leftChild;
                    newSmaller = LEFT_IS_SMALLER;
                }
            } else
                smallerIdx = smaller == LEFT_IS_SMALLER ? leftChild : rightChild;

            TKey smallerKey = this.keyFn.apply(this.heap[smallerIdx]);
            int c = toMoveKey == null ? -1 : this.cmp.compare(toMoveKey, smallerKey);

            if (c > 0 || (c == 0 && time > this.order[smallerIdx])) {
                if (smaller == UNKNOWN)
                    this.smallerChild[j] = newSmaller;
                break;
            }

            this.assign(j, smallerIdx);
            this.smallerChild[j] = UNKNOWN;
            j = smallerIdx;
        } while (true);
        this.assign(j, toMove, time);
    }

}
