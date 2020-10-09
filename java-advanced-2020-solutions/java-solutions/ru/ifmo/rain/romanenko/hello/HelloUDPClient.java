package ru.ifmo.rain.romanenko.hello;

import info.kgeorgiy.java.advanced.hello.HelloClient;

import java.io.IOException;
import java.net.*;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

public class HelloUDPClient implements HelloClient {
    private static final int SOCKET_TIMEOUT_IN_MILLISECONDS = 350;
    private static final int TERMINATION_TIMEOUT_PER_REQUEST = 5;

    private int skipNotDigits(final String s, int pos) {
        while (pos < s.length() && !Character.isDigit(s.charAt(pos))) {
            ++pos;
        }
        return pos;
    }

    private int findNum(final String s, int pos, final String match) {
        pos = skipNotDigits(s, pos);
        final int l = pos;
        while (pos < s.length() && Character.isDigit(s.charAt(pos))) {
            ++pos;
        }
        return !s.substring(l, pos).equals(match) ? -1 : pos;
    }

    private boolean validateResponse(final String s, final String threadId, final String requestId) {
        int pos = findNum(s, 0, threadId);
        if (pos == -1 || (pos = findNum(s, pos, requestId)) == -1) return false;
        return skipNotDigits(s, pos) == s.length();
    }

    @Override
    public void run(final String host, final int port, final String prefix, final int threads, final int requests) {
        try {
            final SocketAddress socketAddress = new InetSocketAddress(InetAddress.getByName(host), port);
            final ExecutorService executor = Executors.newFixedThreadPool(threads);
            IntStream.range(0, threads).
                    forEach(thread -> executor.submit(() -> {
                                try (final DatagramSocket socket = new DatagramSocket()) {
                                    socket.setSoTimeout(SOCKET_TIMEOUT_IN_MILLISECONDS);
                                    final BufferedPacket bufferedPacket = new BufferedPacket(socket.getReceiveBufferSize(), socketAddress);
                                    for (int i = 0; i < requests; i++) {
                                        final String message = prefix + thread + "_" + i;
                                        System.out.println("Sending: " + message);
                                        boolean received = false;
                                        while (!received && !socket.isClosed() && !Thread.interrupted()) {
                                            // :NOTE: копипаста // solved
                                            try {
                                                bufferedPacket.sendMessage(socket, message);
                                            } catch (final IOException e) {
                                                System.err.println("Failed to send send: " + e.getMessage());
                                                continue;
                                            }
                                            try {
                                                bufferedPacket.receiveOnSocket(socket);
                                                final String response = bufferedPacket.getString();
                                                if (validateResponse(response, Integer.toString(thread), Integer.toString(i))) {
                                                    received = true;
                                                    System.out.println("Received: " + response);
                                                }
                                            } catch (final IOException e) {
                                                System.err.println("Failed to receive response: " + e.getMessage());
                                            }
                                        }
                                    }
                                } catch (final SocketException e) {
                                    System.err.println("Failed to establish connection via socket: " + e.getMessage());
                                }
                            }
                    ));
            executor.shutdown();
            executor.awaitTermination(TERMINATION_TIMEOUT_PER_REQUEST * requests * threads, TimeUnit.SECONDS);
        } catch (final UnknownHostException e) {
            System.err.println("Unknown host: " + host + ":" + e.getMessage());
        } catch (final InterruptedException e) {
            System.err.println("Termination failed: " + e.getMessage());
        }
    }

    public static void main(final String[] args) {
        if (args == null || args.length != 5 || Arrays.stream(args).anyMatch(Objects::isNull)) {
            System.err.println("Expected: HelloUDPClient (name|ip) port prefix threads requests");
            return;
        }
        try {
            final String host = args[0];
            final int port = Integer.parseInt(args[1]);
            final String prefix = args[2];
            final int threads = Integer.parseInt(args[3]);
            final int request = Integer.parseInt(args[4]);

            new HelloUDPClient().run(host, port, prefix, threads, request);
        } catch (final NumberFormatException e) {
            System.err.println("Port | threads | requests must be integer: " + e.getMessage());
        }
    }
}
