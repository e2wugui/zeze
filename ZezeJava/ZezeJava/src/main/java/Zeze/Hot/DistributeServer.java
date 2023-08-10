package Zeze.Hot;

import java.util.TreeMap;
import Zeze.Config;
import Zeze.Net.Service;
import Zeze.Transaction.Bean;
import Zeze.Util.Benchmark;
import Zeze.Util.Reflect;

public class DistributeServer {
	private final static TreeMap<String, Bean> beans = new TreeMap<>();

	public static TreeMap<String, Bean> getBeans() {
		return beans;
	}

	public static void main(String [] args) throws Exception {
		String solution = null;
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
		var count = 0;
		for (var path : Reflect.collectClassPaths(ClassLoader.getSystemClassLoader())) {
			++count;
			if (path.startsWith(solution)) {
				var cls = Class.forName(path);
				if (Bean.class.isAssignableFrom(cls)) // is bean
					beans.put(cls.getName(), (Bean)cls.getConstructor().newInstance());
			}
		}
		b.report("DistributeServer", count);

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

	public static class ServerService extends Service {
		public ServerService() {
			super("Zeze.Hot.DistributeServer.ServerService", (Config)null);
		}
	}

}
