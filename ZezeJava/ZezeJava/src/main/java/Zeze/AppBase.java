package Zeze;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantLock;
import Zeze.Arch.Gen.GenModule;
import Zeze.Netty.HttpServer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class AppBase extends ReentrantLock {
	public abstract Application getZeze();

	public @NotNull IModule @Nullable [] createRedirectModules(@NotNull Class<?> @NotNull [] moduleClasses) {
		var modules = GenModule.instance.createRedirectModules(this, moduleClasses);
		if (GenModule.instance.genFileSrcRoot != null) {
			System.out.println("---------------");
			System.out.println("New Source File Has Generate. Re-Compile Need.");
			System.exit(0);
		}
		return modules;
	}

	// 历史上是 public 的。
	// 先改成 protected 看看。
	protected final ConcurrentHashMap<String, Zeze.IModule> modules = new ConcurrentHashMap<>();

	public @NotNull ConcurrentMap<String, IModule> getModules() {
		return modules;
	}

	public void addModule(@NotNull IModule module) {
		modules.put(module.getName(), module);
	}

	public void removeModule(@NotNull IModule module) {
		modules.remove(module.getName());
	}

	public void removeModule(@NotNull String moduleName) {
		modules.remove(moduleName);
	}

	public void createZeze(@Nullable Config config) throws Exception {
		throw new UnsupportedOperationException();
	}

	public void createService() throws Exception {
		throw new UnsupportedOperationException();
	}

	public void createModules() throws Exception {
		throw new UnsupportedOperationException();
	}

	/**
	 * 新增的接口。为了兼容，这里不抛出异常。
	 */
	public void startLastModules() throws Exception {
	}

	public @Nullable HttpServer getHttpServer() {
		return null;
	}
}
