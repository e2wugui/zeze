package Zeze.Hot;

import java.util.TreeMap;

public class ModuleManager {
	private final TreeMap<String, HotClassLoader> modules = new TreeMap<>();

	// ModuleInterface 采用其他管理措施以后，这个方法很可能不需要了。
	public HotClassLoader find(String className) {
		// 因为存在子模块：
		// 优先匹配长的名字。
		// TreeMap是否有更优算法？
		for (var e : modules.descendingMap().entrySet()) {
			if (className.startsWith(e.getKey()))
				return e.getValue();
		}
		return null; // throw ?
	}

	public void put(HotClassLoader hot /* 先用这个类描述热更单位 */) {
		var exist = modules.get(hot.getNamespace());
		if (exist == null) {
			hot.start();
			modules.put(hot.getNamespace(), hot);
		}
		upgrade(exist, hot);
	}

	private void upgrade(HotClassLoader old, HotClassLoader hot) {
		old.stop(); // todo 生命期管理，确定服务是否可用，等等。
		hot.upgrade(old);
		hot.start();
		modules.put(hot.getNamespace(), hot);
	}
}
