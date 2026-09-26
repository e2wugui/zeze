package Zeze.Services.ZokerImpl;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import Zeze.Builtin.Zoker.BService;
import Zeze.Builtin.Zoker.StartService;
import Zeze.Builtin.Zoker.StopService;
import Zeze.Services.Zoker;
import Zeze.Util.Task;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ServiceManager {
	private static final Logger logger = LogManager.getLogger(ServiceManager.class);

	// 进程可捕获/忽略SIGTERM，无界waitFor会永久占用Normal派发线程并累积耗尽线程池，
	// 等待全部有界：优雅退出窗口超时即强杀；强杀仍不退出的属于极端残留，记录后放弃。
	private static final long STOP_GRACEFUL_SECONDS = 10;
	private static final long STOP_FORCE_SECONDS = 10;

	private final Zoker zoker;
	private final ConcurrentHashMap<String, Process> processes = new ConcurrentHashMap<>();

	public ServiceManager(Zoker zoker) {
		this.zoker = zoker;
	}

	public Zoker getZoker() {
		return zoker;
	}

	public void listService(ArrayList<Zeze.Builtin.Zoker.BService.Data> out) {
		// GE-D02 新布局：services/ 的每个子目录是一个服务容器（services/<svc>/<versionNo>/... + current），
		// 服务存在性仍以"services/<svc> 目录存在"为准；运行状态来自本进程的processes记账。
		var listFiles = zoker.getServiceDir().listFiles();
		if (null != listFiles) {
			for (var file : listFiles) {
				if (file.isDirectory()) {
					var service = new BService.Data();
					service.setServiceName(file.getName());
					var process = processes.get(service.getServiceName());
					service.setState(null != process ? "running" : "");
					if (null != process)
						service.setPs(process.info().toString());
					out.add(service);
				}
			}
		}
	}

	private static List<String> buildCommand(@SuppressWarnings("unused") String serviceName) {
		return new ArrayList<>();
	}

	private Process newProcess(String serviceName) {
		var pb = new ProcessBuilder();
		// GE-D02 新布局：服务文件在 services/<svc>/<current指向的版本>/ 下，工作目录解析 current 指针；
		// 无现役指针（从未commit/现场被破坏）属于明确失败，不再回退到容器目录（那里只有版本目录，没有服务文件）。
		var workingDir = DistributeManager.currentVersionDir(new File(zoker.getServiceDir(), serviceName));
		if (null == workingDir)
			throw Task.forceThrow(new IOException("service has no current version: " + serviceName));
		pb.directory(workingDir);
		pb.command(buildCommand(serviceName));
		try {
			return pb.start();
		} catch (IOException e) {
			throw Task.forceThrow(e);
		}
	}

	public void startService(StartService r) {
		var serviceName = r.Argument.getServiceName();
		var process = processes.computeIfAbsent(serviceName, __ -> newProcess(serviceName));
		r.Result.setServiceName(serviceName);
		r.Result.setState("running");
		r.Result.setPs(process.info().toString());
	}

	public int stopService(StopService r) throws InterruptedException {
		var serviceName = r.Argument.getServiceName();
		var process = processes.remove(serviceName);
		if (null == process)
			return 0;

		r.Result.setServiceName(serviceName);
		r.Result.setState("");
		r.Result.setPs(process.info().toString());

		if (!r.Argument.isForce() && supportsNormalTermination(process)) {
			process.destroy();
			try {
				if (process.waitFor(STOP_GRACEFUL_SECONDS, TimeUnit.SECONDS))
					return process.exitValue();
				logger.warn("stopService graceful timeout {}s, force killing: {}", STOP_GRACEFUL_SECONDS, serviceName);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				// 中断不能留下已从processes移除但存活的进程，落下去强杀
			}
		}
		try {
			process.destroyForcibly();
			if (process.waitFor(STOP_FORCE_SECONDS, TimeUnit.SECONDS))
				return process.exitValue();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
		// 强杀限期后仍存活（不可杀子进程残留等极端情形）：句柄交GC收殓，不再阻塞派发线程
		logger.error("stopService: process still alive after destroyForcibly: {}", serviceName);
		return -1;
	}

	private static boolean supportsNormalTermination(Process process) {
		try {
			return process.supportsNormalTermination();
		} catch (Exception ex) {
			return false;
		}
	}
}
