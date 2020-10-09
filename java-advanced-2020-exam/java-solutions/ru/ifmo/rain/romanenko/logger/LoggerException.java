package ru.ifmo.rain.romanenko.logger;

public class LoggerException extends RuntimeException {
    public LoggerException(final String message, final Throwable cause) {
        super(message + ": " + cause.getMessage(), cause);
    }
}