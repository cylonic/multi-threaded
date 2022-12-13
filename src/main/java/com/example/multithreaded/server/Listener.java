package com.example.multithreaded.server;

import com.example.multithreaded.io.SocketReader;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Listens on the specified port at localhost spawning threads up to maxConnections number of times
 */
public class Listener implements Runnable {

    private final ExecutorService executorService;
    private final int maxConnections;
    private final int port;
    private final BlockingQueue<String> queue;
    private ServerSocket serverSocket = null;
    private final AtomicBoolean signalTerminate;

    public Listener(int port, int maxConnections, BlockingQueue<String> queue, AtomicBoolean signalTerminate) {
        this.signalTerminate = signalTerminate;
        this.maxConnections = maxConnections;
        this.port = port;
        this.queue = queue;
        this.executorService = Executors.newFixedThreadPool(maxConnections + 1);
    }

    @Override
    public void run() {
        if (serverSocket == null) {
            try {
                serverSocket = new ServerSocket(port, maxConnections);
            } catch (IOException e) {
                System.err.println("Failed to create server on port: " + port);
                throw new RuntimeException(e);
            }
        }


        while (!serverSocket.isClosed()) {
            try {
                executorService.submit(new SocketReader(serverSocket.accept(), queue, signalTerminate));
            } catch (IOException e) {
                // We expect an error when the server socket is closed
                if (!e.getMessage().endsWith("accept failed")) {
                    System.err.println("Failed upon error in " + this.getClass().getSimpleName() + e.getMessage());
                    close();
                    throw new RuntimeException(e);
                }
            }
        }
    }

    /**
     * Closes socket and the thread pool
     * <p>
     * Since server socket accept method is blocking, we must close it from external thread
     */
    public synchronized void close() {
        try {
            if (serverSocket != null) {
                serverSocket.close();
                executorService.shutdown();
            }
        } catch (IOException e) {
            // quiet
        }
    }
}
