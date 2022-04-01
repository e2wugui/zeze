package Zeze.Arch;

@FunctionalInterface
public interface RedirectAllDoneHandle {
    void handle(ModuleRedirectAllContext ctx) throws Throwable;
}
