package com.example.multithreaded.io;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Reads the shared queue and writes to log file. Filters duplicates, keeps track of counts.
 */
public class LogWriter implements Runnable {

    private long count = 0;
    private long dupeCount = 0;
    private final String NAME = "numbers.log";

    private final BlockingQueue<String> queue;
    private final AtomicBoolean signalTerminate;

    private BufferedWriter writer;

    private final Set<String> seen = new HashSet<>();

    public LogWriter(BlockingQueue<String> queue, AtomicBoolean signalTerminate) {
        this.queue = queue;
        this.signalTerminate = signalTerminate;
    }

    @Override
    public void run() {
        try {
            writer = Files.newBufferedWriter(Path.of(NAME));

            while (!(signalTerminate.get() && queue.isEmpty())) {
                String line = queue.poll();
                if (line == null) {
                    continue;
                }
                if (seen.add(line)) {
                    writer.write(line);
                    writer.newLine();
                    count++;
                } else {
                    dupeCount++;
                }
            }
        } catch (IOException e) {
            System.err.println("Failed upon error in " + this.getClass().getSimpleName() + e.getMessage());
            close();
            throw new RuntimeException(e);
        }
        close();
    }

    private void close() {
        try {
            if (writer != null) {
                writer.flush();
                writer.close();
            }
        } catch (IOException e) {
            // quiet
        }
    }

    public long getCount() {
        return count;
    }

    public long getDupeCount() {
        return dupeCount;
    }
}
