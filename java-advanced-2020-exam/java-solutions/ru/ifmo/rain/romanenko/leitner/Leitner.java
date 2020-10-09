package ru.ifmo.rain.romanenko.leitner;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.Collator;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

public class Leitner {
    static Map<String, String> readData(final String source, final Locale inputLocale, final Locale outputLocale) throws IOException {
        final Map<String, String> res = new TreeMap<>(Collator.getInstance(inputLocale));
        final var outputComparator = Collator.getInstance(outputLocale);
        for (final var line : Files.readAllLines(Paths.get(source))) {
            final var parts = line.split("=");
            if (parts.length != 2) throw new IOException("Wrong format at line \"" + line + "\"");
            final String in = parts[0];
            final String out = parts[1];
            final var prev = res.putIfAbsent(in, out);
            if (prev != null && !outputComparator.equals(prev, out)) {
                throw new IOException("Different values \"" + prev + "\", \"" + out + "\" for the same key \"" + in + "\" found.");
            }
        }
        return res;
    }

}
