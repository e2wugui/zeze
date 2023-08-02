package Zeze.Hot;

/**
 * module 引用其他 module 时，可以保存下来获得服务，
 * 不用每次都去查询。
 */
public class HotModuleContext {
	private volatile HotModule module;

	HotModuleContext(HotModule module) {
		this.module = module;
	}

	void setModule(HotModule module) {
		this.module = module;
	}

	public <T extends HotService> T getService() {
		return (T)module.getService();
	}
}
