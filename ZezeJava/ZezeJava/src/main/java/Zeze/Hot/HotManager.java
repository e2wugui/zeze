package Zeze.Hot;

import java.util.TreeMap;

public class HotManager {
	private final TreeMap<String, HotClassLoader> classLoaders = new TreeMap<>();

	// ModuleInterface 采用其他管理措施以后，这个方法很可能不需要了。
	public HotClassLoader find(String className) {
		// 因为存在子模块：
		// 优先匹配长的名字。
		// TreeMap是否有更优算法？
		for (var e : classLoaders.descendingMap().entrySet()) {
			if (className.startsWith(e.getKey()))
				return e.getValue();
		}
		return null; // throw ?
	}

	public void install(HotClassLoader hot /* 先用这个类描述热更单位 */) {
		if (null != classLoaders.putIfAbsent(hot.getNamespace(), hot))
			throw new RuntimeException();
		hot.start();
	}

	public void upgrade(HotClassLoader hot) {
		var old = classLoaders.get(hot.getNamespace());
		if (null == old)
			throw new RuntimeException();
		old.stop();
		hot.upgrade(old);
		hot.start();
		classLoaders.put(hot.getNamespace(), hot);
		// 模块生命期管理：不支持uninstall。细节还没定。
	}
}
