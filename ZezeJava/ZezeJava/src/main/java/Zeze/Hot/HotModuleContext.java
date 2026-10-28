package Zeze.Hot;

import org.jspecify.annotations.Nullable;


/**
 * module 引用其他 module 时，可以保存下来获得服务，
 * 不用每次都去查询。
 */
public class HotModuleContext<T extends HotService> {
	private final String moduleName;
	private volatile @Nullable HotModule module;

	HotModuleContext(HotModule module) {
		this.module = module;
		this.moduleName = module.getName();
	}

	void setModule(@Nullable HotModule module) {
		this.module = module;
	}

	@Nullable HotModule getModule() {
		return module;
	}

	@SuppressWarnings("unchecked")
	public @Nullable T getService() {
		var m = module;
		// module为null：所属模块stop失败被disable（永久）或正处于热更的stopInternal→start窗口。
		// 此时以裸NPE失败没有任何诊断信息（跨模块引用恰恰在模块异常停止时最需要可诊断）。
		if (null == m)
			throw new IllegalStateException("hot module stopped or upgrading: " + moduleName);
		return (T)m.getService();
	}
}
