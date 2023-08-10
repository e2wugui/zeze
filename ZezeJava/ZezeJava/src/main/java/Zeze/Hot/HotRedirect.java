package Zeze.Hot;

import Zeze.Arch.Gen.GenModule;

public class HotRedirect extends ClassLoader {
	private final HotManager manager;

	public HotRedirect(HotManager manager) {
		super(manager);
		this.manager = manager;
	}

	@Override
	public Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
		// 1. Redirect编译Module子类的时候，需要装载Module的类。
		// 2. 如果使用Cl树，那么编译每个HotModule都需要设置一个子Cl。
		// 3. 参考osgi机制。横向把请求转给HotModule来装载类。
		// *. 重载这个方法实现；把GenModule.compiler.useParentClassLoader(this);
		// * parent? findLoadedClass?
		// * 这个Cl是纯粹代理的，按module优先实现横向查找，剩下的可以按标准路线走。

		// 已经生成好的Redirect_全局类。
		if (name.startsWith(GenModule.REDIRECT_PREFIX) && name.contains("_Module")) {
			name = name.substring(GenModule.REDIRECT_PREFIX.length()).replace("_", ".");
		}

		var cl = manager.findHotModule(name);
		if (null != cl)
			return cl.loadClass(name);
		return super.loadClass(name, resolve);
	}
}
