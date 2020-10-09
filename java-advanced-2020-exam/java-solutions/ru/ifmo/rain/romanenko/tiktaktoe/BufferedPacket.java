package ru.ifmo.rain.romanenko.tiktaktoe;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;

public class BufferedPacket {
    //TODO: Перенести сокет сюда, если не буду многопоточность и ловить много игр

    private final DatagramPacket packet;
    private final byte[] buffer;

    public BufferedPacket(final int bufferSize) {
        buffer = new byte[bufferSize];
        packet = new DatagramPacket(buffer, bufferSize);
    }

    public BufferedPacket(final int bufferSize, final SocketAddress socketAddress) {
        buffer = new byte[bufferSize];
        packet = new DatagramPacket(buffer, buffer.length, socketAddress);
    }

    @Override
    public String toString() {
        return new String(packet.getData(), packet.getOffset(), packet.getLength(), StandardCharsets.UTF_8);
    }

    public void sendMessage(final DatagramSocket socket, final String data) throws IOException {
        packet.setData(data.getBytes(StandardCharsets.UTF_8));
        socket.send(packet);
    }

    public void sendMessageWithEx(final DatagramSocket socket, final String data, final String ex) {
        try {
            this.sendMessage(socket, data);
        } catch (final IOException e) {
            System.err.println(ex + ": " + e.getMessage());
        }
    }

    public void receiveOnSocket(final DatagramSocket socket) throws IOException {
        packet.setData(buffer);
        packet.setLength(buffer.length);
        socket.receive(packet);
    }

    public void receiveOnSocketWithEx(final DatagramSocket socket, final String ex) {
        try {
            this.receiveOnSocket(socket);
        } catch (final IOException e) {
            System.err.println(ex + ": " + e.getMessage());
        }
    }
}
