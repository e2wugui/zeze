package Zeze.Arch;

@FunctionalInterface
public interface RedirectAllDoneHandle<T extends RedirectResult> {
	void handle(ModuleRedirectAllContext<T> ctx) throws Throwable;
}
