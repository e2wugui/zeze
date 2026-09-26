package Zeze.Services.ZokerImpl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import Zeze.Builtin.Zoker.BService;
import Zeze.Builtin.Zoker.StartService;
import Zeze.Builtin.Zoker.StopService;
import Zeze.IModule;
import Zeze.Services.Zoker;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

/**
 * 服务进程生命周期（GE-D01 方案A：部署描述文件约定）。
 *
 * <p>命令来源：services/&lt;svc&gt;/current 指向的版本目录下的部署描述文件
 * {@link #SERVICE_PROPERTIES_NAME}（Properties 格式：{@code command=} 必需；
 * {@code args=} 可选，空白分隔；{@code env=} 可选，k=v 逗号分隔），随分发内容自带，
 * commitService 原样落盘——"分发即自包含"，启动命令不进协议参数。
 * 描述文件缺失/不可解析（含无现役版本）→ eNoServiceProperties；进程创建失败 → eStartFail。
 * 两者都是协议错误码不再异常上抛（上抛无结果包，客户端只能等满 RPC 超时）。</p>
 *
 * <p>进程记账与真实进程一致：启动装账后挂 {@code onExit()} 退出监控，进程退出时清理
 * processes 条目并记录退出码；listService 以 {@code isAlive()} 判 Running（死条目不报
 * running，条目在但进程死=Stopped）；对死条目再 start 会检查 isAlive 并替换重启
 * （不先 stopService 也能重新拉起）。</p>
 *
 * <p>stopService 结局三态写进 Result.State（BService.State 字符串语义，零协议形状变更）：
 * Stopped（优雅退出/本已退出）/ Force-Killed（超时强杀成功）/ Alive-After-Force（强杀仍存活），
 * Ps 字段携带退出码。</p>
 */
public class ServiceManager {
	private static final Logger logger = LogManager.getLogger(ServiceManager.class);

	/** 现役版本目录下的部署描述文件名，随分发内容自带。 */
	static final String SERVICE_PROPERTIES_NAME = "service.properties";
	static final String KEY_COMMAND = "command";
	static final String KEY_ARGS = "args";
	static final String KEY_ENV = "env";

	// BService.State 的取值（协议 bean 注释语义：Running,Stopped；stop 结局细化三态）
	static final String STATE_RUNNING = "Running";
	static final String STATE_STOPPED = "Stopped";
	static final String STATE_FORCE_KILLED = "Force-Killed";
	static final String STATE_ALIVE_AFTER_FORCE = "Alive-After-Force";

	// 进程可捕获/忽略SIGTERM，无界waitFor会永久占用Normal派发线程并累积耗尽线程池，
	// 等待全部有界：优雅退出窗口超时即强杀；强杀仍不退出的属于极端残留，记录后放弃。
	private static final long STOP_GRACEFUL_SECONDS = 10;
	private static final long STOP_FORCE_SECONDS = 10;

	private final @Nullable Zoker zoker;
	private final File serviceDir;
	private final ConcurrentHashMap<String, Process> processes = new ConcurrentHashMap<>();

	public ServiceManager(Zoker zoker) {
		this.zoker = zoker;
		this.serviceDir = zoker.getServiceDir();
	}

	/** 直构测试形态：不依赖 Zoker（网络服务），直接给定 services/ 布局根目录。 */
	ServiceManager(File serviceDir) {
		this.zoker = null;
		this.serviceDir = serviceDir;
	}

	public @Nullable Zoker getZoker() {
		return zoker;
	}

	public void listService(ArrayList<BService.Data> out) {
		// GE-D02 新布局：services/ 的每个子目录是一个服务容器（services/<svc>/<versionNo>/... + current），
		// 服务存在性仍以"services/<svc> 目录存在"为准；运行状态来自本进程的processes记账，
		// 以 isAlive 判定（GE-D01）：死条目（onExit回调未及清理的窗口）不报 running。
		var listFiles = serviceDir.listFiles();
		if (null != listFiles) {
			for (var file : listFiles) {
				if (file.isDirectory()) {
					var service = new BService.Data();
					service.setServiceName(file.getName());
					var process = processes.get(service.getServiceName());
					if (null != process && process.isAlive()) {
						service.setState(STATE_RUNNING);
						service.setPs(process.info().toString());
					} else if (null != process) {
						// 条目在但进程死=Stopped；无条目（从未由本Zoker启动）保持""（原语义）
						service.setState(STATE_STOPPED);
					}
					out.add(service);
				}
			}
		}
	}

