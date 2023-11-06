package Zeze.Hot;

import java.io.File;
import java.nio.file.Path;
import java.util.TreeMap;
import java.util.jar.JarFile;
import Zeze.Config;
import Zeze.Net.Service;
import Zeze.Transaction.Bean;
import Zeze.Util.Benchmark;
import Zeze.Util.Reflect;

public class DistributeServer {
	private static final TreeMap<String, Bean> beans = new TreeMap<>();
	private static int count = 0;
	private static String solution = null;

	public static TreeMap<String, Bean> getBeans() {
		return beans;
	}

	public static void main(String[] args) throws Exception {
		String host = "127.0.0.1";
		int port = 4545;

		for (var i = 0; i < args.length; ++i) {
			switch (args[i]) {
			case "-solution":
				solution = args[++i];
				break;
			case "-host":
				host = args[++i];
				break;
			case "-port":
				port = Integer.parseInt(args[++i]);
				break;
			}
		}

		if (null == solution)
			throw new RuntimeException("-solution must present.");

		// 只搜索solution名字空间下的bean。创建实例并加入容器。
		var b = new Benchmark();
		for (var path : Reflect.collectClassPaths(ClassLoader.getSystemClassLoader())) {
			if (path.endsWith(".jar")) {
				//System.out.println("---->" + path);
				loadJar(path);
				continue;
			}
			if (path.endsWith(".class")) {
				System.out.println(path + "<----------------");
				continue;
			}
			var file = new File(path);
			if (file.isDirectory()) {
				//System.out.println("---->" + file);
				loadBean(file.toPath(), file);
				continue;
			}
			System.out.println(path + " what?");
		}
		b.report("DistributeServer Classes", count);
		b.report("DistributeServer Beans", beans.size());

		var server = new ServerService();
		var hotDistribute = new HotDistribute();

		try {
			hotDistribute.RegisterProtocols(server);
			server.newServerSocket(host, port, null);
			server.start();

			//noinspection InfiniteLoopStatement
			while (true) {
				//noinspection BusyWait
				Thread.sleep(1000);
			}
		} finally {
			server.stop();
		}
	}

	private static void loadJar(String path) throws Exception {
		try (var jar = new JarFile(path)) {

			for (var e = jar.entries(); e.hasMoreElements(); ) {
				var entry = e.nextElement();
				var className = entry.getName().replace('\\', '/').replace('/', '.');
				if (className.endsWith(".class")) {
					className = className.substring(0, className.length() - ".class".length());
					loadBean(className);
				}
			}
		}
	}

	private static void loadBean(Path home, File dir) throws Exception {
		var files = dir.listFiles();
		if (null == files)
			return;

		for (var file : files) {
			if (file.isDirectory()) {
				loadBean(home, file);
				continue;
			}
			var relative = home.relativize(file.toPath());
			loadBean(relative.toString().replace('\\', '/').replace('/', '.'));
		}
	}

	private static void loadBean(String className) throws Exception {
		++count;
		//System.out.println(className);
		if (className.startsWith(solution)) {
			var cls = Class.forName(className);
			if (Bean.class.isAssignableFrom(cls)) // is bean
				beans.put(cls.getName(), (Bean)cls.getConstructor().newInstance());
		}
	}

	public static class ServerService extends Service {
		public ServerService() {
			super("Zeze.Hot.DistributeServer.ServerService", (Config)null);
		}
	}

}
