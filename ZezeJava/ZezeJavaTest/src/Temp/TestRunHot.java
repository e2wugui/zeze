package Temp;

public class TestRunHot {
	public static void main(String []args) throws Exception {
		// 【注意】每次运行这个测试，都需要先运行 zeze\ZezeJava\ZezeJavaTest\hot\distribute.bat。

		var tempApp = new TempApp();
		// 由于测试的module.class和interface.class也存在与本AppClassLoader的路径中。
		// 按ClassLoader.Parent优先规则，实际装载这些类的ClassLoader是AppClassLoader，而不是HotManager。
		// 这样测试能运行，但没有完全模拟出实际热更模式。
		System.out.println("hot install ...");
		try {
			var module = tempApp.manager.installReadies(false);
			System.out.println(tempApp.manager.getClass().getClassLoader());
		} catch (Exception ex) {
			throw new RuntimeException("maybe run this 'zeze\\ZezeJava\\ZezeJavaTest\\hot\\distribute.bat'", ex);
		}
	}
}
