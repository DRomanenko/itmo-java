package ru.ifmo.rain.romanenko.arrayset;

import java.util.*;

public class ArraySet<T> extends AbstractSet<T> implements NavigableSet<T> {
    private final List<T> data;
    private final Comparator<? super T> comparator;

    private ArraySet(final List<T> data, final Comparator<? super T> comparator) {
        this.data = data;
        this.comparator = comparator;
    }

    public ArraySet() {
        this(Collections.emptyList(), null);
    }

    public ArraySet(final Comparator<? super T> comparator) {
        this(List.of(), comparator);
    }

    public ArraySet(final Collection<? extends T> other) {
        this(other, null);
    }

    public ArraySet(final Collection<? extends T> other, final Comparator<? super T> comparator) {
        final TreeSet<T> tmp = new TreeSet<>(comparator);
        tmp.addAll(other);
        this.data = List.copyOf(tmp);
        this.comparator = comparator;
    }

    @Override
    public boolean isEmpty() {
        return data.isEmpty();
    }

    private int binSearch(final T t) {
        return Collections.binarySearch(data, Objects.requireNonNull(t), comparator);
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean contains(final Object o) {
        return binSearch((T) Objects.requireNonNull(o)) >= 0;
    }

    @Override
    public Iterator<T> iterator() {
        return data.iterator();
    }

    @Override
    public int size() {
        return data.size();
    }

    private boolean checkIndex(final int ind) {
        return 0 <= ind && ind < data.size();
    }

    private int getIndex(final T t, final int deltaFound, final int deltaNotFound) {
        int result = binSearch(Objects.requireNonNull(t));
        result = (result < 0) ? -result - 1 + deltaNotFound :  result + deltaFound;
        return checkIndex(result) ? result : -1;
    }

    private T getElement(final int ind) {
        if (ind == -1) {
            return null;
        }
        return data.get(ind);
    }

    private int lowerIndex(final T t) {
        return getIndex(t, -1, -1);
    }

    private int floorIndex(final T t) {
        return getIndex(t, 0, -1);
    }

    private int ceilingIndex(final T t) {
        return getIndex(t, 0, 0);
    }

    private int higherIndex(final T t) {
        return getIndex(t, 1, 0);
    }

    @Override
    public T lower(final T t) {
        return getElement(lowerIndex(t));
    }

    @Override
    public T floor(final T t) {
        return getElement(floorIndex(t));
    }

    @Override
    public T ceiling(final T t) {
        return getElement(ceilingIndex(t));
    }

    @Override
    public T higher(final T t) {
        return getElement(higherIndex(t));
    }

    @Override
    public T pollFirst() {
        throw new UnsupportedOperationException();
    }

    @Override
    public T pollLast() {
        throw new UnsupportedOperationException();
    }

    @Override
    public NavigableSet<T> descendingSet() {
        final boolean reversed = !(data instanceof ReversedList) || ((ReversedList<T>) data).getReversed();
        return new ArraySet<>(new ReversedList<>(data, reversed), Collections.reverseOrder(comparator));
    }

    @Override
    public Iterator<T> descendingIterator() {
        return descendingSet().iterator();
    }

    @Override
    public NavigableSet<T> subSet(final T fromElement, final boolean fromInclusive, final T toElement, final boolean toInclusive) {
        final int l = fromInclusive ? ceilingIndex(fromElement) : higherIndex(fromElement);
        final int r = toInclusive ? floorIndex(toElement) : lowerIndex(toElement);
        if (l == -1 || r == -1 || l > r) {
            return new ArraySet<>(comparator);
        }
        return new ArraySet<>(data.subList(l, r + 1), comparator);
    }

    @Override
    public NavigableSet<T> headSet(final T toElement, final boolean inclusive) {
        return isEmpty() ? this : subSet(first(), true, toElement, inclusive);
    }

    @Override
    public NavigableSet<T> tailSet(final T fromElement, final boolean inclusive) {
        return isEmpty() ? this : subSet(fromElement, inclusive, last(), true);
    }

    @SuppressWarnings("unchecked")
    private int compare(final T fromElement, final T toElement) {
        return (comparator == null) ? ((Comparable<T>) fromElement).compareTo(toElement) : comparator.compare(fromElement, toElement);
    }

    @Override
    public SortedSet<T> subSet(final T fromElement, final T toElement) throws IllegalArgumentException {
        if (compare(fromElement, toElement) > 0) {
            throw new IllegalArgumentException("Error: The left border should be less than the right");
        }
        return subSet(fromElement, true, toElement, false);
    }

    @Override
    public SortedSet<T> headSet(final T toElement) {
        return headSet(toElement, false);
    }

    @Override
    public SortedSet<T> tailSet(final T fromElement) {
        return tailSet(fromElement, true);
    }

    @Override
    public Comparator<? super T> comparator() {
        return comparator;
    }

    private void checkNotEmpty() {
        if (data.isEmpty()) {
            throw new NoSuchElementException();
        }
    }

    @Override
    public T first() {
        checkNotEmpty();
        return data.get(0);
    }

    @Override
    public T last() {
        checkNotEmpty();
        return data.get(size() - 1);
    }
}
