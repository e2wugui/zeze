package Zeze.Arch;

@FunctionalInterface
public interface RedirectResultHandle {
    void handle(Zeze.Transaction.Bean result);
}
