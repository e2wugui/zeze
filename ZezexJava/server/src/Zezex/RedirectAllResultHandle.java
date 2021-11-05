package Zezex;

@FunctionalInterface
public interface RedirectAllResultHandle {
    public void handle(long sessionId, int hash, long returnCode, Zeze.Transaction.Bean result);
}
