package Temp;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DemoMain {
	private static final Logger logger = LogManager.getLogger();
	public interface Ia {
		void helloA();
	}

	public interface Ib {
		void helloB();
	}

	static final String Impl =

			"""
					public class Ab implements Temp.DemoMain.Ia, Temp.DemoMain.Ib {
						@Override
						public void helloA() {
							System.out.println("helloA");
						}

						@Override
						public void helloB() {
							System.out.println("helloB");
						}

					}
					""";

	public static void main1(String[] args) throws Exception {
		System.out.println(ClassLoader.getSystemClassLoader());
		var compiler = new Zeze.Util.InMemoryJavaCompiler();
		var abClass = compiler.compile("Ab", Impl);
		System.out.println(abClass.getClassLoader());
		System.out.println(Ia.class.getClassLoader());
		System.out.println(Ib.class.getClassLoader());

		Ia ia = (Ia)abClass.getConstructor().newInstance();
		ia.helloA();
		Ib ib = (Ib)ia;
		ib.helloB();
	}

	public static void main(String[] args) throws IOException {
		System.out.println("中文");
		System.out.println(System.getProperty("user.name"));
		System.out.println(InetAddress.getLocalHost().getHostName());
		logger.info("中文");
		logger.info(System.getProperty("user.name"));
		if (args.length == 0)
			return;
		var a = new File("a/b");
		var b = new File("a/../a/b");
		var c = new File("D:\\zeze\\ZezeJava\\a\\b");
		System.out.println(a.getPath()); // a\b
		System.out.println(b.getPath()); // a\..\a\b
		System.out.println(c.getPath()); // D:\zeze\ZezeJava\a\b

		System.out.println(a.getAbsolutePath()); // D:\zeze\ZezeJava\a\b
		System.out.println(b.getAbsolutePath()); // D:\zeze\ZezeJava\a\..\a\b
		System.out.println(c.getAbsolutePath()); // D:\zeze\ZezeJava\a\b

		System.out.println(b.getCanonicalPath()); // D:\zeze\ZezeJava\a\b
		System.out.println(b.getCanonicalFile().getPath()); // D:\zeze\ZezeJava\a\b

		System.out.println(a.hashCode()); // 1286710
		System.out.println(b.hashCode()); // -1336709385
		System.out.println(c.hashCode()); // -30160750

		System.out.println(a.getCanonicalFile().hashCode()); // -30160750
		System.out.println(b.getCanonicalFile().hashCode()); // -30160750
		System.out.println(c.getCanonicalFile().hashCode()); // -30160750

		System.out.println(new File(".").getPath()); // .
		System.out.println(new File(".").getAbsolutePath()); // D:\zeze\ZezeJava\.
		System.out.println(new File("").getPath()); //
		System.out.println(new File("").getAbsolutePath()); // D:\zeze\ZezeJava
	}
}
