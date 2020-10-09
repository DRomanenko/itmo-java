package ru.ifmo.rain.romanenko.tiktaktoe;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.Arrays;
import java.util.Objects;
import java.util.Scanner;

public class TikTakToeUDPServer {
    private static final int bufferSize = 1024;
    private static DatagramSocket socket;

    private static boolean getMove(final BufferedPacket packetP1, final BufferedPacket packetP2, final TikTakToe game) {
        final Scanner sc = new Scanner(packetP1.toString());
        final int x = sc.nextInt(), y = sc.nextInt();
        try {
            final Result result = game.makeMove(x, y);
            switch (result) {
                case WIN:
                    packetP1.sendMessageWithEx(socket, "E:Your Win!", "Failed to send result to P1");
                    packetP2.sendMessageWithEx(socket, "E:Your Lose!", "Failed to send result to P2");
                    break;
                case DRAW:
                    packetP1.sendMessageWithEx(socket, "E:Draw!", "Failed to send result to P1");
                    packetP2.sendMessageWithEx(socket, "E:Draw!", "Failed to send result to P2");
                    break;
                case UNKNOWN:
                    packetP1.sendMessageWithEx(socket, "G", "Failed to send G");
                    packetP2.sendMessageWithEx(socket, "S:" + x + " " + y, "Failed to send (x, y)");
                    break;
            }
            if (result == Result.WIN || result == Result.DRAW) {
                game.clear();
                return true;
            }
        } catch (final Exception e) {
            try {
                packetP1.sendMessage(socket, "B:" + e.getMessage());
            } catch (final IOException e1) {
                System.err.println("Failed to send ERROR to P1: " + e1.getMessage());
            }
        }
        return false;
    }

    public static void main(final String[] args) {
        if (args == null || args.length != 1 || Arrays.stream(args).anyMatch(Objects::isNull)) {
            System.err.println("Expected: TikTakToeServer port");
            return;
        }
        final int port = Integer.parseInt(args[0]);

        try {
            socket = new DatagramSocket(port);
        } catch (final SocketException e) {
            System.err.println("Failed to create socket on port: " + port + ": " + e.getMessage());
            return;
        }

        final BufferedPacket packetP1 = new BufferedPacket(bufferSize),
                packetP2 = new BufferedPacket(bufferSize);

        //TODO:
        // - Унифицировать на ArrayList или стеке -- доставать по два игра, когда набирается нужное число
        // - Убрать повторяющийся код с одинаковым кодом для P1 и P2
        // - С P1 и P2 должно начинаться, а не заканчиваться
        // - Добавить валидацию


        // Connection P1.
        packetP1.receiveOnSocketWithEx(socket, "Failed to connection P1");
        System.out.println("P1 connected");

        // Connection P1.
        packetP2.receiveOnSocketWithEx(socket, "Failed to connection P2");
        System.out.println("P2 connected");


        do {

            //TODO: добавить выбор стороны - быстрейшему игроку или рандомом.

            // Send player's side.
            packetP1.sendMessageWithEx(socket, "X", "Failed to send side to P1");
            packetP2.sendMessageWithEx(socket, "X", "Failed to send side to P2");
            boolean first1 = true;
            final TikTakToe game = new TikTakToe(3, 'X');
            while (!socket.isClosed()) {
                //FIXME: Убрать этот костыль

                // Move P1. Send move P1 to P2.
                if (first1) {
                    packetP1.sendMessageWithEx(socket, "Your turn", "Failed to send \"Your turn\"");
                }
                packetP1.receiveOnSocketWithEx(socket, "Failed to receive move from P1");
                if (getMove(packetP1, packetP2, game))
                    break;

                // Move P2. Send move P2 to P1.

                //packetP2.sendMessageWithEx(socket, "Your turn", "Failed to send \"Your turn\"");
                packetP2.receiveOnSocketWithEx(socket, "Failed to receive move from P2");
                if (getMove(packetP2, packetP1, game))
                    break;
                first1 = false;
            }

            //FIXME: починить поддержку бесконечной игры: почему-то всё зависает после двух ответов 'Y'

            // Read my answer. Send.
            packetP1.receiveOnSocketWithEx(socket, "Failed to receive answer from P1");
            final char ansP1 = packetP1.toString().charAt(0);
            packetP2.receiveOnSocketWithEx(socket, "Failed to receive answer from P2");
            final char ansP2 = packetP2.toString().charAt(0);
            packetP2.sendMessageWithEx(socket, String.valueOf(ansP1), "Failed to send answer to P2");
            packetP1.sendMessageWithEx(socket, String.valueOf(ansP2), "Failed to send answer to P1");
            if (ansP1 != 'Y' || ansP2 != 'Y') {
                break;
            }
        } while (true);
    }
}
