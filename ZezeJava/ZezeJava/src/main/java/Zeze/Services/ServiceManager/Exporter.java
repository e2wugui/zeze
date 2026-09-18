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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 【独立后台进程】
 * <p>
 * 订阅sm，并输出结果到其他系统。
 * 当前需求是：nginx-config-file, nginx-config-http
 */
public class Exporter {
	private static final @NotNull Logger logger = LogManager.getLogger(Exporter.class);
	private final AbstractAgent agent;
	private final java.util.List<IExporter> exports = new ArrayList<>();
	// 失败待补偿记账：按（导出器,服务名）二元组登记（FND8-68）——原全体eAll导出器共享
	// 一个服务名集合，混合成败时（如NginxConfig磁盘满+NginxHttp正常）后位成功者无条件
	// remove洗掉前位失败者的记账，被洗的导出器进程内永不再补偿、配置无限期陈旧。
	// 每个导出器只记自己欠的、只清自己还的；eEdit按整个BEditService导出（粒度非单服务，
	// 二元组键不匹配），失败仅记日志、无补偿记账（现库唯一实现ExporterPrint无失败面）。
	// onEdit串行运行于triggerOnChanged的one-by-one后台worker，理论无并发访问；
	// 仍用并发集合以降低对调用线程模型的隐含依赖。
	private final java.util.Map<IExporter, java.util.Set<String>> failedServices = new java.util.concurrent.ConcurrentHashMap<>();

	public Exporter() throws Exception {
		var conf = Config.load();
		agent = Application.createServiceManager(conf, "ServiceManagerRaftExporter");
		if (null == agent)
			throw new IllegalStateException("agent is null. check your config for ServiceManager.");
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
						// 收集不同的服务名字（各eAll导出器共用一份，只提取一次）。
						editNames = new HashSet<>();
						for (var e : edit.getRemove())
							editNames.add(e.getServiceName());
						for (var e : edit.getAdd())
							editNames.add(e.getServiceName());
					}
					// 每个导出器遍历"本次事件服务名 ∪ 自己的失败集"（FND8-68）：
					// 补偿只重导自己欠的，成败记账互不覆盖；已成功的导出器不重复补偿导出。
					var epFailed = failedServices.computeIfAbsent(ep, k -> java.util.concurrent.ConcurrentHashMap.newKeySet());
					var serviceSet = new HashSet<String>(editNames);
					serviceSet.addAll(epFailed);
					for (var serviceName : serviceSet) {
						// 与退订竞态（FND4-61）：triggerOnChanged经executeOneByOne异步排队，期间
						// unSubscribeService已remove该服务的subscribeStates——跳过为正确语义（已不
						// 关心）；原NPE被triggerOnChanged捕获记日志，同批其余服务的导出整体丢失。
						var state = agent.getSubscribeStates().get(serviceName);
						if (state == null) {
							// 已退订（不再关心）：同步清除失败记账（所有导出器中该服务条目），
							// 避免残留空条目随每次事件反复空转。
							for (var fs : failedServices.values())
								fs.remove(serviceName);
							continue;
						}
						try {
							ep.exportAll(serviceName, state.getServiceInfosVersion());
							// 导出成功：解除本导出器的失败记账（remove幂等，未登记时无副作用）。
							epFailed.remove(serviceName);
						} catch (Exception e) {
							// FND6-26：逐服务隔离——单服务导出失败记错继续，同批其余服务不受影响。
							logger.error("exportAll fail. exporter={}, service={}", ep.getClass().getName(), serviceName, e);
							// 失败记账：首次登记（add返回true）记warn说明补偿机制；
							// 之后任一后续事件都会带上该服务自动重导。
							if (epFailed.add(serviceName))
								logger.warn("exportAll failed, will re-export on next event. exporter={}, service={}",
										ep.getClass().getName(), serviceName);
							// 中断卫生：恢复中断标志，避免吞掉one-by-one worker的中断状态。
							if (e instanceof InterruptedException)
								Thread.currentThread().interrupt();
						}
					}
					break;
				case eEdit:
					// eEdit失败无补偿记账（粒度为整个edit，见failedServices注释）。
					ep.exportEdit(edit);
					break;
				}
			} catch (Exception e) {
				// FND6-26：逐exporter隔离——原任一exporter抛异常（文件IO失败、Runtime.exec失败等）
				// 中断整个onEdit，同批其余exporter与其余服务的导出全部跳过且无重试，nginx配置
				// 持续陈旧直到下一事件。FND4-61只修了state==null的NPE特例，未覆盖一般异常。
				logger.error("export fail. exporter={}", ep.getClass().getName(), e);
				// 中断卫生：恢复中断标志，避免吞掉one-by-one worker的中断状态。
				//noinspection ConstantValue
				if (e instanceof InterruptedException)
					Thread.currentThread().interrupt();
			}
		}
	}

	public void stop() throws IOException {
		agent.close();
		// 停机收尾：逐个释放exporter底层资源（如ExporterNginxHttp的HttpClient线程池），
		// 防止进程停机线程泄漏；单个close失败仅记日志，不影响其余exporter与停机流程。
		for (var ep : exports) {
			try {
				ep.close();
			} catch (Throwable e) {
				logger.error("exporter close fail. exporter={}", ep.getClass().getName(), e);
			}
		}
	}

	public void addExporter(@NotNull String name, @NotNull Properties shared, @Nullable String param) {
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
			// FND4-62：未知导出器名（拼写错误等）原静默忽略——部署配置错误零反馈。fail-fast
			// 启动失败并提示合法值。
			throw new IllegalArgumentException("unknown exporter '" + name + "', valid: NginxConfig | NginxHttp | Print");
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
					privateParam = args[i += 2]; // move i to next 2
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
