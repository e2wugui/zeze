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

	/** 运行时装载/生成redirect子类并实例化；构建期源码生成走GenModule.generateRedirectSources。 */
	public @NotNull IModule @NotNull [] createRedirectModules(@NotNull Class<?> @NotNull [] moduleClasses) {
		return GenModule.instance.createRedirectModules(this, moduleClasses);
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

	@SuppressWarnings("RedundantThrows")
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
