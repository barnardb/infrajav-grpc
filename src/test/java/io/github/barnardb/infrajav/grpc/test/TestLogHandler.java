package io.github.barnardb.infrajav.grpc.test;

import io.github.barnardb.infrajav.grpc.ActiveNameResolver;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static java.util.Arrays.asList;

public class TestLogHandler extends Handler implements Closeable {

    public final List<LogRecord> records = Collections.synchronizedList(new ArrayList<>());
    private final List<Logger> loggers;

    public TestLogHandler(Logger... loggers) {
        this.loggers = asList(loggers);
        this.loggers.forEach(l -> l.addHandler(this));
    }

    public static TestLogHandler forClass(Class<ActiveNameResolver> clazz) {
        Logger logger = Logger.getLogger(clazz.getName());
        logger.setLevel(Level.ALL);
        return new TestLogHandler(logger);
    }

    @Override
    public void publish(LogRecord record) {
        records.add(record);
    }

    @Override
    public void flush() {
        // nothing to do
    }

    @Override
    public void close() throws SecurityException {
        loggers.forEach(l -> l.removeHandler(this));
    }
}
