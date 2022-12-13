package com.example.multithreaded.io;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

/**
 * The worker responsible to read from a socket
 * <p>
 * Puts valid 9 length numeric values on shared queue, discards else
 * <p>
 * If "terminate" is detected from a socket, shutdown app
 */
public class SocketReader implements Runnable {

    public static final String TERMINATE = "terminate";
    private final Pattern pattern = Pattern.compile("\\d{9}");
    private final Socket socket;
    private final BlockingQueue<String> queue;
    private final AtomicBoolean signalTerminate;
    private BufferedReader br;

    int count = 0;


    public SocketReader(Socket socket, BlockingQueue<String> queue, AtomicBoolean signalTerminate) {
        this.socket = socket;
        this.queue = queue;
        this.signalTerminate = signalTerminate;
    }

    @Override
    public void run() {
        try {
            br = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            while (!signalTerminate.get()) {
                String line;
                if ((line = br.readLine()) != null) {
                    if (pattern.matcher(line).matches()) {
                        queue.put(line);
                        count++;
                    } else if (TERMINATE.equals(line)) {
                        signalTerminate.set(true);
                    } else {
                        close();
                    }
                }
            }
        } catch (IOException | InterruptedException e) {
            // will close socket if bad data passes through, dont want to log this
            if (!e.getMessage().endsWith("Stream closed")) {
                System.err.println("Failed upon error in " + this.getClass().getSimpleName() + e.getMessage());
                close();
                throw new RuntimeException(e);
            }
        }
        close();
    }

    private void close() {
        try {
            socket.close();
            br.close();
        } catch (IOException e) {
            // quiet
        }
    }
}
