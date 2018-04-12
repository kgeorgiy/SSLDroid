package hu.blint.ssldroid;

public class TunnelException extends Exception {
    public TunnelException(String message, Throwable throwable) {
        super(message + ": " + throwable.getLocalizedMessage(), throwable);
    }
}
