package Zeze.Services.ServiceManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Properties;
import Zeze.Application;
import Zeze.Config;
import Zeze.Util.KV;
import Zeze.Util.Task;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 【独立后台进程】
 * <p>
 * 订阅sm，并输出结果到其他系统。
 * 当前需求是：ngnix-config-file, ngnix-config-http
 */
public class Exporter {
	private final AbstractAgent agent;
	private final java.util.List<IExporter> exports = new ArrayList<>();

	public Exporter() throws Exception {
		var conf = Config.load();
		agent = Application.createServiceManager(conf, "ServiceManagerRaftExporter");
		if (null == agent)
			throw new IllegalStateException("agent is null. check your config.");
	}

	public void start() throws Exception {
		agent.setOnChanged(this::onEdit);
		agent.start();
		agent.waitReady();
	}

	private void onEdit(BEditService edit) throws Exception {
		HashSet<String> serviceSet = null;
		for (var ep : exports) {
			switch (ep.getType()) {
			case eAll:
				if (null == serviceSet) {
					// 收集不同的服务名字。
					serviceSet = new HashSet<>();
					for (var e : edit.getRemove())
						serviceSet.add(e.getServiceName());
					for (var e : edit.getAdd())
						serviceSet.add(e.getServiceName());
				}
				for (var serviceName : serviceSet) {
					ep.exportAll(serviceName, agent.getSubscribeStates().get(serviceName).getServiceInfosVersion());
				}
				break;
			case eEdit:
				ep.exportEdit(edit);
				break;
			}
		}
	}

	public void stop() throws IOException {
		agent.close();
	}

	public void addExporter(@NotNull String name, @NotNull Properties shared, @Nullable String param) throws Exception {
		switch (name) {
		case "NginxConfig": exports.add(new ExporterNginxConfig(new ExporterConfig(shared, param))); break;
		case "NginxHttp": exports.add(new ExporterNginxHttp(new ExporterConfig(shared, param))); break;
		case "Print": exports.add(new ExporterPrint(null)); break;
		}
	}

	public void subscribeService(java.util.List<String> services) {
		var sub = new BSubscribeArgument();
		for (var ser : services)
			sub.subs.add(new BSubscribeInfo(ser, 0));
		agent.subscribeServices(sub);
	}

	public static void main(String[] args) throws Exception {
		Task.tryInitThreadPool();
		var exporter = new Exporter();
		var shared = new Properties();
		var services = new ArrayList<String>();
		var exporters = new ArrayList<KV<String, String>>();
		for (var i = 0; i < args.length; ++i) {
			if (args[i].equals("-e")) {
				var className = args[++i];
				// 如果还有参数，看看是不是跟随的-private，如果是，读取私有参数。
				String privateParam = null;
				if (i < args.length - 1 && args[i + 1].equals("-private") /* peek */) {
					privateParam = args[i+=2]; // move i to next 2
				}
				exporters.add(KV.create(className, privateParam));
			} else if (args[i].equals("-s")) {
				services.add(args[++i]);
			} else if (args[i].equals("-d")) {
				exporter.addExporter("Print", shared, null);
			} else if (args[i].startsWith("-")) {
				// shared options
				// 先看有没有value。
				var key = args[i]; // eat key and next
				String value = ""; // default for no value
				if (i + 1 < args.length && !args[i + 1].startsWith("-") /* peek next if is value */)
					value = args[++i]; // eat value
				shared.put(key, value);
			} else {
				System.out.println("Usage: [shared_options] -e class [-private options]... -s service ... ");
				System.out.println("    shared_options: -version ver -file file -url url -reload cmd");
				System.out.println("    -private options: same as shared_options, and will overwrite shared_options.");
				System.out.println("    -private must follow \"-e class\", and only effect this class instance.");
				throw new IllegalArgumentException();
			}
		}
		for (var e : exporters)
			exporter.addExporter(e.getKey(), shared, e.getValue());
		exporter.start();
		exporter.subscribeService(services);
		try {
			synchronized (Thread.currentThread()) {
				Thread.currentThread().wait();
			}
		} finally {
			exporter.stop();
		}
	}
}
