package ru.ifmo.rain.romanenko.concurrent;

import info.kgeorgiy.java.advanced.concurrent.AdvancedIP;
import info.kgeorgiy.java.advanced.mapper.ParallelMapper;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * {@link List} iterative parallelism (uses treads) support, implements interface {@link AdvancedIP}.
 *
 * @author Demian Romanenko (mrnearall@gmail.com)
 * @version 1.0
 */
public class IterativeParallelism implements AdvancedIP {
    private final ParallelMapper mapper;

    /**
     * Constructor if {@link ParallelMapper} given.
     *
     * @param mapper parallel mapper.
     */
    public IterativeParallelism(final ParallelMapper mapper) {
        Objects.requireNonNull(mapper);
        this.mapper = mapper;
    }

    /**
     * Default constructors. Don't use {@link IterativeParallelism}.
     * {@link #mapper} is null
     */
    public IterativeParallelism() {
        mapper = null;
    }

    private static <T> List<Stream<T>> split(final int threads, final List<T> values) {
        final List<Stream<T>> parts = new ArrayList<>();
        final int blockSize = values.size() / threads;
        final int tail = values.size() % threads;

        int start = 0;
        for (int i = 0; i < threads; i++) {
            final int currentBlockSize = blockSize + (i < tail ? 1 : 0);
            if (currentBlockSize > 0) {
                parts.add(values.subList(start, start + currentBlockSize).stream());
                start += currentBlockSize;
            }
        }
        return parts;
    }

    private <T, P, R> R parallelAction(int threads,
                                       final List<T> values,
                                       final Function<Stream<T>, P> action,
                                       final Function<Stream<P>, R> reducer) throws InterruptedException {
        if (threads < 0) {
            throw new IllegalArgumentException("Number of threads must be positive");
        }
        threads = Math.min(threads, values.size());

        final List<Stream<T>> parts = split(threads, values);
        return reducer.apply((mapper == null ? processRunner(action, parts) : mapper.map(action, parts)).stream());
    }

    private static <T, R> List<R> processRunner (final Function<? super Stream<T>, R> mapper,
                                                 final List<Stream<T>> parts) throws InterruptedException {
        final List<R> partialResultValues = new ArrayList<>(Collections.nCopies(parts.size(), null));
        final List<Thread> workers = IntStream.range(0, parts.size())
                .mapToObj(i -> new Thread(() ->
                        partialResultValues.set(i, mapper.apply(parts.get(i)))))
                .collect(Collectors.toList());
        workers.forEach(Thread::start);
        joinThreads(workers);
        return partialResultValues;
    }

     private static void joinThreads(final List<Thread> workers) throws InterruptedException {
        for (int i = 0; i < workers.size(); i++) {
            try {
                workers.get(i).join();
            } catch (final InterruptedException e) {
                final InterruptedException exception = new InterruptedException("Some threads didn't end");
                exception.addSuppressed(e);
                for (int j = i; j < workers.size(); j++) {
                    workers.get(j).interrupt();
                }
                for (int j = i; j < workers.size(); j++) {
                    try {
                        workers.get(j).join();
                    } catch (final InterruptedException er) {
                        exception.addSuppressed(er);
                        j--;
                    }
                }
                throw exception;
            }
        }
    }

    static void joinThreadsWithIgnoredEx(final List<Thread> workers) {
        try {
            joinThreads(workers);
        } catch (final InterruptedException ignored) {
            // ignored
        }
    }

    @Override
    public String join(final int threads, final List<?> values) throws InterruptedException {
        return parallelAction(threads, values,
                s -> s.map(Object::toString).collect(Collectors.joining()),
                s -> s.collect(Collectors.joining()));
    }

    private <T, U> List<U> toList(final int threads,
                                  final List<? extends T> values,
                                  final Function<Stream<? extends T>, Stream<? extends U>> transform)
            throws InterruptedException {
        return parallelAction(threads, values,
                s -> transform.apply(s).collect(Collectors.toList()),
                streams -> streams.flatMap(List::stream).collect(Collectors.toList()));
    }

    @Override
    public <T> List<T> filter(final int threads, final List<? extends T> values, final Predicate<? super T> predicate) throws InterruptedException {
        return toList(threads, values, s -> s.filter(predicate));
    }

    @Override
    public <T, U> List<U> map(final int threads, final List<? extends T> values, final Function<? super T, ? extends U> f) throws InterruptedException {
        return toList(threads, values, s -> s.map(f));
    }

    @Override
    public <T> T maximum(final int threads, final List<? extends T> values, final Comparator<? super T> comparator) throws InterruptedException {
        return parallelAction(threads, values, s -> s.max(comparator).orElse(null), s -> s.max(comparator).orElse(null)); // get
    }

    @Override
    public <T> T minimum(final int threads, final List<? extends T> values, final Comparator<? super T> comparator) throws InterruptedException {
        return maximum(threads, values, comparator.reversed());
    }

    @Override
    public <T> boolean all(final int threads, final List<? extends T> values, final Predicate<? super T> predicate) throws InterruptedException {
        return parallelAction(threads, values, s -> s.allMatch(predicate), s -> s.allMatch(Boolean::booleanValue));

    }

    @Override
    public <T> boolean any(final int threads, final List<? extends T> values, final Predicate<? super T> predicate) throws InterruptedException {
        return !all(threads, values, predicate.negate());
    }

    private static <T> Function<Stream<T>, T> getMapReduce(final Monoid<T> monoid) {
        return s -> s.reduce(monoid.getIdentity(), monoid.getOperator());
    }

    @Override
    public <T> T reduce(final int threads, final List<T> values, final Monoid<T> monoid) throws InterruptedException {
        return parallelAction(threads, values, getMapReduce(monoid), getMapReduce(monoid));
    }

    @Override
    public <T, R> R mapReduce(final int threads, final List<T> values, final Function<T, R> lift, final Monoid<R> monoid) throws InterruptedException {
        final Function<Stream<T>, R> reducer = s -> s.map(lift).reduce(monoid.getIdentity(), monoid.getOperator());
        return parallelAction(threads, values, reducer, getMapReduce(monoid));
    }
}
