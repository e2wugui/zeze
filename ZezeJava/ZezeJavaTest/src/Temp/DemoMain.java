package Temp;

import java.io.File;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.concurrent.Future;
import Zeze.Component.AutoKey;
import Zeze.Transaction.Bean;
import Zeze.Util.StableRandom;
import Zeze.Util.Task;
import org.apache.logging.log4j.Level;
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

	public static void main(String[] args) throws Exception {
		System.out.println(Bean.hash64("UnitTest.Zeze.Component.TestBean"));
		if (args.length == 0)
			return;
		System.out.println(AutoKey.getServerIdFromId(2167583089L));
		System.out.println(AutoKey.getServerIdFromId(6195073));

		Task.tryInitThreadPool();
		var futures = new ArrayList<Future<?>>(100);
		var seed = System.currentTimeMillis();
		for (int i = 0; i < 100; i += 3) {
			var s = seed++;
			var seed2 = s ^ i;
			futures.add(Task.runUnsafe(() -> {
				var r = StableRandom.local();
				r.setSeed(seed2);
				System.out.println(r.nextLong(65536));
			}, "runUnsafe"));
		}
		for (var future : futures)
			future.get();
		if (args.length == 0)
			return;
		/*

		var lq = new ConcurrentLinkedQueue<Integer>();
		lq.add(1);
		lq.add(2);
		lq.add(3);
		for (var e : lq) {
			System.out.println(e);
			lq.poll();
		}
		if (args.length == 0)
			return;
		*/
		//noinspection RedundantCast
		logger.log(Level.WARN, "n={}, s={}, p={}", 123, "abc", args, (Exception)null);
		System.out.println("中文");
		System.out.println(System.getProperty("user.name"));
		System.out.println(InetAddress.getLocalHost().getHostName());
		logger.info("中文");
		logger.info(System.getProperty("user.name"));
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
