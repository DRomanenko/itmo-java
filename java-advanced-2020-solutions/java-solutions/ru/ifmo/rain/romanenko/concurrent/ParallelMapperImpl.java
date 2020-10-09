package ru.ifmo.rain.romanenko.concurrent;

import info.kgeorgiy.java.advanced.concurrent.AdvancedIP;
import info.kgeorgiy.java.advanced.mapper.ParallelMapper;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * {@link List} iterative parallelism (uses treads) support, implements interface {@link AdvancedIP}.
 *
 * @author Demian Romanenko (mrnearall@gmail.com)
 * @version 0.9
 */
public class ParallelMapperImpl implements ParallelMapper {
    private final List<Thread> workers;
    private final SynchronizedQueue tasks = new SynchronizedQueue();
    private int remaining;

    private static class SynchronizedQueue {
        private final Queue<Runnable> tasks = new ArrayDeque<>();
        private boolean closed = false;

        synchronized Runnable poll() throws InterruptedException {
            while (tasks.isEmpty()) {
                wait();
            }
            return tasks.poll();
        }

        synchronized void add(final Runnable task) {
            if (!closed) {
                tasks.add(task);
                notify();
            }
        }

        synchronized boolean isClosed() {
            return closed;
        }

        synchronized void close() {
            closed = true;
        }

        synchronized Queue<Runnable> getRemaining() {
            return tasks;
        }
    }

    private class SynchronizedTasks<R> {
        private int remaining;
        private final List<R> data;
        private RuntimeException exception = null;

        <T> SynchronizedTasks(final Function<? super T, ? extends R> f, final List<? extends T> args) {
            remaining = args.size();
            data = new ArrayList<>(Collections.nCopies(args.size(), null));
            for (int i = 0; i < args.size(); i++) {
                final int ind = i;
                tasks.add(() -> {
                    try {
                        if (!tasks.isClosed()) {
                            // :NOTE: Изменения после возврата значения
                            set(ind, f.apply(args.get(ind)));
                        }
                    } catch (final RuntimeException e) {
                        setException(e);
                    }
                    shutdown();
                });
            }
        }

        // :NOTE: Неудачное имя
        synchronized void shutdown() {
            if(--remaining == 0) {
                notify();
            }
        }

        synchronized List<R> getData() throws InterruptedException {
            while (remaining > 0) {
                wait();
            }
            if (exception != null) {
                throw exception;
            }
            return data;
        }

        synchronized void setException(final RuntimeException e) {
            if (exception == null) {
                exception = e;
            } else {
                exception.addSuppressed(e);
            }
        }

        synchronized void set(final int pos, final R value) {
            data.set(pos, value);
        }
    }

    /**
     * Thread-count constructor. Creates a {@link ParallelMapperImpl} instance with a given of {@code threads}.
     *
     * @param threads a number of threads
     */
    public ParallelMapperImpl(final int threads) {
        if (threads <= 0) {
            throw new IllegalArgumentException("Number of threads must be positive");
        }
        final Runnable currTask = () -> {
            try {
                while (!Thread.interrupted()) {
                    tasks.poll().run();
                }
            } catch (final InterruptedException ignored) {
                // ignored
            }
        };
        workers = Stream.generate(() -> new Thread(currTask)).limit(threads).collect(Collectors.toList());
        workers.forEach(Thread::start);
    }

    @Override
    public <T, R> List<R> map(final Function<? super T, ? extends R> f,
                              final List<? extends T> args) throws InterruptedException {
        if (tasks.isClosed()) {
            throw new RuntimeException("Mapper is over");
        }
        // :NOTE: Несинхронизованное изменение
        remaining++;
        final SynchronizedTasks<R> solver = new SynchronizedTasks<>(f, args);
        synchronized (this) {
            if (--remaining == 0) {
                notify();
            }
        }
        return solver.getData();
    }

    @Override
    public void close() {
        tasks.close();
        workers.forEach(Thread::interrupt);
        IterativeParallelism.joinThreadsWithIgnoredEx(workers);
        synchronized (this) {
            while (remaining != 0) {
                try {
                    wait();
                } catch (final InterruptedException ignored) {
                    // ignored
                }
            }
        }
        tasks.getRemaining().forEach(Runnable::run);
    }
}
