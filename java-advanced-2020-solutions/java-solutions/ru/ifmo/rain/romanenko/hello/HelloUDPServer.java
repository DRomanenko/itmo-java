package ru.ifmo.rain.romanenko.hello;

import info.kgeorgiy.java.advanced.hello.HelloServer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

public class HelloUDPServer implements HelloServer, AutoCloseable {
    private static final int EXECUTOR_TIMEOUT_IN_SECONDS = 30;

    private ExecutorService executor;
    private DatagramSocket socket;

    @Override
    public void start(final int port, final int threads) {
        try {
            socket = new DatagramSocket(port);
        } catch (final SocketException e) {
            System.err.println("Failed to create socket on port: " + port + ": " + e.getMessage());
            return;
        }

        final int bufferSize;
        try {
            bufferSize = socket.getReceiveBufferSize();
        } catch (final SocketException e) {
            System.err.println("Failed to get buffer size: " + e.getMessage());
            return;
        }

        executor = Executors.newFixedThreadPool(threads);
        // :NOTE: IntStream // solved
        IntStream.range(0, threads).forEach(unused -> executor.submit(() -> {
            try {
                final BufferedPacket bufferedPacket = new BufferedPacket(bufferSize);
                while (!Thread.interrupted() && !socket.isClosed()) {
                    try {
                        bufferedPacket.receiveOnSocket(socket);
                        bufferedPacket.sendMessage(socket, "Hello, " + bufferedPacket.getString());
                    } catch (final IOException e) {
                        if (!socket.isClosed()) {
                            System.err.println("Sending failed: " + e.getMessage());
                        }
                    }
                }
            } finally {
                Thread.currentThread().interrupt();
            }
        }));
    }

    @Override
    public void close() {
        socket.close();
        executor.shutdown();
        try {
            executor.awaitTermination(EXECUTOR_TIMEOUT_IN_SECONDS, TimeUnit.SECONDS);
        } catch (final InterruptedException e) {
            System.err.println("Termination failed: " + e.getMessage());
        }
    }

    public static void main(final String[] args) {
        if (args == null || args.length != 2 || Arrays.stream(args).anyMatch(Objects::isNull)) {
            System.err.println("Expected: HelloUDPServer port threads");
            return;
        }

        try {
            final int port = Integer.parseInt(args[0]);
            final int threads = Integer.parseInt(args[1]);

            try (final HelloServer server = new HelloUDPServer()) {
                server.start(port, threads);
                final BufferedReader reader = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
                reader.readLine();
            } catch (final IOException e) {
                System.err.println("Receiving failed: " + e.getMessage());
            }
        } catch (final NumberFormatException e) {
            System.err.println("All args must be integers: " + e.getMessage());
        }
    }
}
