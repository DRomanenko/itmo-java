package ru.ifmo.rain.romanenko.logger;

public class FileLogger extends AbstractLogger{
    FileLogger(final String filename, final String format, final Level limit) {
        super(filename + ".txt", format, limit);
    }
}
