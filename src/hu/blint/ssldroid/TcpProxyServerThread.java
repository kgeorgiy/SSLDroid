package hu.blint.ssldroid;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
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

import android.util.Log;

public class TcpProxyServerThread extends Thread {

    String tunnelName;
    int listenPort;
    String tunnelHost;
    int tunnelPort;
    String keyFile, keyPass;
    Relay inRelay, outRelay;
    ServerSocket ss = null;
    int sessionid = 0;
    private SSLSocketFactory sslSocketFactory;

    public TcpProxyServerThread(String tunnelName, int listenPort, String tunnelHost, int tunnelPort, String keyFile, String keyPass) {
        this.tunnelName = tunnelName;
        this.listenPort = listenPort;
        this.tunnelHost = tunnelHost;
        this.tunnelPort = tunnelPort;
        this.keyFile = keyFile;
        this.keyPass = keyPass;
    }

    // Create a trust manager that does not validate certificate chains
    // TODO: handle this somehow properly (popup if cert is untrusted?)
    // TODO: cacert + crl should be configurable
    TrustManager[] trustAllCerts = new TrustManager[] {
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

    public final SSLSocketFactory getSocketFactory(String pkcsFile,
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

    public void run() {
        try {
            ss = new ServerSocket(listenPort, 50, InetAddress.getLocalHost());
            Log.d("SSLDroid", "Listening for connections on "+InetAddress.getLocalHost().getHostAddress()+":"+
                  + this.listenPort + " ...");
        } catch (Exception e) {
            Log.d("SSLDroid", "Error setting up listening socket: " + e.toString());
            return;
        }
        while (true) {
            String fullSessionId = tunnelName + "/" + sessionid;
            sessionid++;
            try {
                Thread fromBrowserToServer = null;
                Thread fromServerToBrowser = null;

                if (isInterrupted()) {
                    log(fullSessionId, "Interrupted server thread, closing sockets...");
                    ss.close();
                    return;
                }
                // accept the connection from my client
                Socket sc = null;
                try {
                    sc = ss.accept();
                } catch (SocketException e) {
                    log(fullSessionId, "Accept failure: " + e.toString());
                }

                Socket st = null;
                try {
                    final SSLSocketFactory sf = getSocketFactory(this.keyFile, this.keyPass, fullSessionId);
                    st = (SSLSocket) sf.createSocket(this.tunnelHost, this.tunnelPort);
                    setSNIHost(sf, (SSLSocket) st, this.tunnelHost);
                    ((SSLSocket) st).startHandshake();
                } catch (IOException e) {
                    log(fullSessionId, "SSL failure: " + e.toString());
                    return;
                }
                catch (Exception e) {
                    log(fullSessionId, "SSL failure: " + e.toString());
                    if (sc != null)
                      {
                        sc.close();
                      }
                    return;
                }

                if (sc == null || st == null) {
                    log(fullSessionId, "Trying socket operation on a null socket, returning");
                    return;
                }
                log(fullSessionId, "Tunnelling port "
                        + listenPort + " to port "
                        + tunnelPort + " on host "
                        + tunnelHost + " ...");

                // relay the stuff through
                fromBrowserToServer = new Relay(
                        sc.getInputStream(), st.getOutputStream(), "client", sessionid);
                fromServerToBrowser = new Relay(
                        st.getInputStream(), sc.getOutputStream(), "server", sessionid);

                fromBrowserToServer.start();
                fromServerToBrowser.start();

            } catch (IOException ee) {
                log(fullSessionId, "Ouch: " + ee.toString());
            }
        }
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
};

