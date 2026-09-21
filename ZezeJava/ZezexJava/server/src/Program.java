import Zeze.Services.Daemon;

public class Program {
	public synchronized static void main(String[] args) throws Exception {
		System.setProperty(Daemon.propertyNameClearInUse, "true");
		//【用来生成出Redirect模块，调试用】
		// args = new String[] { "-GenFileSrcRoot", "C:\\code\\zeze\\ZezeJava\\ZezexJava\\server\\src" };
		var genFileSrcRoot = Game.App.getInstance().Start(args);
		if (genFileSrcRoot != null)
			return; // 生成模式：代码已生成，不进入服务wait，进程自然退出
		try {
			Program.class.wait();
		} finally {
			Game.App.getInstance().Stop();
		}
	}
}
