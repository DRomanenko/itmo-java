package ru.ifmo.rain.romanenko.hello;

import info.kgeorgiy.java.advanced.hello.HelloServer;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class HelloUDPNonblockingServer extends HelloUDPServer implements HelloServer {
    private static final int EXECUTOR_TIMEOUT_IN_SECONDS = 1;

    private Selector selector;
    private ExecutorService workers;
    private DatagramChannel channel;

    private int bufferSize;
    private final ConcurrentLinkedQueue<Client> tasks = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<Client> clients = new ConcurrentLinkedQueue<>();

    private class Client {
        public String text;
        public ByteBuffer buffer;
        public SocketAddress socketAddress;

        public Client() {
            buffer = ByteBuffer.allocate(bufferSize);
        }
    }

    // :NOTE: Не выделен общий код со старой реализацией (пункт 5 задания)
    public static void main(final String[] args) {
        try (final HelloUDPServer helloUDPServer = new HelloUDPNonblockingServer()) {
            if (args == null || args.length != 2 || Arrays.stream(args).anyMatch(Objects::isNull)) {
                System.err.println("Expected: HelloUDPServer port threads");
                return;
            }
            try {
                final int port = Integer.parseInt(args[0]);
                final int threads = Integer.parseInt(args[1]);
                helloUDPServer.start(port, threads);
                // :NOTE: Особенно интересно будет, если консоль не UTF-8
                final BufferedReader reader = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
                reader.readLine();
            } catch (final NumberFormatException e) {
                System.err.println("All args must be integers: " + e.getMessage());
            }
        } catch (final IOException e) {
            System.err.println("Wrong read line: " + e.getMessage());
        }
    }

    @Override
    public void start(final int port, final int threads) {
        try {
            selector = Selector.open();
            try {
                channel = DatagramChannel.open();
                channel.socket().bind(new InetSocketAddress(port));
            } catch (final SocketException e) {
                System.err.println("The socket is already open on this port: " + e.getMessage());
            }
            // :NOTE: NullPointerException
            channel.configureBlocking(false);
            channel.register(selector, SelectionKey.OP_READ);
            bufferSize = channel.getOption(StandardSocketOptions.SO_RCVBUF);
        } catch (final IOException e) {
            System.err.println(e.getMessage());
        }
        for (int i = 0; i < threads; ++i) {
            clients.add(new Client());
        }
        workers = Executors.newFixedThreadPool(threads + 1);
        workers.submit(() -> {
            try {
                while (!Thread.interrupted()) {
                    selector.select();
                    for (final Iterator<SelectionKey> i = selector.selectedKeys().iterator(); i.hasNext(); ) {
                        final SelectionKey key = i.next();
                        try {
                            if (key.isWritable()) {
                                final Client client = Objects.requireNonNull(tasks.poll());
                                channel.send(client.buffer, client.socketAddress);
                                clients.add(client);
                                if (tasks.isEmpty()) {
                                    // :NOTE: Синхронизация interestOpsAnd не требуется
                                    synchronized (this) {
                                        key.interestOpsAnd(~SelectionKey.OP_WRITE);
                                    }
                                }
                                if ((key.interestOps() & SelectionKey.OP_READ) == 0) {
                                    // :NOTE: Синхронизация interestOpsOr не требуется
                                    synchronized (this) {
                                        key.interestOpsOr(SelectionKey.OP_READ);
                                    }
                                }
                            }
                            if (key.isValid() && key.isReadable()) {
                                final Client client = Objects.requireNonNull(clients.poll());
                                client.buffer.clear();
                                client.socketAddress = channel.receive(client.buffer);
                                if (clients.isEmpty()) {
                                    // :NOTE: Синхронизация interestOpsAnd не требуется
                                    synchronized (this) {
                                        key.interestOpsAnd(~SelectionKey.OP_READ);
                                    }
                                }
                                workers.submit(() -> makeResponse(client, key));
                            }
                        } finally {
                            i.remove();
                        }
                    }

                }
            } catch (final IOException e) {
                System.err.println(e.getMessage());
            }
        });
    }

    private void makeResponse(final Client client, final SelectionKey key) {
        client.buffer.flip();
        client.text = "Hello, " + StandardCharsets.UTF_8.decode(client.buffer);
        client.buffer.clear();
        client.buffer.put(client.text.getBytes(StandardCharsets.UTF_8));
        client.buffer.flip();
        synchronized (this) {
            tasks.add(client);
            key.interestOps(SelectionKey.OP_READ | SelectionKey.OP_WRITE);
        }
        // :NOTE: Надо делать далеко не всегда
        selector.wakeup();
    }

    private void close(final Closeable closeable) {
        try {
            closeable.close();
        } catch (final IOException e) {
            System.err.println(e.getMessage());
        }
    }

    @Override
    public void close() {
        if (channel.isOpen()) {
            close(channel);
        }
        if (selector.isOpen()) {
            close(selector);
        }
        workers.shutdown();
        try {
            workers.awaitTermination(EXECUTOR_TIMEOUT_IN_SECONDS, TimeUnit.SECONDS);
        } catch (final InterruptedException e) {
            System.err.println("Termination failed: " + e.getMessage());
        }
    }
}
