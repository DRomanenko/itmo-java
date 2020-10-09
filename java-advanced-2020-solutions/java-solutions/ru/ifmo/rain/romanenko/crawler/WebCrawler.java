package ru.ifmo.rain.romanenko.crawler;

import info.kgeorgiy.java.advanced.crawler.*;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;

/*
 * @author Demian Romanenko (mrnearall@gmail.com)
 * @version 1.0000000
 */
public class WebCrawler implements Crawler {
    //
    private final Downloader downloader;
    private final ExecutorService downloaders;
    private final ExecutorService extractors;
    private final int perHost;
    private final ConcurrentHashMap<String, HostDownloadersQueue> hostQueues = new ConcurrentHashMap<>();

    public WebCrawler(final Downloader downloader, final int downloaders, final int extractors, final int perHost) {
        this.downloader = downloader;
        this.downloaders = Executors.newFixedThreadPool(downloaders);
        this.extractors = Executors.newFixedThreadPool(extractors);
        this.perHost = perHost;
    }

    private class HostDownloadersQueue {
        private int nowRunning = 0;
        private final Queue<Runnable> tasks = new ArrayDeque<>();

        synchronized void add(final Runnable task) {
            tasks.add(task);
            runNext();
        }

        private synchronized void runNext() {
            if (nowRunning < perHost) {
                if (!tasks.isEmpty()) {
                    final Runnable task = tasks.poll();
                    nowRunning++;
                    downloaders.submit(() -> {
                        try {
                            task.run();
                        } finally {
                            synchronized (HostDownloadersQueue.this) {
                                nowRunning--;
                                runNext();
                            }
                        }
                    });
                }
            }
        }
    }

    @Override
    public Result download(final String url, final int depth) {
        return new Worker(url, depth).getResult();
    }

    @Override
    public void close() {
        checkedShutdown(extractors);
        checkedShutdown(downloaders);
    }

    private void checkedShutdown(final ExecutorService pool) {
        pool.shutdown();
        try {
            if (!pool.awaitTermination(30, TimeUnit.SECONDS)) {
                pool.shutdownNow();
            }
        } catch (final InterruptedException e) {
            pool.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    private class Worker {
        final Set<String> results = ConcurrentHashMap.newKeySet();
        final Map<String, IOException> errors = new ConcurrentHashMap<>();
        final Phaser phaser = new Phaser();

        Worker(final String url, final int depth) {
            phaser.register();
            download(url, depth);
            phaser.arriveAndAwaitAdvance();
            results.removeAll(errors.keySet());
        }

        private void downloadURL(final String url, final int depth, final Phaser phaser) {
            try {
                final Document page = downloader.download(url);
                if (depth > 1) {
                    phaser.register();
                    extractors.submit(() -> {
                        try {
                            page.extractLinks().forEach(link -> download(link, depth - 1));
                        } catch (final IOException e) {
                            errors.put(url, e);
                        } finally {
                            phaser.arrive();
                        }
                    });
                }
            } catch (final IOException e) {
                errors.put(url, e);
            }
        }

        private void download(final String url, final int depth) {
            if (depth == 0) {
                return;
            }
            if (results.add(url)) {
                try {
                    final HostDownloadersQueue hostQueue = hostQueues.computeIfAbsent(URLUtils.getHost(url),
                            s -> new HostDownloadersQueue());
                    phaser.register();
                    hostQueue.add(() -> {
                        try {
                            downloadURL(url, depth, phaser);
                        } finally {
                            phaser.arrive();
                        }
                    });
                } catch (final IOException e) {
                    errors.put(url, e);
                }
            }
        }

        private Result getResult() {
            return new Result(new ArrayList<>(results), errors);
        }
    }

    private static int get(final String[] args, final int ind) {
        return ind >= args.length ? 1 : Integer.parseInt(args[ind]);
    }

    public static void main(final String[] args) {
        if (args == null || args.length == 0 || args.length > 5 || Arrays.stream(args).anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("Expected: URL [depth [downloads [extractors [perHost]]]]");
        }
        try (final Crawler crawler = new WebCrawler(new CachingDownloader(), get(args, 2), get(args, 3), get(args, 4))) {
            crawler.download(args[0], get(args, 1));
        } catch (final Exception e) {
            System.err.println(e.getMessage());
        }
    }
}
