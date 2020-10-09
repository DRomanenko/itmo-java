package ru.ifmo.rain.romanenko.tiktaktoe;

import java.util.Scanner;

public class OfflineTikTakToe {
    private static final Scanner sc = new Scanner(System.in);

    private static char validateNext(final char a, final char b, final String message) {
        char currentChar;
        while (true) {
            currentChar = Character.toUpperCase(sc.next().charAt(0));
            if (currentChar == a || currentChar == b) {
                break;
            }
            System.out.println(message);
        }
        return currentChar;
    }

    public static void main(final String[] args) {
        do {
            System.out.println("Enter field size (2 to 9):");
            final int N = sc.nextInt();
            System.out.println("Enter your side (X or O):");
            final char player = validateNext('X', 'O', "Side can be X or O!");
            final TikTakToe game = new TikTakToe(N, player);
            System.out.println(game.toString());
            while (true) {
                final int X = sc.nextInt(), Y = sc.nextInt();
                try {
                    final Result result = game.makeMove(X, Y);
                    System.out.println(game.toString());
                    switch (result) {
                        case WIN:
                            System.out.println(game.getPlayer() + "'s player wins!");
                            break;
                        case DRAW:
                            System.out.println("Draw");
                            break;
                    }
                    if (result == Result.WIN || result == Result.DRAW) {
                        game.clear();
                        break;
                    }
                } catch (final Exception e) {
                    System.out.println(e.getMessage());
                }
            }
            System.out.println("Want to play another one (Enter Y or N)?");
        } while (validateNext('Y', 'N', "Enter Y or N!") != 'Y');
    }
}
