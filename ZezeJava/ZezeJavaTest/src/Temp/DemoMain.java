package Temp;

public class DemoMain {
	public static void main(String[] args) throws Exception {
		var jarFile = "C:\\code\\zeze\\ZezeJava\\ZezeJavaTest\\build\\classes\\java\\main\\m.jar";
		var hot = new Zeze.Hot.HotClassLoader("", new java.io.File(jarFile));
		var configClass = hot.loadModuleClass("Temp.ModuleA");
		var config = (Temp.IModuleInterface)configClass.getConstructor().newInstance();
		config.helloWorld();
		// TODO interface 使用问题？
	}
}
