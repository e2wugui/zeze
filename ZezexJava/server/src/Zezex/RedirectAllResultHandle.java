package Zezex;

@FunctionalInterface
public interface RedirectAllResultHandle {
    public void handle(long sessionId, int hash, int returnCode, Zeze.Transaction.Bean result);
}
