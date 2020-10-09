package ru.ifmo.rain.romanenko.leitner;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class LeitnerSystem {
    private static final int BASKETS = 10;
    static double[] probabilities;
    static int curNum = 1;

    private static void readFromFile(final String path, final ArrayList<Map<String, String>> interpreter) {
        try (final BufferedReader reader = Files.newBufferedReader(Paths.get(path))) {
            String line = reader.readLine();
            while (line != null) {
                // :NOTE: - Повторные вычисления
                final String in = line.substring(0, line.lastIndexOf('-')).trim();
                final String translation = line.substring(line.lastIndexOf('-') + 1).trim();
                interpreter.get(0).put(in, translation);
                line = reader.readLine();
            }
        } catch (final IOException e) {
            e.printStackTrace();
        }
    }

    public static <T> Map.Entry<T, T> getByIndex(final Map<T, T> s, int index) {
        for (final Map.Entry<T, T> cur : s.entrySet()) {
            if (--index < 0) {
                return cur;
            }
        }
        // :NOTE: * NullPointerException
        return null;
    }

    private static Map.Entry<String, String> genRandomWord(final ArrayList<Map<String, String>> interpreter, final double acc) {
        int cur = 0;
        final int target = (int) (Math.random() * acc);
        for (int i = 0; i < BASKETS; ++i) {
            if (cur + probabilities[i] * interpreter.get(i).size() > target) {
                curNum = i;
                return getByIndex(interpreter.get(i), (int) (((cur + probabilities[i] * interpreter.get(i).size()) - target) / probabilities[i]));
            }
            cur += probabilities[i] * interpreter.get(i).size();
        }
        return null;
    }

    private static void printBaskets (final ArrayList<Map<String, String>> interpreter) {
        for (int i = 0; i < BASKETS; ++i) {
            System.out.println(i + " : " +interpreter.get(i).toString());
        }
    }

    public static void main(final String[] args) {
        if (args == null || args.length != 1 || Arrays.stream(args).anyMatch(Objects::isNull)) {
            System.err.println("Expected: LeitnerSystem path");
            return;
        }
        final String path = args[0];

        // :NOTE: * Использование конкретных коллекций в определениях
        final ArrayList<Map<String, String>> interpreter = new ArrayList<>(BASKETS);
        for (int i = 0; i < BASKETS; i++) {
            interpreter.add(new HashMap<>());
        }
        readFromFile(path, interpreter);

        double acc = interpreter.get(0).size();
        probabilities = new double[BASKETS];
        // :NOTE: - Вероятности надо было нормелизовать
        probabilities[0] = 1;
        for (int i = 1; i < BASKETS; ++i) {
            probabilities[i] = 1.5 * probabilities[i - 1];
        }


        try (final BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
            String line = "kek";
            do {
                final Map.Entry<String, String> randomWord = genRandomWord(interpreter, acc);
                // :NOTE: # Не реализовано сохранение и загрузка текущего состояния
                //FIXME
                System.out.println(Objects.requireNonNull(randomWord).getKey());
                line = reader.readLine();
                if (line != null) {
                    if ("?".equals(line)) {
                        printBaskets(interpreter);
                    } else if(line.equals(randomWord.getValue())) {
                        if (curNum != BASKETS) {
                            acc = acc - probabilities[curNum] + probabilities[curNum + 1];
                            interpreter.get(curNum).remove(randomWord.getKey());
                            interpreter.get(curNum + 1).put(randomWord.getKey(), randomWord.getValue());
                        }
                        System.out.println("Correct!");
                    } else {
                        System.out.println("Incorrect!");
                        acc = acc - probabilities[curNum] + probabilities[0];
                        interpreter.get(curNum).remove(randomWord.getKey());
                        interpreter.get(0).put(randomWord.getKey(), randomWord.getValue());
                    }
                } else  {
                    break;
                }
            } while (true);
        } catch (final IOException e) {
            e.printStackTrace();
        }
    }
}
