package ru.ifmo.rain.romanenko.walk;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.*;

public class RecursiveWalk {
    private final Path inputPath;
    private final Path outputPath;

    RecursiveWalk(final String inputFile, final String outputFile) throws WalkerException {
        inputPath = checkPath(inputFile, "Incorrect path to input file");
        outputPath = checkPath(outputFile, "Incorrect path to output file");

        final Path parent = outputPath.getParent();
        if (parent != null && Files.notExists(parent)) {
            try {
                Files.createDirectories(parent);
            } catch (final IOException e) {
                throw new WalkerException("ERROR: Can't create directory for output file: " + e.getMessage());
            }
        }
    }

    private Path checkPath(final String file, final String message) throws WalkerException {
        try {
            return Paths.get(file);
        } catch (final InvalidPathException e) {
            throw new WalkerException(message + ": " + e.getMessage());
        }
    }

    private void walk() throws WalkerException {
        try (final BufferedReader reader = Files.newBufferedReader(inputPath)) {
            try (final BufferedWriter writer = Files.newBufferedWriter(outputPath)) {
                final FileVisitor visitor = new FileVisitor(writer);
                try {
                    String path;
                    while ((path = reader.readLine()) != null) {
                        try {
                            try {
                                Files.walkFileTree(Paths.get(path), visitor);
                            } catch (final InvalidPathException e) {
                                visitor.write(0, path);
                            }
                        } catch (final IOException e) {
                            throw new WalkerException("ERROR: Failed while writing output file: " + e.getMessage());
                        }
                    }
                } catch (final IOException e) {
                    throw new WalkerException("ERROR: Failed while reading input file: " + e.getMessage());
                }
            } catch (final IOException e) {
                throw new WalkerException("ERROR: Failed to writing output file: " + e.getMessage());
            }
        } catch (final IOException e) {
            throw new WalkerException("ERROR: Failed to reading input file: " + e.getMessage());
        }
    }

    public static void main(final String[] args) {
        try {
            if (args == null || args.length != 2 || args[0] == null || args[1] == null) {
                throw new WalkerException("ERROR: Expected arguments: INPUT_FILE OUTPUT_FILE");
            }
            new RecursiveWalk(args[0], args[1]).walk();
        } catch (final WalkerException e) {
            System.err.println(e.getMessage());
        }
    }
}
