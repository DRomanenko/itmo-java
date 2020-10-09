package ru.ifmo.rain.romanenko.logger;

public class HtmlLogger extends AbstractLogger{
    HtmlLogger(final String filename, final String format, final Level limit) {
        super(filename + ".html", format, limit);
    }
}
