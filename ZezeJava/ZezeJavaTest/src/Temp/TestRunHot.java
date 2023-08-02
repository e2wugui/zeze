package Temp;

import java.nio.file.Path;
import Zeze.Hot.HotManager;

public class TestRunHot {
	public static void main(String []args) throws Exception {
		// 【注意】每次运行这个测试，都需要先运行 zeze\ZezeJava\ZezeJavaTest\hot\distribute.bat。

		var workingDir = Path.of("ZezeJavaTest\\hot");
		var distributeDir = Path.of("ZezeJavaTest\\hot\\distributes");
		var manager = new HotManager(workingDir, distributeDir);
		// 由于测试的module.class和interface.class也存在与本AppClassLoader的路径中。
		// 按ClassLoader.Parent优先规则，实际装载这些类的ClassLoader是AppClassLoader，而不是HotManager。
		// 这样测试能运行，但没有完全模拟出实际热更模式。
		var module = manager.install("Temp");
		var service = module.getService();
		service.test(manager);
		System.out.println(manager.getClass().getClassLoader());
		manager.stopAndJoin();
	}
}
