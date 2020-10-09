package ru.ifmo.rain.romanenko.tiktaktoe;

import java.net.*;
import java.util.Arrays;
import java.util.Objects;
import java.util.Scanner;

public class TikTakToeUDPClient {
    private static final int SOCKET_TIMEOUT_IN_MILLISECONDS = 350;
    private static final Scanner sc = new Scanner(System.in);
    private static final int bufferSize = 1024;

    public static void main(final String[] args) {
        if (args == null || args.length != 2 || Arrays.stream(args).anyMatch(Objects::isNull)) {
            System.err.println("Expected: TikTakToeUDPClient (name|ip) port");
            return;
        }

        final String host = args[0];
        final int port = Integer.parseInt(args[1]);
        try {
            final SocketAddress socketAddress = new InetSocketAddress(InetAddress.getByName(host), port);
            try (final DatagramSocket socket = new DatagramSocket()) {
                //socket.setSoTimeout(SOCKET_TIMEOUT_IN_MILLISECONDS);
                final BufferedPacket packet = new BufferedPacket(bufferSize, socketAddress);

                do {
                    // Connection to server.
                    packet.sendMessageWithEx(socket, String.valueOf(Math.random() * 10 + 0), "Failed to connection");

                    // Receive player's side.
                    packet.receiveOnSocketWithEx(socket, "Failed to receive side");

                    final TikTakToe game = new TikTakToe(3, Character.toUpperCase(packet.toString().charAt(0)));
                    //FIXME: Сделать разный вывод для противников.
                    System.out.println(game.toString());
                    while (!socket.isClosed()) {
                        // Receive permission to move.
                        packet.receiveOnSocketWithEx(socket, "Failed to receive move");
                        String receivedMessage = packet.toString();

                        if (receivedMessage.charAt(0) == 'S') {
                            final Scanner help = new Scanner(receivedMessage.substring(2));
                            final int x = help.nextInt(), y = help.nextInt();
                            game.makeMove(x, y);
                            System.out.println(game.toString());
                        } else if (receivedMessage.charAt(0) == 'E') { //TODO: убрать и унифицировать с E в case
                            System.out.println(receivedMessage.substring(2));
                            game.clear();
                            break;
                        } else if (!"Your turn".equals(receivedMessage)) {
                            System.err.println("Unknown message from server: " + receivedMessage);
                        }

                        // Read move. Send.
                        final int x = sc.nextInt(), y = sc.nextInt();
                        packet.sendMessageWithEx(socket, x + " " + y, "Failed to send (x, y)");

                        // Receive result.
                        packet.receiveOnSocketWithEx(socket, "Failed to receive result");
                        receivedMessage = packet.toString();

                        // Process result.
                        switch (receivedMessage.charAt(0)) {
                            case 'G': // Good move. Everything is good. -- G
                                game.makeMove(x, y);
                                System.out.println(game.toString(false));
                                break;
                            case 'B': // Bad Move.                      -- B:ERROR
                            case 'E': // End of game. Win, Draw, Lose.  -- E:Your Win/Draw/Your Lose!
                                System.out.println(receivedMessage.substring(2));
                                break;
                        }
                        if ('E' == receivedMessage.charAt(0)) {
                            game.clear();
                            break;
                        }
                    }
                    System.out.println("Want to play another one (Enter Y or N)?");
                    // Read my answer. Send.
                    final char myAns = Character.toUpperCase(sc.next().charAt(0));
                    packet.sendMessageWithEx(socket, String.valueOf(myAns), "Failed to send my answer");

                    // Receive other answer.
                    packet.receiveOnSocketWithEx(socket, "Failed to receive answer");
                    final char otherAns = packet.toString().charAt(0);
                    System.out.println("Other player enter: " + otherAns);

                    if (myAns != 'Y' || otherAns != 'Y') {
                        break;
                    }
                } while (true);
            } catch (final SocketException e) {
                System.err.println("Failed to establish connection via socket: " + e.getMessage());
            }
        } catch (final UnknownHostException e) {
            System.err.println("Unknown host: " + host + ":" + e.getMessage());
        }
    }
}
