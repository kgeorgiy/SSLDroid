package hu.blint.ssldroid;

public class TunnelConfig {
    public final long id;
    public final String name;
    public final int listenPort;
    public final int targetPort;
    public final String targetHost;
    public final String keyFile;
    public final String keyPass;
    public final TrustType trustType;
    public final String trustFile;
    public final String trustPass;

    public TunnelConfig(long id, String name, int listenPort, String targetHost, int targetPort, String keyFile, String keyPass, final TrustType trustType, String trustFile, String trustPass) {
        this.id = id;
        this.name = name;
        this.listenPort = listenPort;
        this.targetPort = targetPort;
        this.targetHost = targetHost;
        this.keyFile = keyFile;
        this.keyPass = keyPass;
        this.trustType = trustType;
        this.trustFile = trustFile;
        this.trustPass = trustPass;
    }

    @Override
    public String toString() {
        return name + " " + listenPort + ":" + targetHost + ":" + targetPort;
    }
}
