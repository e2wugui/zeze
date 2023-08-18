package Zeze;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import Zeze.Arch.Gen.GenModule;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class AppBase {
	public abstract Application getZeze();

	public @Nullable IModule[] createRedirectModules(Class<?> @NotNull [] moduleClasses) {
		return GenModule.instance.createRedirectModules(this, moduleClasses);
	}

	// 历史上是 public 的。
	// 先改成 protected 看看。
	protected final ConcurrentHashMap<String, Zeze.IModule> modules = new ConcurrentHashMap<>();

	public ConcurrentMap<String, IModule> getModules() {
		return modules;
	}

	public void addModule(IModule module) {
		modules.put(module.getName(), module);
	}

	public void removeModule(IModule module) {
		modules.remove(module.getName());
	}

	public void removeModule(String moduleName) {
		modules.remove(moduleName);
	}
}
