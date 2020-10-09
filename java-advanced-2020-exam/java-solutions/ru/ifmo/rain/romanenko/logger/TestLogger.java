package ru.ifmo.rain.romanenko.logger;

public class TestLogger {
    public static void main(final String[] args) {
        /*
         * 0 - уровень
         * 1 - дата
         * 2 - время
         * 3 - сообщение
         * 4 - эксепшн
         */
        final Logger logger = new CompositeLogger(
                new FileLogger("txt_test", "[{0}] {1} {2}: {3} {4}", Logger.Level.INFO),
                new HtmlLogger("html_test1", "<b>{0}</b> {1} {2}: {3} {4}", Logger.Level.ERROR),
                new HtmlLogger("html_test2", "<b>{0}</b> {3} {4}", Logger.Level.DEBUG)
                );

        logger.log(Logger.Level.ERROR, "Hello");

        logger.log(Logger.Level.DEBUG, "Hello2");

        logger.log(Logger.Level.INFO, "Hello3");

        logger.log(Logger.Level.WARNING, "Hello4");
    }
}
