package Zezex;

@FunctionalInterface
public interface RedirectResultHandle<R extends Zeze.Transaction.Bean> {
    public void handle(R result);
}
