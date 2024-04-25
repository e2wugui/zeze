package Zeze.Services.ServiceManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import Zeze.Application;
import Zeze.Config;
import Zeze.Util.Task;

/**
 * 【独立后台进程】
 *
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

	private void onEdit(BEditService edit) {
		HashSet<String> serviceSet = null;
		for (var ep : exports) {
			switch (ep.getType()) {
			case eAll:
				if (null == serviceSet) {
					// 收集不同的服务名字。
					serviceSet = new HashSet<String>();
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

	public void addExporter(String name, String param) throws Exception {
		exports.add((IExporter)Class.forName(name).getConstructor(String.class).newInstance(param));
	}

	public void subscribeService(java.util.List<String> services) {
		var sub = new BSubscribeArgument();
		for (var ser : services)
			sub.subs.add(new BSubscribeInfo(ser, 0));
		agent.subscribeServices(sub);
	}

	public static void main(String [] args) throws Exception {
		Task.tryInitThreadPool();
		var exporter = new Exporter();
		var services = new ArrayList<String>();
		for (var i = 0; i < args.length; ++i) {
			if (args[i].equals("-e"))
				exporter.addExporter(args[++i], args[++i]);
			else if (args[i].equals("-s"))
				services.add(args[++i]);
			else if (args[i].equals("-d"))
				exporter.addExporter("Zeze.Services.ServiceManager.ExporterPrint", "");
			else
				throw new IllegalArgumentException("Usage: -e exporterClass param1 ... -s serviceName ...");
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