	/** 启动描述：command+args 组命令、env 注入进程环境、工作目录=现役版本目录。 */
	static final class LaunchSpec {
		final File workingDir;
		final List<String> command = new ArrayList<>();
		final Map<String, String> env = new LinkedHashMap<>();

		LaunchSpec(File workingDir) {
			this.workingDir = workingDir;
		}
	}

	/**
	 * 解析版本目录下的 {@code service.properties}（包内静态供直构测试）。
	 * 文件缺失（FileNotFoundException）/command 键缺失或为空/env 条目非法 → IOException，
	 * 调用方统一映射 eNoServiceProperties（缺少可用的部署描述文件）。
	 * 约定：args 按空白分隔不支持引号包裹；env 值内不支持逗号。
	 */
	static LaunchSpec parseLaunchSpec(File currentVersionDir) throws IOException {
		var file = new File(currentVersionDir, SERVICE_PROPERTIES_NAME);
		var props = new Properties();
		try (var input = new FileInputStream(file)) {
			props.load(input);
		}
		var command = props.getProperty(KEY_COMMAND, "").trim();
		if (command.isEmpty())
			throw new IOException("service.properties missing 'command': " + file);
		var spec = new LaunchSpec(currentVersionDir);
		spec.command.add(command);
		var args = props.getProperty(KEY_ARGS, "").trim();
		if (!args.isEmpty())
			spec.command.addAll(Arrays.asList(args.split("\\s+")));
		var env = props.getProperty(KEY_ENV, "").trim();
		if (!env.isEmpty()) {
			for (var pair : env.split(",")) {
				var kv = pair.split("=", 2);
				var key = kv[0].trim();
				if (kv.length != 2 || key.isEmpty())
					throw new IOException("service.properties bad 'env' entry '" + pair + "': " + file);
				spec.env.put(key, kv[1]);
			}
		}
		return spec;
	}

	private LaunchSpec loadLaunchSpec(String serviceName) throws IOException {
		// GE-D02 新布局：服务文件在 services/<svc>/<current指向的版本>/ 下。
		// 无现役指针（从未commit/现场被破坏）= 描述文件必然缺失，同映射 eNoServiceProperties。
		var workingDir = DistributeManager.currentVersionDir(new File(serviceDir, serviceName));
		if (null == workingDir)
			throw new FileNotFoundException("service has no current version: " + serviceName);
		return parseLaunchSpec(workingDir);
	}

	private static Process launch(LaunchSpec spec) throws IOException {
		var pb = new ProcessBuilder();
		pb.directory(spec.workingDir);
		pb.command(spec.command);
		if (!spec.env.isEmpty())
			pb.environment().putAll(spec.env);
		return pb.start();
	}

	/**
	 * 进程退出监控（GE-D01）：启动装账后注册，进程退出时清理processes条目并记录退出码，
	 * 死进程不再占用"running"语义。
	 * {@code remove(key, process)} 条件移除：startService 替换重启死句柄后，
	 * 旧句柄迟到的退出回调不会误删新条目。
	 */
	private void watchExit(String serviceName, Process process) {
		process.onExit().whenComplete((exited, ex) -> {
			if (ex != null) {
				logger.error("service onExit error: {}", serviceName, ex);
				return;
			}
			if (processes.remove(serviceName, process))
				logger.info("service exited: {} exitCode={}", serviceName, exitCode(exited));
		});
	}

	private static int exitCode(Process process) {
		try {
			return process.exitValue();
		} catch (IllegalThreadStateException ex) {
			return -1;
		}
	}

