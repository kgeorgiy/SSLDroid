package hu.blint.ssldroid;

import java.io.IOException;
import android.util.Log;

/**
 * This is a modified version of the TcpTunnelGui utility borrowed from the
 * xml.apache.org project.
 */
public class TcpProxy {
    private final TunnelConfig config;
    private final TcpProxyServerThread server;
    private final Thread serverThread;

    public TcpProxy(TunnelConfig config) throws IOException {
        this.config = config;
        server = new TcpProxyServerThread(config);
        serverThread = new Thread(server);
        Log.d("SSLDroid", "Starting tunnel: " + config);
    }

    public void stop() {
        try {
            //close the server socket and interrupt the server thread
            serverThread.interrupt();
            server.close();
        } catch (IOException e) {
            Log.d("SSLDroid", "Interrupt failure: " + e.toString());
        }
        Log.d("SSLDroid", "Stopping tunnel " + config);
    }
}
