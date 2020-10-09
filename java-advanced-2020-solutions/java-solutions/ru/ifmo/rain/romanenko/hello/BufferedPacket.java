package ru.ifmo.rain.romanenko.hello;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;

/*
 * @author Demian Romanenko (mrnearall@gmail.com)
 * @version 1.00
 */
public class BufferedPacket {
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

    public String getString() {
        return new String(packet.getData(), packet.getOffset(), packet.getLength(), StandardCharsets.UTF_8);
    }

    public void sendMessage(final DatagramSocket socket, final String data) throws IOException {
        packet.setData(data.getBytes(StandardCharsets.UTF_8));
        socket.send(packet);
    }

    public void receiveOnSocket(final DatagramSocket socket) throws IOException {
        packet.setData(buffer);
        packet.setLength(buffer.length);
        socket.receive(packet);
    }
}
