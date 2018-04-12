package hu.blint.ssldroid;

import java.io.Closeable;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import javax.net.ssl.SSLSocket;

public class TcpTunnel implements Runnable, Closeable {

    private final TunnelConfig config;

    private int sessionNo = 0;
    private final ActiveTunnel tunnel;
    private final ServerSocket ss;
    private final Thread thread;

    TcpTunnel(TunnelConfig config) throws TunnelException {
        this.config = config;
        tunnel = new ActiveTunnel(config);
        try {
            ss = new ServerSocket(config.listenPort, 50);
        } catch (IOException e) {
            throw new TunnelException("Bind error", e);
        }
        Log.d("Starting tunnel" + ": " + config + " " + ss);
        thread = new Thread(this);
        thread.start();
    }

    private void error(String fullSessionId, String message, Exception e) {
        log(fullSessionId, message + ": " + e.toString());
    }

    @Override
    public void run() {
        Log.d("Listening for connections on " + ss.getLocalSocketAddress() + " ...");
        while (true) {
            String fullSessionId = config.name + "/" + ++sessionNo;
            try {
                if (Thread.interrupted()) {
                    log(fullSessionId, "Interrupted server thread, closing sockets...");
                    return;
                }
                // accept the connection from my client
                Socket client = ss.accept();
                try {
                    run(fullSessionId, client);
                } catch (IOException e) {
                    error(fullSessionId, "Error accepting client", e);
                }
            } catch (IOException ee) {
                error(fullSessionId, "Ouch", ee);
            }
        }
    }

    private void run(String fullSessionId, Socket client) throws IOException {
        SSLSocket server = tunnel.connect(5000);

        log(fullSessionId, "-------------------------------- Tunnelling port "
                + ss.getLocalSocketAddress() + " to port "
                + server.getRemoteSocketAddress() + " ...");

        // relay the stuff through
        Relay fromBrowserToServer = new Relay(
                fullSessionId + "/client", client.getInputStream(), server.getOutputStream());
        Relay fromServerToBrowser = new Relay(
                fullSessionId + "/server", server.getInputStream(), client.getOutputStream());
        new Thread(fromBrowserToServer).start();
        new Thread(fromServerToBrowser).start();
    }

    private void log(String fullSessionId, String message) {
        Log.d(fullSessionId + "" + ": " + message);
    }

    @Override
    public void close() {
        try {
            //close the server socket and interrupt the server thread
            thread.interrupt();
            ss.close();
        } catch (IOException e) {
            Log.d("Interrupt failure" + ": " + e.toString());
        }
        Log.d("Stopping tunnel " + config);
    }
}

