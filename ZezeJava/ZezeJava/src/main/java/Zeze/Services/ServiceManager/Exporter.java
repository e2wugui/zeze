package Zeze.Services.ServiceManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Properties;
import Zeze.Application;
import Zeze.Config;
import Zeze.Util.KV;
import Zeze.Util.Task;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.Nullable;

/**
 * 独立后台进程：订阅 ServiceManager 服务变更并导出到外部系统（如 Nginx 配置或 HTTP 接口）。
 */
public class Exporter {
	private static final Logger logger = LogManager.getLogger(Exporter.class);
	private final AbstractAgent agent;
	private final java.util.List<IExporter> exports = new ArrayList<>();
	// 按导出器分别记录 eAll 失败服务，供后续事件补偿重导，避免不同导出器成败互相覆盖。
	private final java.util.Map<IExporter, java.util.Set<String>> failedServices = new java.util.concurrent.ConcurrentHashMap<>();

	public Exporter() throws Exception {
		var conf = Config.load();
		var createdAgent = Application.createServiceManager(conf, "ServiceManagerRaftExporter");
		if (null == createdAgent)
			throw new IllegalStateException("agent is null. check your config for ServiceManager.");
		agent = createdAgent;
	}

	public void start() throws Exception {
		agent.setOnChanged(this::onEdit);
		agent.start();
		agent.waitReady();
	}

	private void onEdit(BEditService edit) {
		HashSet<String> editNames = null;
		for (var ep : exports) {
			try {
				switch (ep.getType()) {
				case eAll:
					if (null == editNames) {
						editNames = new HashSet<>();
						for (var e : edit.getRemove())
							editNames.add(e.getServiceName());
						for (var e : edit.getAdd())
							editNames.add(e.getServiceName());
					}
					// 合并本次变更与本导出器的待补偿失败集。
					var epFailed = failedServices.computeIfAbsent(ep, k -> java.util.concurrent.ConcurrentHashMap.newKeySet());
					var serviceSet = new HashSet<String>(editNames);
					serviceSet.addAll(epFailed);
					for (var serviceName : serviceSet) {
						// 异步回调期间可能已退订，跳过并清理各导出器中残留的失败记账。
						var state = agent.getSubscribeStates().get(serviceName);
						if (state == null) {
							for (var fs : failedServices.values())
								fs.remove(serviceName);
							continue;
						}
						try {
							ep.exportAll(serviceName, state.getServiceInfosVersion());
							epFailed.remove(serviceName);
						} catch (Exception e) {
							logger.error("exportAll fail. exporter={}, service={}", ep.getClass().getName(), serviceName, e);
							if (epFailed.add(serviceName))
								logger.warn("exportAll failed, will re-export on next event. exporter={}, service={}",
										ep.getClass().getName(), serviceName);
							if (e instanceof InterruptedException)
								Thread.currentThread().interrupt();
						}
					}
					break;
				case eEdit:
					ep.exportEdit(edit);
					break;
				}
			} catch (Exception e) {
				// 隔离单个导出器异常，避免影响同批其他导出器。
				logger.error("export fail. exporter={}", ep.getClass().getName(), e);
				//noinspection ConstantValue
				if (e instanceof InterruptedException)
					Thread.currentThread().interrupt();
			}
		}
	}

	public void stop() throws IOException {
		try {
			agent.close();
		} finally {
			// agent关闭失败时也要释放各导出器的资源。
			for (var ep : exports) {
				try {
					ep.close();
				} catch (Throwable e) {
					logger.error("exporter close fail. exporter={}", ep.getClass().getName(), e);
				}
			}
		}
	}

	public void addExporter(String name, Properties shared, @Nullable String param) {
		switch (name) {
		case "NginxConfig":
			exports.add(new ExporterNginxConfig(new ExporterConfig(shared, param)));
			break;
		case "NginxHttp":
			exports.add(new ExporterNginxHttp(new ExporterConfig(shared, param)));
			break;
		case "Print":
			exports.add(new ExporterPrint(null));
			break;
		default:
			throw new IllegalArgumentException("unknown exporter '" + name + "', valid: NginxConfig | NginxHttp | Print");
		}
	}

	public void subscribeService(java.util.List<String> services) {
		var sub = new BSubscribeArgument();
		for (var ser : services)
			sub.subs.add(new BSubscribeInfo(ser, 0));
		agent.subscribeServices(sub);
	}

	static void parseArgs(String [] args, Properties shared,
						   java.util.List<String> services,
						   java.util.List<KV<String, @Nullable String>> exporters) {
		for (var i = 0; i < args.length; ++i) {
			if (args[i].equals("-e")) {
				if (i + 1 >= args.length)
					throw usageError("'-e' requires a class name argument");
				var className = args[++i];
				String privateParam = null;
				if (i + 1 < args.length && args[i + 1].equals("-private") /* peek */) {
					if (i + 2 >= args.length)
						throw usageError("'-private' requires an options argument");
					var next = args[i + 2];
					// -private 参数允许以 '-' 开头，但不能直接是顶层开关本身。
					if (next.equals("-e") || next.equals("-s") || next.equals("-d") || next.equals("-private"))
						throw usageError("'-private' requires an options argument, got top-level switch '" + next + "'");
					privateParam = args[i += 2];
				}
				exporters.add(KV.create(className, privateParam));
			} else if (args[i].equals("-s")) {
				if (i + 1 >= args.length)
					throw usageError("'-s' requires a service name argument");
				services.add(args[++i]);
			} else if (args[i].equals("-d")) {
				exporters.add(KV.create("Print", null));
			} else if (args[i].startsWith("-")) {
				// 共享选项
				var key = args[i];
				String value = "";
				if (i + 1 < args.length && !args[i + 1].startsWith("-") /* peek next if is value */)
					value = args[++i];
				shared.put(key, value);
			} else {
				throw usageError("unknown argument: '" + args[i] + "'");
			}
		}
	}

	private static IllegalArgumentException usageError(String message) {
		System.out.println("Usage: [shared_options] -e class [-private options]... -s service ... ");
		System.out.println("    shared_options: -version ver -file file -url url -reload cmd");
		System.out.println("    -private options: same as shared_options, and will overwrite shared_options.");
		System.out.println("    -private must follow \"-e class\", and only effect this class instance.");
		return new IllegalArgumentException(message);
	}

	public static void main(String[] args) throws Exception {
		Task.tryInitThreadPool();
		var exporter = new Exporter();
		var shared = new Properties();
		var services = new ArrayList<String>();
		var exporters = new ArrayList<KV<String, @Nullable String>>();
		parseArgs(args, shared, services, exporters);
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
