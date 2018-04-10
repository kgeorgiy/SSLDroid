package hu.blint.ssldroid;

import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class Relay implements Runnable {
    private final static int BUFFER_SIZE = 4096;

    private String id;
    private InputStream in;
    private OutputStream out;

    public Relay(String id, InputStream in, OutputStream out) {
        this.id = id;
        this.in = in;
        this.out = out;
    }

    @Override
    public void run() {
        try {
            try {
                try {
                    int n;
                    byte buf[] = new byte[BUFFER_SIZE];
                    while ((n = in.read(buf)) > 0) {
                        if (Thread.interrupted()) {
                            // We've been interrupted: no more relaying
                            log("Thread interrupted");
                            return;
                        }
                        out.write(buf, 0, n);
                        out.flush();
                    }
                    log("Quitting");
                } finally {
                    log("Closing streams");
                    in.close();
                }
            } finally {
                out.close();
            }
        } catch (IOException e) {
            log(e.toString());
        }
    }

    private void log(String message) {
        Log.d("SSLDroid", id + ": " + message);
    }
}
