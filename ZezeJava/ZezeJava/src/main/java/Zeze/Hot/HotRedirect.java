package Zeze.Hot;

public class HotRedirect extends ClassLoader {
	private final HotManager manager;

	public HotRedirect(HotManager manager) {
		this.manager = manager;
	}

	@Override
	public Class<?> loadClass(String name) throws ClassNotFoundException {
		// 1. Redirect编译Module子类的时候，需要装载Module的类。
		// 2. 如果使用Cl树，那么编译每个HotModule都需要设置一个子Cl。
		// 3. 参考osgi机制。横向把请求转给HotModule来装载类。
		// *. 重载这个方法实现；把GenModule.compiler.useParentClassLoader(this);
		// todo parent? findLoadedClass?
		var cl = manager.findHotModule(name);
		if (null != cl)
			return cl.loadClass(name);
		return super.loadClass(name);
	}
}
