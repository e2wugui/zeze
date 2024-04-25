package Zeze.Services.ServiceManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Properties;
import javax.validation.constraints.Null;
import Zeze.Application;
import Zeze.Config;
import Zeze.Util.Task;
import org.jetbrains.annotations.NotNull;

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
					for (var e : edit.getPut())
						serviceSet.add(e.getServiceName());
					for (var e : edit.getUpdate())
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

	public void addExporter(@NotNull String name, @NotNull Properties shared, @Null String param) throws Exception {
		var config = new ExporterConfig(shared, param);
		exports.add((IExporter)Class.forName(name).getConstructor(ExporterConfig.class).newInstance(config));
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
		for (var i = 0; i < args.length; ++i) {
			if (args[i].equals("-e")) {
				var className = args[++i];
				// 如果还有参数，看看是不是跟随的-private，如果是，读取私有参数。
				String privateParam = null;
				if (i < args.length - 1 && args[i + 1].equals("-private") /* peek */) {
					privateParam = args[i+=2]; // move i to next 2
				}
				exporter.addExporter(className, shared, privateParam);
			} else if (args[i].equals("-s")) {
				services.add(args[++i]);
			} else if (args[i].equals("-d")) {
				exporter.addExporter("Zeze.Services.ServiceManager.ExporterPrint", shared, null);
			} else if (args[i].startsWith("-")) {
				// shared options
				// 先看有没有value。
				var key = args[i++]; // eat key and next
				String value = ""; // default for no value
				if (i < args.length && !args[i].startsWith("-") /* peek next if is value */)
					value = args[i++]; // eat value
				shared.put(key, value);
			} else {
				System.out.println("Usage: [options] -e class ... -s service ... ");
				System.out.println("    options: -version ver -file file -url url -relead cmd");
				System.out.println("    private options sample: -e class -private \"-version ver -file file -url url\"");
				System.out.println("    -private must follow \"-e class\", and will effect only for this instance.");
				throw new IllegalArgumentException();
			}
		}
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
