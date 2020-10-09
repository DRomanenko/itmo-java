package ru.ifmo.rain.romanenko.tiktaktoe;

import java.util.Arrays;

public class TikTakToe {
    final char EMPTY = '.';
    final char X = 'X';
    final char O = 'O';

    final int size;
    char player;
    Character[][] board;

    public TikTakToe() {
        this(3, 'X');
    }

    public TikTakToe(final Character player_) {
        this(3, player_);
    }

    public TikTakToe(final int size, final char player) {
        this.size = size;
        this.player = player;
        board = new Character[this.size][this.size];
        clear();
    }

    public void clear() {
        for (final Character[] row : board) {
            Arrays.fill(row, EMPTY);
        }
    }

    public Character getPlayer() {
        return player;
    }

    public Character[][] getBoard() {
        return board;
    }

    public Result makeMove(int x, int y) {
        if (Math.min(x, y) <= 0 || Math.max(x, y) > size) {
            throw new MyException(String.format("Coordinates must be between 1 and %d!", size));
        }

        if (board[--x][--y] != EMPTY) {
            throw new MyException("Cell must be empty!");
        }

        board[x][y] = player;
        int diagonally1 = 0, diagonally2 = 0, empty = 0;
        for (int i = 0; i < size; ++i) {
            int row = 0, col = 0;
            for (int q = 0; q < size; ++q) {
                if (board[i][q] == EMPTY) {
                    ++empty;
                }
                if (board[i][q] == player) {
                    ++row;
                }
                if (board[q][i] == player) {
                    ++col;
                }
            }
            if (row == size || col == size) {
                return Result.WIN;
            }
            if (board[i][i] == player) {
                ++diagonally1;
            }
            if (board[i][size - 1 - i] == player) {
                ++diagonally2;
            }
        }
        if (empty == 0) {
            return Result.DRAW;
        }
        if (diagonally1 == size || diagonally2 == size) {
            return Result.WIN;
        }
        player = player == X ? O : X;
        return Result.UNKNOWN;
    }

    //FIXME: убрать этот страшный повторяющийся код
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder(player + "'s move! Enter row and column:\n ");
        for (int i = 0; i < size; ++i) {
            sb.append(i + 1);
        }
        for (int i = 0; i < size; ++i) {
            sb.append("\n");
            sb.append(i + 1);
            for (int q = 0; q < size; ++q) {
                sb.append(board[i][q]);
            }
        }
        return sb.toString();
    }

    public String toString(final boolean invite) {
        final String inviteToEnter = player + "'s move! Enter row and column:\n ";
        final StringBuilder sb = new StringBuilder(invite ? inviteToEnter : " ");
        for (int i = 0; i < size; ++i) {
            sb.append(i + 1);
        }
        for (int i = 0; i < size; ++i) {
            sb.append("\n");
            sb.append(i + 1);
            for (int q = 0; q < size; ++q) {
                sb.append(board[i][q]);
            }
        }
        return sb.toString();
    }
}
