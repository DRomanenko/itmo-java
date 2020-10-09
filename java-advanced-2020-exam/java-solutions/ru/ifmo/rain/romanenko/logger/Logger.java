package ru.ifmo.rain.romanenko.logger;

public interface Logger {
    enum Level {
        DEBUG(1), INFO(2), WARNING(3), ERROR(4);
        private final int priority;

        Level(final int level) {
            priority = level;
        }

        public int getPriority() {
            return priority;
        }
    }

    void log(Level importance, String message);

    void log(Level importance, String message, Throwable cause);
}
