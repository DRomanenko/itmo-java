package ru.ifmo.rain.romanenko.leitner;

import java.io.Serializable;
import java.util.*;

public class Buckets implements Serializable {
    private final Map<String, String> words;
    private final Locale outLocale;
    List<List<String>> buckets;
    Random random = new Random();

    public Buckets(final int n, final Map<String, String> words, final Locale out) {
        outLocale = out;
        if (n <= 0) {
            throw new IllegalArgumentException("Number of buckets must be natural");
        }
        if (words.isEmpty()) {
            throw new IllegalArgumentException("Number of words must be natural");
        }
        this.words = words;
        buckets = new ArrayList<>(Collections.nCopies(n, new ArrayList<>()));
        buckets.get(0).addAll(words.keySet());
    }

    private int getMaxDouble() {
        var localMaxDouble = 0;
        var current = 1.5;
        for (int i = 0; i < buckets.size(); i++, current *= 1.5) {
            localMaxDouble += current * buckets.get(i).size();
        }
        return localMaxDouble;
    }

    public int getN() {
        return words.size();
    }

    static class KeyInstance {
        final int bucket, index;
        final String word;

        KeyInstance(final int bucket, final int index, final String word) {
            this.bucket = bucket;
            this.index = index;
            this.word = word;
        }
    }

    KeyInstance getInstance() {
        final var d = random.nextDouble() % getMaxDouble();
        double step = 1.5;
        double cur = 0;
        for (int i = 0; i < buckets.size(); cur += step * buckets.get(i).size(), i++, step *= 1.5) {
            if (cur + step * buckets.get(i).size() < d) {
                final var ind = Integer.min(buckets.size() - 1, (int) ((d - cur) % step));
                return new KeyInstance(i, ind, buckets.get(i).get(ind));
                //min for collision +EPSILON
            }
        }
        //last for collision
        for (int i = buckets.size() - 1; i >= 0; i--) {
            if (!buckets.get(i).isEmpty()) {
                final var j = buckets.get(i).size() - 1;
                return new KeyInstance(i, j, buckets.get(i).get(j));
            }
        }
        return null; //cannot happen because we have >=1 words
    }

    void move(final KeyInstance instance, final int bucket) {
        if (bucket < 0 || bucket >= buckets.size()) {
            throw new IndexOutOfBoundsException("Wrong bucket index");
        }
        if (bucket == instance.bucket) {
            return;
        }
        buckets
                .get(instance.index)
                .set(instance.index,
                        buckets
                                .get(instance.bucket).get(buckets.get(instance.bucket).size() - 1));
        buckets.get(instance.bucket).remove(buckets.get(instance.bucket).size() - 1);
        buckets.get(bucket).add(instance.word);
    }
}
