package Zeze.Arch;

@FunctionalInterface
public interface RedirectAllResultHandle {
    void handle(long sessionId, int hash, long returnCode, Zeze.Transaction.Bean result) throws Throwable;
}
