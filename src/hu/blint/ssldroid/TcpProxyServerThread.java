package hu.blint.ssldroid;

import android.util.Log;

import java.io.Closeable;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.UnrecoverableKeyException;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

public class TcpProxyServerThread implements Runnable, Closeable {

    private final TunnelConfig config;

    private int sessionNo = 0;
    private SSLSocketFactory sslSocketFactory;
    private final ServerSocket ss;

    TcpProxyServerThread(TunnelConfig config) throws IOException {
        this.config = config;
        ss = new ServerSocket(config.listenPort, 50);
    }

    // Create a trust manager that does not validate certificate chains
    // TODO: handle this somehow properly (popup if cert is untrusted?)
    // TODO: cacert + crl should be configurable
    private TrustManager[] trustAllCerts = new TrustManager[] {
    new X509TrustManager() {
        public java.security.cert.X509Certificate[] getAcceptedIssuers() {
            return null;
        }
        public void checkClientTrusted(
        java.security.cert.X509Certificate[] certs, String authType) {
        }
        public void checkServerTrusted(
        java.security.cert.X509Certificate[] certs, String authType) {
        }
    }
    };

    private final SSLSocketFactory getSocketFactory(String pkcsFile,
                                                    String pwd, String fullSessionId) {
        if (sslSocketFactory == null) {
            try {
                KeyManagerFactory keyManagerFactory;
                if (pkcsFile != null && !pkcsFile.isEmpty()) {
                    keyManagerFactory = KeyManagerFactory.getInstance("X509");
                    KeyStore keyStore = KeyStore.getInstance("PKCS12");
                    keyStore.load(new FileInputStream(pkcsFile), pwd.toCharArray());
                    keyManagerFactory.init(keyStore, pwd.toCharArray());
                } else {
                    keyManagerFactory = null;
                }
                SSLContext context = SSLContext.getInstance("TLS");
                context.init(keyManagerFactory == null ? null : keyManagerFactory.getKeyManagers(), trustAllCerts,
                             new SecureRandom());
                sslSocketFactory = context.getSocketFactory();
            } catch (FileNotFoundException e) {
                log(fullSessionId, "Error loading the client certificate file:"
                        + e.toString());
            } catch (KeyManagementException e) {
                log(fullSessionId, "No SSL algorithm support: " + e.toString());
            } catch (NoSuchAlgorithmException e) {
                log(fullSessionId, "No common SSL algorithm found: " + e.toString());
            } catch (KeyStoreException e) {
                log(fullSessionId, "Error setting up keystore:" + e.toString());
            } catch (java.security.cert.CertificateException e) {
                log(fullSessionId, "Error loading the client certificate:" + e.toString());
            } catch (IOException e) {
                log(fullSessionId, "Error loading the client certificate file:"
                        + e.toString());
            } catch (UnrecoverableKeyException e) {
                String message = "Error loading the client certificate:" + e.toString();
                log(fullSessionId, message);
            }
        }
        return sslSocketFactory;
    }

    @Override
    public void run() {
        Log.d("SSLDroid", "Listening for connections on " + ss.getLocalSocketAddress() + " ...");
        while (true) {
            String fullSessionId = getTunnelName() + "/" + ++sessionNo;
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
                    log(fullSessionId, "Error accepting client: " + e.toString());
                }
            } catch (IOException ee) {
                log(fullSessionId, "Ouch: " + ee.toString());
            }
        }
    }

    private void run(String fullSessionId, Socket client) throws IOException {
        SSLSocketFactory sf = getSocketFactory(this.getKeyFile(), this.getKeyPass(), fullSessionId);
        SSLSocket server = (SSLSocket) sf.createSocket(this.getTunnelHost(), this.getTunnelPort());
        setSNIHost(sf, server, this.getTunnelHost());
        server.startHandshake();

        log(fullSessionId, "Tunnelling port "
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
        Log.d("SSLDroid", fullSessionId + ": " + message);
    }

    private void setSNIHost(final SSLSocketFactory factory, final SSLSocket socket, final String hostname) {
        if (factory instanceof android.net.SSLCertificateSocketFactory && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR1) {
            ((android.net.SSLCertificateSocketFactory)factory).setHostname(socket, hostname);
        } else {
            try {
                socket.getClass().getMethod("setHostname", String.class).invoke(socket, hostname);
            } catch (Throwable e) {
                // ignore any error, we just can't set the hostname...
            }
        }
    }

    @Override
    public void close() throws IOException {
        ss.close();
    }

    private String getTunnelName() {
        return config.name;
    }

    private String getTunnelHost() {
        return config.targetHost;
    }

    private int getTunnelPort() {
        return config.targetPort;
    }

    private String getKeyFile() {
        return config.keyFile;
    }

    private String getKeyPass() {
        return config.keyPass;
    }
}

