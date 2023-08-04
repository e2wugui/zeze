package Zeze.Hot;

import org.jetbrains.annotations.NotNull;

/**
 * module 引用其他 module 时，可以保存下来获得服务，
 * 不用每次都去查询。
 */
public class HotModuleContext<T extends HotService> {
	private volatile HotModule module;

	HotModuleContext(@NotNull HotModule module) {
		this.module = module;
	}

	void setModule(@NotNull HotModule module) {
		System.out.println("setModule " + this.module.getClass().getClassLoader() + "->" + module.getClass().getClassLoader());
		this.module = module;
	}

	@SuppressWarnings("unchecked")
	public T getService() {
		return (T)module.getService();
	}
}
