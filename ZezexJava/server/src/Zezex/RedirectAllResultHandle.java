package Zezex;

@FunctionalInterface
public interface RedirectAllResultHandle<R extends Zeze.Transaction.Bean> {
    public void handle(long sessionId, int hash, int returnCode, R result);
}
