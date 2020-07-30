package com.github.zabetak.benchmark;

import java.lang.reflect.Array;
import java.util.*;

import com.github.zabetak.benchmark.PartialSortBenchmark.Record;

public class LimitSort<E> {

    private static int INIT_ARRAY = 512 / 2;

    private Object[] content;
    private Comparator<? super Object> cmp;
    private int limit;
    private int size = 0;

    static class Etmp {
        Object o;
        int idx;

        @Override
        public String toString() {
            return idx + ":" + o;
        }
    }

    private List<Etmp> toInsert = new ArrayList<>();

    private Comparator<Etmp> cmpTmp = (a, b) -> {
        int c = Integer.compare(a.idx, b.idx);
        if (c != 0)
            return c;
        return cmp.compare(a.o, b.o);
    };

    /** Beginning of array is sorted up to (excluding) this index */
    private int completeUntil = 0;
    private int completeFrom;
    private int tail;

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

    public void offer(E e) {
        if (size < limit) {
            ensureCapacity(size + 1);

            content[size++] = e;
            tail++;
            return;
        }

        if (tail < completeFrom) {
            sort();
        }

        if (cmp.compare(content[tail], e) <= 0) {
            return;
        }

        // we need to make space
        int idx = Arrays.binarySearch(content, 0, tail, e, (a, b) -> {
            int c = cmp.compare(a, b);
            if (c == 0)
                return -1;
            return c;
        });
        if (idx < 0)
            idx = -idx - 1;

        if (idx == tail && idx > completeFrom) {
            content[tail] = e;
            return;
        }

        completeUntil = Math.min(completeUntil, idx);
        completeFrom = Math.max(completeFrom, idx);

        content[tail--] = null;
        Etmp n = new Etmp();
        n.idx = idx;
        n.o = e;
        toInsert.add(n);
    }

    private void markSorted() {
        completeUntil = size;
        tail = size - 1;
        completeFrom = 0;
    }

    private void sort() {
        if (this.toInsert.isEmpty()) {
            Arrays.sort(content, 0, size, cmp);
            markSorted();
            return;
        }

        toInsert.sort(cmpTmp);

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
        if (this.size == content.length) {
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

            if (i == completeUntil) {
                System.out.print(" <" + completeUntil + " ");
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
