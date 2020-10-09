package ru.ifmo.rain.romanenko.logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.text.MessageFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public class AbstractLogger implements Logger {
    private final Path file;
    private final MessageFormat formatter;
    private final Level limit;

    public AbstractLogger(final String filename, final String format, final Level limit) {
        file = checkPath(filename, "ERROR: Incorrect path");
        formatter = new MessageFormat(format);
        this.limit = limit;
    }

    private Path checkPath(final String file, final String message) throws LoggerException {
        try {
            return Paths.get(file);
        } catch (final InvalidPathException e) {
            throw new LoggerException(message, e);
        }
    }

    private void printLog(final Level importance, final String message, final Throwable cause) {
        if (importance.getPriority() >= limit.getPriority()) {
            final LocalDateTime now = LocalDateTime.now();
            final LocalDate strDate = now.toLocalDate();
            final LocalTime strTime = now.toLocalTime();
            final String strCause = cause == null ? "" : cause.getLocalizedMessage();
            final Object[] args = {importance.toString(), strDate, strTime, message, strCause};
            try {
                synchronized (this) {
                    // TODO add BlockingQueue, otherwise the time order is not saved
                    new File(String.valueOf(file)).createNewFile(); // ignored if already exist
                    Files.writeString(file,
                            formatter.format(args) + System.lineSeparator(),
                            StandardOpenOption.APPEND);
                }
            } catch (final IOException e) {
                throw new LoggerException("ERROR: bad file", e);
            }
        }
    }

    @Override
    public void log(final Level importance, final String message) {
        printLog(importance, message, null);
    }

    @Override
    public void log(final Level importance, final String message, final Throwable cause) {
        printLog(importance, message, cause);
    }
}
