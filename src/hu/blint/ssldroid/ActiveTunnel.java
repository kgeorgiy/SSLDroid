package hu.blint.ssldroid;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

public class ActiveTunnel {
    private final TunnelConfig config;
    private final SSLSocketFactory socketFactory;
    private final int connectionTimeout;

    public ActiveTunnel(TunnelConfig config, int connectionTimeout) throws TunnelException {
        this.config = config;
        this.connectionTimeout = connectionTimeout;

        socketFactory = createSocketFactory();
    }

    public static KeyStore createKeyStore(String keyFile, String keyPass) throws TunnelException {
        try {
            FileInputStream stream = new FileInputStream(keyFile);
            try {
                KeyStore keyStore = KeyStore.getInstance("PKCS12");
                keyStore.load(stream, keyPass.toCharArray());
                return keyStore;
            } finally {
                stream.close();
            }
        } catch (CertificateException e) {
            throw new TunnelException("Error loading the client certificate file", e);
        } catch (NoSuchAlgorithmException e) {
            throw new TunnelException("PKCS12 not supported on this system", e);
        } catch (KeyStoreException e) {
            throw new TunnelException("Error setting up keystore", e);
        } catch (IOException e) {
            throw new TunnelException("Error loading the client certificate file", e);
        }
    }

    private KeyManager[] createKeyManagers() throws TunnelException {
        if (config.keyFile == null || config.keyFile.isEmpty()) {
            return null;
        }
        try {
            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance("X509");
            keyManagerFactory.init(createKeyStore(config.keyFile, config.keyPass), config.keyPass.toCharArray());
            return keyManagerFactory.getKeyManagers();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            throw new TunnelException("X509 not supported on this system: " + e, e);
        } catch (KeyStoreException e) {
            throw new TunnelException("Error setting up keystore", e);
        } catch (UnrecoverableKeyException e) {
            throw new TunnelException("Invalid certificate password", e);
        }
    }

    private SSLSocketFactory createSocketFactory() throws TunnelException {
        try {
            SSLContext context = SSLContext.getInstance("TLS");
            context.init(createKeyManagers(), config.trustType.getTrustManagers(config.trustFile, config.trustPass), null);
            return context.getSocketFactory();
        } catch (NoSuchAlgorithmException e) {
            throw new TunnelException("TLS not supported on this system", e);
        } catch (KeyManagementException e) {
            throw new TunnelException("Error initializing TLS context", e);
        }
    }

    public SSLSocket connect() throws IOException {
        SSLSocket socket = (SSLSocket) socketFactory.createSocket();
        socket.connect(new InetSocketAddress(config.targetHost, config.targetPort), connectionTimeout);
        setSNIHost(socketFactory, socket, config.targetHost);
        socket.startHandshake();
        return socket;
    }

    private void setSNIHost(final SSLSocketFactory factory, final SSLSocket socket, final String hostname) {
        if (factory instanceof android.net.SSLCertificateSocketFactory) {
            ((android.net.SSLCertificateSocketFactory) factory).setHostname(socket, hostname);
        } else {
            try {
                socket.getClass().getMethod("setHostname", String.class).invoke(socket, hostname);
            } catch (Throwable e) {
                // ignore any error, we just can't set the hostname...
            }
        }
    }
}
