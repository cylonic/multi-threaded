package com.example.multithreaded;


import com.example.multithreaded.io.LogWriter;
import com.example.multithreaded.server.Listener;
import com.example.multithreaded.stats.StatsManager;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Runs the socket listener and log writer.
 * <p>
 * Spawns 1 thread to listen on the specified port on localhost,
 * this thread will spawn up to maxConnection number of threads
 * to listen to individual socket connections
 * <p>
 * Spawns 1 thread to write to log file and filters duplicates
 * <p>
 * Utilizes a shared thread to pass data
 */
public class Main {
    private final AtomicBoolean signalTerminate = new AtomicBoolean(false);
    private final ExecutorService executorService;
    private final int maxConnections;
    private final int port;

    public Main(int port, int maxConnections) {
        this.port = port;
        this.maxConnections = maxConnections;
        this.executorService = Executors.newFixedThreadPool(2);
    }

    public static void main(String[] args) {
        System.out.println("Starting up server ....");
        final int port = 4000;
        final int maxConnections = 5;
        Main driver = new Main(port, maxConnections);
        driver.run();
    }

    public void run() {
        // the shared queue
        final BlockingQueue<String> queue = new LinkedBlockingQueue<>();

        // writer thread
        final LogWriter logWriter = new LogWriter(queue, signalTerminate);
        executorService.submit(logWriter);

        // listener thread
        final Listener listener = new Listener(4000, 5, queue, signalTerminate);
        executorService.submit(listener);

        // liveness probe
        final ScheduledExecutorService liveness = Executors.newScheduledThreadPool(1);
        final StatsManager stats = new StatsManager(logWriter);
        liveness.scheduleAtFixedRate(() -> System.out.println(stats.reportStats()), 9, 10, TimeUnit.SECONDS);

        // run until we see a call to terminate then shut it all down
        while (true) {
            if (signalTerminate.get()) {
                listener.close();
                executorService.shutdown();
                liveness.shutdownNow();
                System.out.println("Total uniques: " + logWriter.getCount() + "\n" + "Total dupes: " + logWriter.getDupeCount());
                break;
            }
        }
    }
}