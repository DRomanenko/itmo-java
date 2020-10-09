package ru.ifmo.rain.romanenko.logger;

import java.util.Arrays;
import java.util.List;

public class CompositeLogger implements Logger {
    private final List<Logger> loggers;

    public CompositeLogger(final Logger... loggers) {
        this.loggers = Arrays.asList(loggers);
    }

    @Override
    public void log(final Level importance, final String message) {
        for (final Logger l : loggers) {
            l.log(importance, message);
        }
    }

    @Override
    public void log(final Level importance, final String message, final Throwable cause) {
        for (final Logger l : loggers) {
            l.log(importance, message, cause);
        }
    }
}
