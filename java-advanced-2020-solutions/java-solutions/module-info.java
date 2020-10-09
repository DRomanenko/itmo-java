module ru.ifmo.rain.romanenko {
    requires java.compiler;
    requires info.kgeorgiy.java.advanced.implementor;
    requires info.kgeorgiy.java.advanced.arrayset;
    requires info.kgeorgiy.java.advanced.walk;
    requires info.kgeorgiy.java.advanced.student;
    requires info.kgeorgiy.java.advanced.concurrent;
    requires info.kgeorgiy.java.advanced.mapper;
    requires info.kgeorgiy.java.advanced.crawler;
    requires info.kgeorgiy.java.advanced.hello;

    opens ru.ifmo.rain.romanenko.implementor;
    exports ru.ifmo.rain.romanenko.implementor;
}