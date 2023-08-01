package Temp;

import java.nio.file.Path;
import Zeze.Hot.HotManager;

public class TestRunHot {
	public static void main(String []args) throws Exception {
		var dir = Path.of(".\\ZezeJavaTest\\hot\\interfaces");
		var manager = new HotManager(dir);
		Thread.sleep(1000);
		// 由于测试的module.class和interface.class也存在与本AppClassLoader的路径中。
		// 按ClassLoader.Parent优先规则，实际装载这些类的ClassLoader是AppClassLoader，而不是HotManager。
		// 这样测试能运行，但没有完全模拟出实际热更模式。
		var module = manager.put("Temp", Path.of(".\\ZezeJavaTest\\\\hot\\modules\\m.jar").toFile());
		var service = module.getService();
		service.test(manager);
		System.out.println(manager.getClass().getClassLoader());
		manager.stopAndJoin();
	}
}
