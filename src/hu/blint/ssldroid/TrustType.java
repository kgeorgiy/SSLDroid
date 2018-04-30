package hu.blint.ssldroid;

import android.annotation.SuppressLint;

import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;

import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

public enum TrustType {
    ALL(R.string.tunnel_trustType_all) {
        @Override
        public TrustManager[] getTrustManagers(String keyFile, String keyPassword) {
            return TRUST_ALL;
        }
    },
    VALID(R.string.tunnel_trustType_valid) {
        @Override
        public TrustManager[] getTrustManagers(String keyFile, String keyPassword) {
            return null;
        }
    },
    CUSTOM(R.string.tunnel_trustType_custom) {
        @Override
        public TrustManager[] getTrustManagers(String keyFile, String keyPassword) throws TunnelException {
            try {
                final TrustManagerFactory factory = TrustManagerFactory.getInstance("X509");
                factory.init(ActiveTunnel.createKeyStore(keyFile, keyPassword));
                return factory.getTrustManagers();
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
                throw new TunnelException("X509 algorithm is not supported" , e);
            } catch (KeyStoreException e) {
                throw new TunnelException("Invalid key store", e);
            }
        }
    };

    @SuppressLint("TrustAllX509TrustManager")
    private static final TrustManager[] TRUST_ALL = new TrustManager[]{
        new X509TrustManager() {
            public X509Certificate[] getAcceptedIssuers() {
                return null;
            }

            public void checkClientTrusted(X509Certificate[] certs, String authType) {
            }

            public void checkServerTrusted(X509Certificate[] certs, String authType) {
            }
        }
    };

    private final int id;

    TrustType(int id) {
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public abstract TrustManager[] getTrustManagers(final String keyFile, final String keyPassword) throws TunnelException;
}
