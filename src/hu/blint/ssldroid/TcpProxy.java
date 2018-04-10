package hu.blint.ssldroid;

import java.io.IOException;
import android.util.Log;

/**
 * This is a modified version of the TcpTunnelGui utility borrowed from the
 * xml.apache.org project.
 */
public class TcpProxy {
    String tunnelName;
    int listenPort;
    String tunnelHost;
    int tunnelPort;
    String keyFile, keyPass;
    TcpProxyServerThread server;
    Thread serverThread;

    public TcpProxy(String tunnelName, int listenPort, String targetHost, int targetPort, String keyFile, String keyPass) {
        this.tunnelName = tunnelName;
        this.listenPort = listenPort;
        this.tunnelHost = targetHost;
        this.tunnelPort = targetPort;
        this.keyFile = keyFile;
        this.keyPass = keyPass;
    }

    public void serve() {
        try {
            server = new TcpProxyServerThread(this.tunnelName, this.listenPort, this.tunnelHost,
                                              this.tunnelPort, this.keyFile, this.keyPass);
        } catch (IOException e) {
            Log.d("SSLDroid", "Error setting up listening socket: " + e.toString());
        }
        serverThread = new Thread(server);
    }

    public void stop() {
        if (server != null) {
            try {
                //close the server socket and interrupt the server thread
                serverThread.interrupt();
                server.close();
            } catch (IOException e) {
                Log.d("SSLDroid", "Interrupt failure: " + e.toString());
            }
        }
        Log.d("SSLDroid", "Stopping tunnel "+this.listenPort+":"+this.tunnelHost+":"+this.tunnelPort);
    }
}
