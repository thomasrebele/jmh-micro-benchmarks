package com.github.zabetak.benchmark;

import java.lang.reflect.Array;
import java.util.*;

import com.github.zabetak.benchmark.PartialSortBenchmark.Record;

public class LimitSort<E> {

    private static int INIT_ARRAY = 512 / 2;

    static class Etmp {
        Object o;
        int idx;

        @Override
        public String toString() {
            return idx + ":" + o;
        }
    }

    private Object[] content;
    private Comparator<? super Object> cmp;
    private int limit;
    private int size = 0;

    /**
     * End of array contains all relevant elements and is sorted between
     * completeFrom and tail. In other words: no elements of toInsert need to be
     * inserted between those two indices.
     */
    private int completeFrom;

    /** Marks the currently biggest element */
    private int tail;

    private List<Etmp> toInsert = new ArrayList<>();

    @SuppressWarnings("unchecked")
    LimitSort(Comparator<E> comparator, int limit) {
        this.cmp = (Comparator<? super Object>) comparator;
        this.limit = limit < 0 ? Integer.MAX_VALUE : limit;
        completeFrom = this.limit;
        tail = -1;
        content = (E[]) Array.newInstance(Record.class, getNewSize(INIT_ARRAY));
    }

    private void ensureCapacity(int i) {
        if (content != null && i < content.length)
            return;
        content = Arrays.copyOf(content, getNewSize(content.length));
    }

    private int getNewSize(int currentSize) {
        int newSize = currentSize * 2;
        if (newSize * 2 > limit)
            newSize = limit;
        else
            newSize = Math.min(this.limit, newSize);
        return newSize;
    }

    /** Searches where the object would need to be inserted into the array */
    private int binarySearch(Object o) {
        int left = 0;
        int right = tail - 1;
        while (left <= right) {
            int middle = (left + right) >>> 1;
            int c = cmp.compare(o, content[middle]);
            if (c > 0)
                left = middle + 1;
            else
                right = middle - 1;
        }
        return left;
    }

    public void offer(E e) {
        if (size < limit) {
            ensureCapacity(size + 1);
            content[size++] = e;
            tail++;
            return;
        }

        if (tail < completeFrom)
            sort();

        if (cmp.compare(content[tail], e) <= 0)
            return;

        int idx = binarySearch(e);
        // we can just replace the tail
        if (idx == tail && idx > completeFrom) {
            content[tail] = e;
            return;
        }

        // we need to make space
        completeFrom = Math.max(completeFrom, idx);
        content[tail--] = null;
        Etmp n = new Etmp();
        n.idx = idx;
        n.o = e;
        toInsert.add(n);
    }

    private void markSorted() {
        tail = size - 1;
        completeFrom = 0;
    }

    private void sort() {
        if (this.toInsert.isEmpty()) {
            Arrays.sort(content, 0, size, cmp);
            markSorted();
            return;
        }

        toInsert.sort((a, b) -> {
            int c = Integer.compare(a.idx, b.idx);
            if (c != 0)
                return c;
            return cmp.compare(a.o, b.o);
        });

        // TODO tre possible attack: force copying of a big slice after every other new
        // element. Avoid by analyzing toInsert and process a part.

        int contentOutIdx = size;
        int contentInIdx = tail + 1;
        for (int i = toInsert.size(); i-- > 0;) {
            Etmp tmp = toInsert.get(i);
            int dst = tmp.idx;
            int len = contentInIdx - dst;

            // a for small slices, a loop is quicker
            if (len <= 5) {
                for (int j = len; j-- > 0;) {
                    content[--contentOutIdx] = content[--contentInIdx];
                }
            } else {
                contentInIdx -= len;
                contentOutIdx -= len;
                System.arraycopy(content, contentInIdx, content, contentOutIdx, len);
            }

            content[--contentOutIdx] = tmp.o;
        }
        toInsert.clear();
        markSorted();
    }

    @SuppressWarnings("unchecked")
    public Iterable<E> getResult() {
        sort();
        Object[] result = content;
        content = null;
        if (this.size == result.length) {
            return (Iterable<E>) Arrays.asList(result);
        } else {
            return (Iterable<E>) Arrays.asList(result).subList(0, size);
        }
    }

    public void debug() {
        System.out.print("             ");
        for (int i = -10; i < content.length + 10; i++) {

            if (i == tail) {
                System.out.print(" #" + tail + "# ");
            }

            if (i == completeFrom) {
                System.out.print(" >=" + completeFrom + " ");
            }

            if (i >= 0 && i < content.length)
                System.out.print(content[i] + ", ");

        }

        System.out.print("   toInsert ");
        for (Etmp e : toInsert) {
            System.out.print(e + ", ");
        }

        System.out.println("");
    }
}
