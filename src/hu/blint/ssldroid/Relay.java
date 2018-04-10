package hu.blint.ssldroid;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.SocketException;

import android.util.Log;

public class Relay extends Thread {
    /**
     * 
     */
    private InputStream in;
    private OutputStream out;
    private String side;
    private int fullSessionId;
    private final static int BUFSIZ = 4096;
    private byte buf[] = new byte[BUFSIZ];

    public Relay(InputStream in, OutputStream out, String side, int fullSessionId) {
        this.in = in;
        this.out = out;
        this.side = side;
        this.fullSessionId = fullSessionId;
    }

    public void run() {
        int n = 0;

        try {
            while ((n = in.read(buf)) > 0) {
                if (Thread.interrupted()) {
                    // We've been interrupted: no more relaying
                    log("Interrupted " + side + " thread");
                    try {
                        in.close();
                        out.close();
                    } catch (IOException e) {
                        log(e.toString());
                    }
                    return;
                }
                out.write(buf, 0, n);
                out.flush();

                for (int i = 0; i < n; i++) {
                    if (buf[i] == 7)
                        buf[i] = '#';
                }
            }
        } catch (SocketException e) {
            log(e.toString());
        } catch (IOException e) {
            log(e.toString());
        } finally {
            try {
                in.close();
                out.close();
            } catch (IOException e) {
                log(e.toString());
            }
        }
        log("Quitting " + side + "-side stream proxy...");
    }

    private void log(String message) {
        Log.d("SSLDroid", fullSessionId + ": " + message);
    }
}