	/**
	 * 启动服务。成功填 Result（State=Running）返回 0；失败返回协议错误码
	 * （eNoServiceProperties/eStartFail），不发结果包。
	 */
	public long startService(StartService r) {
		var serviceName = r.Argument.getServiceName();
		Process process;
		while (true) {
			var existing = processes.get(serviceName);
			if (null != existing && existing.isAlive()) {
				// 已在运行：复用现役句柄（幂等），不重复拉起
				process = existing;
				break;
			}
			// 条目缺失或死句柄（进程自然退出/被外部杀死，onExit回调未及清理的窗口）：（重新）拉起
			LaunchSpec spec;
			try {
				spec = loadLaunchSpec(serviceName);
			} catch (IOException ex) {
				logger.error("startService {} no usable service.properties", serviceName, ex);
				return err(Zoker.eNoServiceProperties);
			}
			Process candidate;
			try {
				candidate = launch(spec);
			} catch (IOException ex) {
				logger.error("startService {} launch fail: {}", serviceName, spec.command, ex);
				return err(Zoker.eStartFail);
			}
			var installed = existing == null
					? processes.putIfAbsent(serviceName, candidate) == null
					: processes.replace(serviceName, existing, candidate);
			if (installed) {
				// 装账后再挂退出监控：先挂监控可能赶在装账前触发回调，remove(key,process)错失清理
				watchExit(serviceName, candidate);
				process = candidate;
				break;
			}
			// 并发start竞争落败：丢弃候选重试（对方或为活进程→复用，或又一条死句柄→再启）
			logger.warn("startService {} concurrent start, discard candidate", serviceName);
			candidate.destroyForcibly();
		}
		r.Result.setServiceName(serviceName);
		r.Result.setState(STATE_RUNNING);
		r.Result.setPs(process.info().toString());
		return 0;
	}

	/**
	 * 停止服务，结局三态写进 Result（GE-D01，零协议形状变更）：
	 * Stopped（优雅退出/停时本已退出，Ps=exit=N）/ Force-Killed（超时强杀成功，Ps=exit=N）/
	 * Alive-After-Force（强杀限期后仍存活的极端残留，error日志，Ps=进程info）。
	 * 未运行（从未启动/已停止/已被onExit清理）幂等成功：State=Stopped，Ps=not-running。
	 */
	public void stopService(StopService r) throws InterruptedException {
		var serviceName = r.Argument.getServiceName();
		var process = processes.remove(serviceName);
		r.Result.setServiceName(serviceName);
		if (null == process) {
			r.Result.setState(STATE_STOPPED);
			r.Result.setPs("not-running");
			return;
		}
		if (!process.isAlive()) {
			// 死条目（自然退出/外部杀死后onExit未及清理）：结局已定，汇报自然退出码
			r.Result.setState(STATE_STOPPED);
			r.Result.setPs(formatPs(process.exitValue()));
			return;
		}

		if (!r.Argument.isForce() && supportsNormalTermination(process)) {
			process.destroy();
			try {
				if (process.waitFor(STOP_GRACEFUL_SECONDS, TimeUnit.SECONDS)) {
					r.Result.setState(STATE_STOPPED);
					r.Result.setPs(formatPs(process.exitValue()));
					return;
				}
				logger.warn("stopService graceful timeout {}s, force killing: {}", STOP_GRACEFUL_SECONDS, serviceName);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				// 中断不能留下已从processes移除但存活的进程，落下去强杀
			}
		}
		try {
			process.destroyForcibly();
			if (process.waitFor(STOP_FORCE_SECONDS, TimeUnit.SECONDS)) {
				r.Result.setState(STATE_FORCE_KILLED);
				r.Result.setPs(formatPs(process.exitValue()));
				return;
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
		// 强杀限期后仍存活（不可杀子进程残留等极端情形）：句柄交GC收殓，不再阻塞派发线程
		r.Result.setState(STATE_ALIVE_AFTER_FORCE);
		r.Result.setPs(process.info().toString());
		logger.error("stopService: process still alive after destroyForcibly: {}", serviceName);
	}

	/** Ps 字段携带退出码（死进程的 info() 多为空内容，退出码是停机结局的有效载荷）。 */
	private static String formatPs(int exitCode) {
		return "exit=" + exitCode;
	}

	private static boolean supportsNormalTermination(Process process) {
		try {
			return process.supportsNormalTermination();
		} catch (Exception ex) {
			return false;
		}
	}

	// 直构测试 seam：注入/读取进程记账（模拟 onExit 回调未及清理的死句柄窗口）
	Process getProcessForTest(String serviceName) {
		return processes.get(serviceName);
	}

	void putProcessForTest(String serviceName, Process process) {
		processes.put(serviceName, process);
	}

	// 错误码与 zoker.errorCode 同构（ModuleId*编码）；直构形态（zoker==null）下也要能返回协议错误码。
	private static long err(int code) {
		return IModule.errorCode(Zoker.ModuleId, code);
	}
}
