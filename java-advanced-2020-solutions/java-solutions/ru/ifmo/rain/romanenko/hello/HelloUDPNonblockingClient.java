package ru.ifmo.rain.romanenko.hello;

import info.kgeorgiy.java.advanced.hello.HelloClient;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class HelloUDPNonblockingClient extends HelloUDPClient implements HelloClient {
    private static final int SELECTOR_TIMEOUT_IN_MILLISECONDS = 350;

    // :NOTE: Не выделен общий код со старой реализацией (пункт 5 задания)
    public static void main(final String[] args) {
        if (args == null || args.length != 5 || Arrays.stream(args).anyMatch(Objects::isNull)) {
            System.err.println("Expected: HelloUDPClient (name|ip) port prefix threads requests");
            return;
        }
        final HelloUDPClient helloUDPClient = new HelloUDPNonblockingClient();
        try {
            final String host = args[0];
            final int port = Integer.parseInt(args[1]);
            final String prefix = args[2];
            final int threads = Integer.parseInt(args[3]);
            final int request = Integer.parseInt(args[4]);

            helloUDPClient.run(host, port, prefix, threads, request);
        } catch (final NumberFormatException e) {
            System.err.println("Arguments are invalid: " + e.getMessage());
        }
    }

    private static class Client {
        public final int thread;
        public int count;

        public Client(final int thread) {
            this.thread = thread;
            count = 0;
        }
    }

    private void closeDatagramChannels(final List<DatagramChannel> datagramChannels) {
        for (final DatagramChannel channel : datagramChannels) {
            // :NOTE: Аналогичный кусок есть в сервере
            if (channel.isOpen()) {
                try {
                    channel.close();
                } catch (final IOException m) {
                    System.err.println(m.getMessage());
                }
            }
        }
    }

    @Override
    public void run(final String host, final int port, final String prefix, final int threads, final int requests) {
        final List<DatagramChannel> datagramChannels = new ArrayList<>();
        try (final Selector selector = Selector.open()) {
            int datagramChannelsSize = threads;
            for (int i = 0; i < threads; ++i) {
                datagramChannels.add(DatagramChannel.open());
                datagramChannels.get(i).configureBlocking(false);
                datagramChannels.get(i).register(selector, SelectionKey.OP_WRITE, new Client(i));
            }
            final ByteBuffer currentBuffer = ByteBuffer.allocate(datagramChannels.get(0).getOption(StandardSocketOptions.SO_RCVBUF));
            while (datagramChannelsSize > 0) {
                try {
                    if (selector.select(SELECTOR_TIMEOUT_IN_MILLISECONDS) == 0) {
                        selector.keys().forEach(key -> key.interestOps(SelectionKey.OP_WRITE));
                        continue;
                    }
                    for (final Iterator<SelectionKey> i = selector.selectedKeys().iterator(); i.hasNext(); ) {
                        final SelectionKey key = i.next();
                        try {
                            if (key.isReadable() || (key.isValid() && key.isWritable())) {
                                final DatagramChannel channel = (DatagramChannel) key.channel();
                                final Client client = (Client) key.attachment();
                                if (key.isReadable()) {
                                    currentBuffer.clear();
                                    if (channel.receive(currentBuffer) != null) {
                                        currentBuffer.flip();
                                        checkResponse(currentBuffer, client);
                                        key.interestOps(SelectionKey.OP_WRITE);
                                    }
                                }
                                if (key.isValid() && key.isWritable()) {
                                    if (client.count != requests) {
                                        currentBuffer.clear();
                                        currentBuffer.put((prefix + client.thread + "_" + client.count).getBytes(StandardCharsets.UTF_8));
                                        currentBuffer.flip();
                                        channel.send(currentBuffer, new InetSocketAddress(InetAddress.getByName(host), port));
                                        key.interestOps(SelectionKey.OP_READ);
                                    } else {
                                        --datagramChannelsSize;
                                        channel.close();
                                        key.cancel();
                                    }
                                }
                            }
                        } finally {
                            i.remove();
                        }
                    }
                } catch (final IOException e) {
                    System.err.println(e.getMessage());
                }
            }
        } catch (final IOException e) {
            System.err.println(e.getMessage());
        } finally {
            closeDatagramChannels(datagramChannels);
        }
    }

    private void checkResponse(final ByteBuffer currentBuffer, final Client client) {
        // :NOTE: Я ещё в 10 ДЗ за такое снижал
        if (StandardCharsets.UTF_8.decode(currentBuffer).toString().
                matches("[^0-9]*" + client.thread + "[^0-9]*" + client.count + "[^0-9]*")) {
            ++client.count;
        }
    }
}
