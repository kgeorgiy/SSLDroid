package hu.blint.ssldroid;

public class TunnelConfig {
    public final long id;
    public final String name;
    public final int listenPort;
    public final int targetPort;
    public final String targetHost;
    public final String keyFile;
    public final String keyPass;

    public TunnelConfig(long id, String name, int listenPort, String targetHost, int targetPort, String keyFile, String keyPass) {
        this.id = id;
        this.name = name;
        this.listenPort = listenPort;
        this.targetPort = targetPort;
        this.targetHost = targetHost;
        this.keyFile = keyFile;
        this.keyPass = keyPass;
    }

    @Override
    public String toString() {
        return name + " " + listenPort + ":" + targetHost + ":" + targetPort;
    }
}
