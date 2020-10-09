package ru.ifmo.rain.romanenko.arrayset;

import java.util.*;

public class ReversedList<T> extends AbstractList<T> implements RandomAccess {
    private final List<T> data;
    private final boolean reversed;

    public ReversedList(final List<T> other, final boolean reversed) {
        this.data = Collections.unmodifiableList(other);
        this.reversed = reversed;
    }

    public boolean getReversed() {
        return reversed;
    }

    private int getIndex(final int index) {
        return reversed ? size() - 1 - index : index;
    }

    @Override
    public T get(final int index) {
        return data.get(getIndex(index));
    }

    @Override
    public int size() {
        return data.size();
    }
}
