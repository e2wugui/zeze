package Zeze.Services.ZokerImpl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import Zeze.Builtin.Zoker.BService;
import Zeze.Builtin.Zoker.StartService;
import Zeze.Builtin.Zoker.StopService;
import Zeze.IModule;
import Zeze.Services.Zoker;
import Zeze.Util.AtomicFileWriter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

/**
 * 服务进程生命周期（部署描述文件约定；进程记账领养）。
 *
 * <p>命令来源：services/&lt;svc&gt;/current 指向的版本目录下的部署描述文件
 * {@link #SERVICE_PROPERTIES_NAME}（行式 key=value 格式：{@code command=} 必需；
 * {@code args=} 可选，空白分隔；{@code env=} 可选，k=v 逗号分隔），随分发内容自带，
 * commitService 原样落盘——"分发即自包含"，启动命令不进协议参数。
 * 描述文件缺失/不可解析（含无现役版本）→ eNoServiceProperties；进程创建失败 → eStartFail。
 * 两者都是协议错误码，不异常上抛（上抛无结果包，客户端只能等满 RPC 超时）。</p>
 *
 * <p>进程记账与真实进程一致：启动装账后挂 {@code onExit()} 退出监控，进程退出时清理
 * processes 条目并记录退出码；listService 以 {@code isAlive()} 判 Running（死条目不报
 * running，条目在但进程死=Stopped）；对死条目再 start 会检查 isAlive 并替换重启
 * （不先 stopService 也能重新拉起）。</p>
 *
 * <p>stopService 结局三态写进 Result.State（BService.State 字符串语义，零协议形状变更）：
 * Stopped（优雅退出/本已退出）/ Force-Killed（超时强杀成功）/ Alive-After-Force（强杀仍存活），
 * Ps 字段携带退出码。</p>
 *
 * <p><b>盘上进程身份与跨 Zoker 重启的记账连续</b>：内存记账随 Zoker 进程消失，
 * 唯一跨生命周期可信的载体是盘上文件。服务启动成功时把进程身份原子写入容器根
 * {@code services/<svc>/run.pid}（与 current 指针同层、同 AtomicFileWriter 原语；容器根
 * 不随版本切换/清理消失——pruneVersions 只纳入目录，文件天然不在清理面）。内容=pid+
 * startInstant 指纹（辅 command 行）。三条路径共用同一身份解析 {@link #resolveRunPid}：</p>
 * <ul>
 * <li>启动对账 {@link #adoptOrphans}（Zoker.start 在 listen 前）：扫描各服务容器，身份核实
 * 通过（pid 存活且指纹相符）的孤儿领养装账+挂退出监控——list 可见、stop 可停；死/损坏/
 * 指纹不符的残留就地清理。</li>
 * <li>startService 查重：条目缺失/死时先解析 run.pid，存活且核实 → 领养并幂等返回
 * Running（Ps 标记 adopted+pid），绝不盲目双启；拉起路径写盘失败=不交付（destroy 候选+
 * eStartFail——盘是真相源，无盘身份的进程不允许存在）。</li>
 * <li>stopService 查重：条目缺失≠not-running，同样先解析 run.pid 再判；领养句柄可停。
 * 停毕与 onExit 回调按内容比对条件删除 run.pid（pid 仍是自己的才删）。</li>
 * </ul>
 * <p>领养门槛=身份核实通过，指纹不可核实时宁可失明（告警+视为无条目）绝不按裸 pid 领养
 * ——只杀领养过的，杜绝 PID 复用误杀（误杀比失明危险：失明的代价只是下次 start 重新拉起，
 * 误杀的代价是无辜进程）。领养句柄经 {@link AdoptedProcess} 适配入账（记账类型统一为
 * Process：纯 ProcessHandle 记账会丢掉 spawned 的
 * 退出码——ProcessHandle 无 exitValue，而 Ps=exit=N 是三态契约；适配后两种形态
 * 同型同语义，差异只在退出码/管道不可得）。零协议面变更。</p>
 */
public class ServiceManager {
	private static final Logger logger = LogManager.getLogger(ServiceManager.class);

	/** 现役版本目录下的部署描述文件名，随分发内容自带。 */
	static final String SERVICE_PROPERTIES_NAME = "service.properties";
	static final String KEY_COMMAND = "command";
	static final String KEY_ARGS = "args";
	static final String KEY_ENV = "env";

	/**
	 * 服务容器根的进程身份文件名：services/&lt;svc&gt;/run.pid，与 current
	 * 指针同层。Zoker 自写自清（unlike service.properties 要打包方生成），无部署工具链耦合。
	 */
	static final String RUN_PID_NAME = "run.pid";
	// run.pid 行式内容（key=value，首个'='分隔）的键：pid=进程号，start=startInstant 指纹
	// （ISO-8601 文本，写入时不可得为空串），command=辅证据（命令行，不可得时回退可执行
	// 路径——Windows 的 commandLine 恒 empty、command() 可得；换行清洗）。
	static final String KEY_PID = "pid";
	static final String KEY_START = "start";
	// version=启动时的版本目录名（pruneVersions 在用版本保护的判据）：服务不随 current 切换
	// 重启时仍从旧版本目录运行，prune 不得删它；旧格式身份/写入时不可得为空串。
	static final String KEY_VERSION = "version";

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
	// 同服务 start/stop 互斥（services/<svc> 折叠键，对齐 DistributeManager.commitLocks
	// 的形态与判据）。stopService 首行摘账、此后最长 10s优雅+10s强杀 的停机窗口——窗口内并发
	// start 按 run.pid 领养"正在被终止"的进程并回执 Running：回执即谎言且终局服务死
	// （领养判据只看"pid 存活+指纹相符"，无法区分现役
	// 与正被杀）。互斥使两序皆自洽：stop 先完成→start 见无身份/死残留重新拉起（回执诚实）；
	// start 先完成→stop 正常停它（回执亦诚实）。锁序安全：与 commitLocks 无嵌套（commit 不碰
	// 进程记账，start/stop 不碰版本目录），watchExit 回调不取本锁。键大小写折叠
	// （toLowerCase(Locale.ROOT)）：Windows 上 "svc"/"Svc" 同一物理容器，裸键两把锁互斥失效；
	// Linux 过度串行化可接受（生命周期 RPC 非热路径）。条目数以
	// （折叠后的）服务名为界，与 isSafePathSegment 守卫后的名字面同量级，无攻击面放大。
	private final ConcurrentHashMap<String, Object> opsLocks = new ConcurrentHashMap<>();

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
		// services/ 的每个子目录是一个服务容器（services/<svc>/<versionNo>/... + current），
		// 服务存在性以"services/<svc> 目录存在"为准；运行状态来自本进程的processes记账，
		// 以 isAlive 判定：死条目（onExit回调未及清理的窗口）不报 running。
		// 对账领养过的孤儿在本记账里（adopted 标记可观测），跨 Zoker 重启连续。
		var listFiles = serviceDir.listFiles();
		if (null != listFiles) {
			for (var file : listFiles) {
				if (file.isDirectory()) {
					var service = new BService.Data();
					service.setServiceName(file.getName());
					var process = processes.get(service.getServiceName());
					if (null != process && process.isAlive()) {
						service.setState(STATE_RUNNING);
						service.setPs(psOf(process));
					} else if (null != process) {
						// 条目在但进程死=Stopped；无条目（含指纹不可核实失明的孤儿）保持""
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
	 *
	 * <p><b>行式 key=value 解析（首个'='分隔，值原样保留）——与
	 * {@link RunPidRecord#parse} 同法</b>，不用 {@code Properties.load}：Properties 对值内
	 * 反斜杠做转义还原（{@code C:\srv\app.exe}→{@code C:srvapp.exe}，未识别转义直接丢反斜杠，
	 * Java 规范行为），Windows 路径形态的 command/env/args 静默损坏且 eStartFail 日志显示
	 * 损坏后命令——同文件内 run.pid 的自写解析正是为此弃用 Properties，部署描述对齐同一标准，
	 * 反斜杠无任何转义语义、Windows 路径可直写。顺带的语义收窄（#注释行/续行/unicode转义
	 * 不识别）对本机器生成的小型描述文件零成本。</p>
	 */
	static LaunchSpec parseLaunchSpec(File currentVersionDir) throws IOException {
		var file = new File(currentVersionDir, SERVICE_PROPERTIES_NAME);
		String content;
		try (var input = new FileInputStream(file)) {
			content = new String(input.readAllBytes(), StandardCharsets.UTF_8);
		}
		String command = null;
		String args = null;
		String env = null;
		for (var line : content.split("\n", -1)) {
			var i = line.indexOf('=');
			if (i <= 0)
				continue;
			var key = line.substring(0, i).trim(); // trim 吸收 CRLF 的 \r 与键前后空白
			var value = line.substring(i + 1); // 值原样保留（含反斜杠），消费点自行 trim
			switch (key) {
				case KEY_COMMAND -> command = value;
				case KEY_ARGS -> args = value;
				case KEY_ENV -> env = value;
				default -> {
				}
			}
		}
		if (null == command || command.trim().isEmpty())
			throw new IOException("service.properties missing 'command': " + file);
		var spec = new LaunchSpec(currentVersionDir);
		spec.command.add(command.trim());
		if (null != args && !args.trim().isEmpty())
			spec.command.addAll(Arrays.asList(args.trim().split("\\s+")));
		if (null != env && !env.trim().isEmpty()) {
			for (var pair : env.trim().split(",")) {
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
		// 服务文件在 services/<svc>/<current指向的版本>/ 下。
		// 无现役指针（从未commit/现场被破坏）= 描述文件必然缺失，同映射 eNoServiceProperties。
		var workingDir = DistributeManager.currentVersionDir(new File(serviceDir, serviceName));
		if (null == workingDir)
			throw new FileNotFoundException("service has no current version: " + serviceName);
		return parseLaunchSpec(workingDir);
	}

	/**
	 * 启动描述：command+args 组命令、env 注入进程环境、工作目录=现役版本目录。
	 *
	 * <p><b>输出契约：子进程 stdout/stderr 丢弃（Redirect.DISCARD）</b>。
	 * ProcessBuilder 默认 PIPE 而本记账从不读取流——子进程累计输出越过 OS 管道缓冲（~64KB）
	 * 后 write 阻塞，服务静默冻结而 listService 恒 Running。
	 * DISCARD 零线程零 fd；需要保留输出的部署在 command 自行重定向到文件
	 * （{@code command=cmd} + {@code args=/c app.exe > app.log 2>&1} 形态）。领养形态
	 * （AdoptedProcess）无管道，不受影响。</p>
	 */
	private static Process launch(LaunchSpec spec) throws IOException {
		var pb = new ProcessBuilder();
		pb.directory(spec.workingDir);
		pb.command(spec.command);
		if (!spec.env.isEmpty())
			pb.environment().putAll(spec.env);
		// 无人消费的管道=64KB 后写阻塞冻结（见上输出契约），丢弃即闭合。
		pb.redirectOutput(ProcessBuilder.Redirect.DISCARD);
		pb.redirectError(ProcessBuilder.Redirect.DISCARD);
		return pb.start();
	}

	// ---------- 盘上进程身份（run.pid） ----------

	/** run.pid 内容：pid+startInstant 指纹为主、command 为辅（均为写入时文本）。 */
	static final class RunPidRecord {
		final long pid;
		/** startInstant 的 ISO-8601 文本；写入时不可得为空串（此后对它恒失明，不裸 pid 领养）。 */
		final String start;
		final String command;
		final String version;

		RunPidRecord(long pid, String start, String command) {
			this(pid, start, command, "");
		}

		RunPidRecord(long pid, String start, String command, String version) {
			this.pid = pid;
			this.start = start;
			this.command = command;
			this.version = version;
		}

		/** 辅证据取值：命令行优先，不可得（Windows 恒 empty）回退可执行路径。 */
		static String auxOf(ProcessHandle.Info info) {
			var cl = sanitize(info.commandLine().orElse(""));
			if (!cl.isEmpty())
				return cl;
			return sanitize(info.command().orElse(""));
		}

		/** 行式 key=value（首个'='分隔，值原样保留）解析；pid 缺失/非法=null（损坏）。 */
		static @Nullable RunPidRecord parse(String content) {
			long pid = -1;
			var start = "";
			var command = "";
			var version = "";
			for (var line : content.split("\n", -1)) {
				var i = line.indexOf('=');
				if (i <= 0)
					continue;
				var key = line.substring(0, i);
				var value = line.substring(i + 1);
				switch (key) {
					case KEY_PID -> {
						try {
							pid = Long.parseLong(value.trim());
						} catch (NumberFormatException e) {
							return null;
						}
					}
					case KEY_START -> start = value;
					case KEY_COMMAND -> command = value;
					case KEY_VERSION -> version = value;
					default -> {
					}
				}
			}
			return pid > 0 ? new RunPidRecord(pid, start, command, version) : null;
		}
	}

	/**
	 * 领养句柄的记账适配器：把身份核实通过的 ProcessHandle 适配回记账值
	 * 类型 Process——两种形态（spawned 的子进程/领养的裸句柄）同入一张账，watchExit 的
	 * 条件移除 {@code remove(key,process)} 对适配器（同一实例）同样成立。
	 * 进程面全部委托句柄；子进程面退化：退出码不可得（ProcessHandle 无 exitValue，恒 ITSE，
	 * 既有 exitCode 辅助函数按 -1 收殓）、流管道不存在。Ps/等待语义见 deadPs/onExit。
	 */
	static final class AdoptedProcess extends Process {
		private final ProcessHandle handle;

		AdoptedProcess(ProcessHandle handle) {
			this.handle = handle;
		}

		@Override
		public long pid() {
			return handle.pid();
		}

		@Override
		public boolean isAlive() {
			return handle.isAlive();
		}

		@Override
		public ProcessHandle toHandle() {
			return handle;
		}

		@Override
		public ProcessHandle.Info info() {
			return handle.info();
		}

		@Override
		public CompletableFuture<Process> onExit() {
			return handle.onExit().thenApply(h -> this);
		}

		@Override
		public void destroy() {
			handle.destroy();
		}

		@Override
		public Process destroyForcibly() {
			handle.destroyForcibly();
			return this;
		}

		@Override
		public boolean supportsNormalTermination() {
			return handle.supportsNormalTermination();
		}

		@Override
		public int waitFor() throws InterruptedException {
			try {
				handle.onExit().get();
			} catch (ExecutionException e) {
				logger.error("adopted waitFor onExit error: pid={}", handle.pid(), e);
			}
			// 领养进程退出码不可得：以 ITSE 表达（与"活着调 exitValue"同一异常，调用方按不可得收殓）
			throw new IllegalThreadStateException("adopted process has no exit value");
		}

		@Override
		public boolean waitFor(long timeout, TimeUnit unit) throws InterruptedException {
			try {
				handle.onExit().get(timeout, unit);
				return true;
			} catch (TimeoutException e) {
				return false;
			} catch (ExecutionException e) {
				logger.error("adopted waitFor onExit error: pid={}", handle.pid(), e);
				return true;
			}
		}

		@Override
		public int exitValue() {
			throw new IllegalThreadStateException("adopted process has no exit value");
		}

		@Override
		public OutputStream getOutputStream() {
			throw new UnsupportedOperationException("adopted process has no pipe");
		}

		@Override
		public InputStream getInputStream() {
			throw new UnsupportedOperationException("adopted process has no pipe");
		}

		@Override
		public InputStream getErrorStream() {
			throw new UnsupportedOperationException("adopted process has no pipe");
		}
	}

	private @Nullable RunPidRecord readRunPid(String serviceName) {
		var file = new File(new File(serviceDir, serviceName), RUN_PID_NAME);
		String content;
		try {
			content = Files.readString(file.toPath(), StandardCharsets.UTF_8);
		} catch (IOException ex) {
			return null; // 不存在（常态）或不可读
		}
		var rec = RunPidRecord.parse(content);
		if (null == rec)
			logger.warn("run.pid corrupt: {}", file);
		return rec;
	}

	/**
	 * run.pid 身份解析——启动对账/start 查重/stop 查重三路共用的同一解析：
	 * 返回"pid 存活且指纹核实通过"的领养句柄。
	 * <ul>
	 * <li>不存在：无身份（常态），返回 null；损坏：清理残留（对账收敛一切残局）。</li>
	 * <li>pid 死：清理残留返回 null。</li>
	 * <li>startInstant 不符（同 pid 已是另一个进程实例=PID 复用）：清理残留返回 null
	 * ——绝不领养、绝不误杀。startInstant 是判别门：进程创建时间在 exec 链下不变，
	 * 同 pid+同 startInstant 即同一进程实例（残余风险：碰撞概率=pid 池复用
	 * 落进同一时间片，接受为残余）。</li>
	 * <li>指纹不可核实（盘上 start 空=写入时就不可得，或现场 startInstant 读不到）：
	 * 告警+视为无条目（失明），文件保留（证据留给人工，与死/损坏/不符的清理面区分）。</li>
	 * <li>command（辅证据）不符：告警但不否决领养——Linux exec 链（wrapper 脚本）对自家
	 * 进程 command 必假阴性，否决它会复活双启主缺陷；身份已由 startInstant 核实。</li>
	 * </ul>
	 */
	private @Nullable ProcessHandle resolveRunPid(String serviceName) {
		var file = new File(new File(serviceDir, serviceName), RUN_PID_NAME);
		var rec = readRunPid(serviceName);
		if (null == rec) {
			if (file.isFile())
				deleteResidue(file, "corrupt");
			return null;
		}
		Optional<ProcessHandle> live = ProcessHandle.of(rec.pid);
		if (live.isEmpty() || !live.get().isAlive()) {
			deleteResidue(file, "dead pid=" + rec.pid);
			return null;
		}
		var info = live.get().info();
		var liveStart = info.startInstant().map(Object::toString).orElse("");
		if (rec.start.isEmpty() || liveStart.isEmpty()) {
			// 指纹不可核实：失明不领养（宁可失明，绝不按裸 pid 领养——pid 池复用下裸 pid
			// 命中的可能是无关进程，误杀比失明危险）；文件保留。
			logger.warn("run.pid fingerprint unavailable, blind (not adopted): {} recordedStartPresent={} liveStartPresent={}",
					file, !rec.start.isEmpty(), !liveStart.isEmpty());
			return null;
		}
		if (!rec.start.equals(liveStart)) {
			// startInstant 不符=同 pid 已是另一个进程实例（PID 复用）：陈旧身份，清理。
			deleteResidue(file, "startInstant mismatch pid=" + rec.pid);
			return null;
		}
		var liveCommand = RunPidRecord.auxOf(info);
		if (!rec.command.isEmpty() && !liveCommand.isEmpty() && !rec.command.equals(liveCommand))
			logger.warn("run.pid command differs (adopt anyway, identity verified by startInstant): {} pid={}",
					file, rec.pid);
		return live.get();
	}

	private static void deleteResidue(File file, String why) {
		if (file.delete())
			logger.info("run.pid residue cleaned ({}): {}", why, file);
		else if (file.isFile())
			logger.warn("run.pid residue clean fail ({}): {}", why, file);
	}

	/**
	 * 运行中服务的在用版本目录名（pruneVersions 清理面排除用，只读无副作用——
	 * 不走 resolveRunPid 的残留清理）。null=服务未运行（无身份/pid 已死，不受保护）；
	 * 空串=在运行但版本不可知（旧格式身份），调用方须把该服务整体移出清理面；
	 * 非空=run.pid version 行。只查 pid 存活不核指纹：误报方向是过度保护（多保一个
	 * 目录，有界），不做误删方向的事。
	 */
	@Nullable String runningVersion(String serviceName) {
		var rec = readRunPid(serviceName);
		if (null == rec)
			return null;
		return ProcessHandle.of(rec.pid).filter(ProcessHandle::isAlive).isPresent() ? rec.version : null;
	}

	/** 行式文件容错：命令行内嵌换行会破坏 key=value 行结构；写/核两侧同变换，等价比较不受影响。 */
	private static String sanitize(String commandLine) {
		return commandLine.replace('\n', ' ').replace('\r', ' ');
	}

	/**
	 * 盘是真相源：启动交付前把进程身份原子落盘（与 current 指针同原语 AtomicFileWriter——
	 * 任何崩溃点留下的都是完整的旧身份或新身份，无截断中间态）。
	 * 供直构测试按记录形态摆盘（同一落盘路径，格式契约单点）。
	 */
	static void writeRunPid(File container, RunPidRecord record) throws IOException {
		var text = KEY_PID + "=" + record.pid + "\n"
				+ KEY_START + "=" + record.start + "\n"
				+ KEY_COMMAND + "=" + record.command + "\n"
				+ KEY_VERSION + "=" + record.version + "\n";
		AtomicFileWriter.replace(new File(container, RUN_PID_NAME).toPath(), text.getBytes(StandardCharsets.UTF_8));
	}

	/** 从活句柄取现值写盘（startService 拉起路径）：version=launch 时的工作目录名。 */
	private static void writeRunPid(File container, Process process, String version) throws IOException {
		var info = process.info();
		writeRunPid(container, new RunPidRecord(process.pid(),
				info.startInstant().map(Object::toString).orElse(""),
				RunPidRecord.auxOf(info), version));
	}

	/**
	 * 停毕/onExit 的条件删除：run.pid 内容 pid 仍是本句柄的才删
	 * ——新 start（或他方）已改写身份的文件必须留下，迟到的收殓不误删别人的真相源。
	 * 读-判-删非原子：与新 start 的"写新身份"窗口理论上可交错（读得旧 pid 后对方刚写完即被
	 * 误删），但该窗口需在微秒级读删间隙内塞进一次完整进程拉起，且后果只是下次重启失明
	 * （非误杀），接受残余。
	 */
	private void deleteRunPidIfOwn(String serviceName, Process process) {
		var rec = readRunPid(serviceName);
		if (null == rec || rec.pid != process.pid())
			return;
		var file = new File(new File(serviceDir, serviceName), RUN_PID_NAME);
		if (!file.delete())
			logger.warn("run.pid own-delete fail: {}", file);
	}

	/**
	 * 启动对账（领养）：Zoker.start() 在 listen 之前扫描各服务容器的 run.pid，
	 * 身份核实通过（pid 存活+指纹相符）的孤儿装账并挂退出监控——从此刻起 list 可见、stop 可停、
	 * start 幂等复用，三个生命周期 RPC 的语义跨 Zoker 重启连续。对账点=启动扫描（单一入口，
	 * list 零额外盘 IO，领养即挂 onExit 使外部死亡自愈）；start/stop 路径的按需解析
	 * （resolveRunPid）兜底覆盖崩溃窗口残留。只领养核实通过的——杜绝按裸 pid 领养带来的
	 * PID 复用误杀。
	 */
	public void adoptOrphans() {
		var listFiles = serviceDir.listFiles();
		if (null == listFiles)
			return;
		for (var container : listFiles) {
			if (!container.isDirectory())
				continue;
			var serviceName = container.getName();
			var handle = resolveRunPid(serviceName);
			if (null == handle)
				continue;
			var adopted = new AdoptedProcess(handle);
			if (processes.putIfAbsent(serviceName, adopted) == null) {
				// 装账后再挂退出监控：先挂监控可能赶在装账前触发回调，remove(key,process)错失清理
				watchExit(serviceName, adopted);
				logger.info("adoptOrphans: adopted {} pid={}", serviceName, handle.pid());
			}
		}
	}

	// ---------- 进程记账与生命周期 ----------

	/**
	 * 进程退出监控：启动/领养装账后注册，进程退出时清理processes条目并记录退出码，
	 * 死进程不占用"running"语义。
	 * {@code remove(key, process)} 条件移除：startService 替换重启死句柄后，
	 * 旧句柄迟到的退出回调不会误删新条目（AdoptedProcess 为实例等价，同构成立）。
	 * 回调内按内容比对条件删除 run.pid（pid 仍是自己的才删）——外部死亡/
	 * 自然退出后盘上身份同步收敛，残留不留给下次对账。
	 */
	private void watchExit(String serviceName, Process process) {
		process.onExit().whenComplete((exited, ex) -> {
			if (ex != null) {
				logger.error("service onExit error: {}", serviceName, ex);
				return;
			}
			if (processes.remove(serviceName, process))
				logger.info("service exited: {} pid={} exitCode={}", serviceName, process.pid(), exitCode(process));
			deleteRunPidIfOwn(serviceName, process);
		});
	}

	private static int exitCode(Process process) {
		try {
			return process.exitValue();
		} catch (IllegalThreadStateException ex) {
			return -1; // 活进程竞态窗口与 AdoptedProcess（退出码不可得）同收殓路径
		}
	}

	/** Ps 表达：领养句柄（AdoptedProcess）显式标记 adopted+pid，跨重启形态可观测。 */
	private static String psOf(Process process) {
		var info = process.info().toString();
		return process instanceof AdoptedProcess adopted
				? "adopted pid=" + adopted.pid() + " " + info : info;
	}

	/** 死进程的 Ps：spawned 带退出码；领养句柄无 exitValue（API 限制），以 pid 表达。 */
	private static String deadPs(Process process) {
		return process instanceof AdoptedProcess adopted
				? "pid=" + adopted.pid() + ",exit=n/a(adopted)" : formatPs(exitCode(process));
	}

	/**
	 * 启动服务。成功填 Result（State=Running）返回 0；失败返回协议错误码
	 * （eNoServiceProperties/eStartFail），不发结果包。
	 * 条目缺失/死时先按 run.pid 身份解析查重（与 stop/启动对账共用同一解析）
	 * ——存活且核实=上一代 Zoker 的现役进程，领养并幂等返回 Running（Ps 标记 adopted+pid），
	 * 绝不盲目双启；拉起成功后写盘身份，写盘失败=不交付（盘是真相源，无盘身份的进程不允许存在）。
	 */
	public long startService(StartService r) {
		var serviceName = r.Argument.getServiceName();
		// serviceName 直接拼入 services/<svc> 容器路径（loadLaunchSpec→currentVersionDir），
		// 非单段名（".."逃逸/分隔符/绝对盘符）可把解析范围指到 services/ 之外——与 open 写入的
		// distributes 内容组合即成完整 RCE 链（Zoker 端口无认证，任意 TCP 可发协议帧）。
		// 对齐 commitService 的既有同构守卫（DistributeManager.isSafePathSegment）直接拒绝，
		// 不新增协议错误码：非法名≈永远不存在可用描述文件。
		if (!DistributeManager.isSafePathSegment(serviceName)) {
			logger.error("startService rejected: unsafe serviceName='{}'", serviceName);
			return err(Zoker.eNoServiceProperties);
		}
		// 同服务 start/stop 全程持 opsLocks（见字段注释）——领养查重与 stop 的
		// 摘账-停机窗口不得交错，否则"start 领养正被杀的进程并回执 Running"。
		synchronized (opsLocks.computeIfAbsent(serviceName.toLowerCase(Locale.ROOT), __ -> new Object())) {
			return startServiceLocked(r, serviceName);
		}
	}

	private long startServiceLocked(StartService r, String serviceName) {
		Process process;
		while (true) {
			var existing = processes.get(serviceName);
			if (null != existing && existing.isAlive()) {
				// 已在运行：复用现役句柄（幂等），不重复拉起
				process = existing;
				break;
			}
			// 条目缺失或死句柄（进程自然退出/被外部杀死，onExit回调未及清理的窗口）：
			// 先按 run.pid 查重——上一代 Zoker（或崩溃窗口残留）的存活且
			// 核实通过的进程领养复用；死/损坏/不符的残留在解析内一并清理。
			var orphan = resolveRunPid(serviceName);
			if (null != orphan) {
				var candidate = new AdoptedProcess(orphan);
				var installed = existing == null
						? processes.putIfAbsent(serviceName, candidate) == null
						: processes.replace(serviceName, existing, candidate);
				if (installed) {
					// 装账后再挂退出监控：先挂监控可能赶在装账前触发回调，remove(key,process)错失清理
					watchExit(serviceName, candidate);
					logger.info("startService {} adopted orphan pid={}", serviceName, candidate.pid());
					process = candidate;
					break;
				}
				// 并发start竞争落败：重试（孤儿不销毁——它不是本路径拉起的候选，归属胜者）
				continue;
			}
			// 无可领养身份：（重新）拉起
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
				// 盘是真相源——写盘失败=不交付：回滚装账+杀候选+eStartFail。
				// 写盘在挂监控前：失败路径无回调需要拆；成功后候选若恰好已死，onExit 即时收殓。
				try {
					writeRunPid(new File(serviceDir, serviceName), candidate, spec.workingDir.getName());
				} catch (IOException ex) {
					logger.error("startService {} write run.pid fail, discard candidate pid={}",
							serviceName, candidate.pid(), ex);
					processes.remove(serviceName, candidate);
					candidate.destroyForcibly();
					return err(Zoker.eStartFail);
				}
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
		r.Result.setPs(psOf(process));
		return 0;
	}

	/**
	 * 停止服务，结局三态写进 Result（零协议形状变更）：
	 * Stopped（优雅退出/停时本已退出，Ps=exit=N）/ Force-Killed（超时强杀成功，Ps=exit=N）/
	 * Alive-After-Force（强杀限期后仍存活的极端残留，error日志，Ps=进程info）。
	 * 未运行（从未启动/已停止/已被onExit清理）幂等成功：State=Stopped，Ps=not-running。
	 * 条目缺失≠not-running——先按 run.pid 身份解析（与 start 共用）：
	 * 存活且核实的孤儿领养句柄直接进入停机路径（"stop 能真停"），死/缺失/不可核实才幂等
	 * not-running。停毕按内容比对条件删除 run.pid；Alive-After-Force 不删（进程仍活着，
	 * 身份仍真——误删=下次对账失明）。领养句柄的退出码不可得，Ps 以 pid 表达。
	 */
	public void stopService(StopService r) throws InterruptedException {
		var serviceName = r.Argument.getServiceName();
		// serviceName 直接拼入 services/<svc> 容器路径（readRunPid/resolveRunPid 的 run.pid
		// 读/删原语与领养后的停机原语），非单段名（".."逃逸/分隔符/绝对盘符）可把原语指到
		// services/ 之外——对齐 startService/commitService 的同构守卫直接拒绝。
		// 非法名不可能是已管理服务，按幂等 not-running 回执（stop 无专用名错误码）。
		if (!DistributeManager.isSafePathSegment(serviceName)) {
			logger.error("stopService rejected: unsafe serviceName='{}'", serviceName);
			r.Result.setServiceName(serviceName);
			r.Result.setState(STATE_STOPPED);
			r.Result.setPs("not-running");
			return;
		}
		// 同服务 start/stop 全程持 opsLocks（见字段注释）——摘账后的停机窗口内
		// 并发 start 不得进入（否则领养"正在被终止"的进程，回执 Running 即谎言）。
		synchronized (opsLocks.computeIfAbsent(serviceName.toLowerCase(Locale.ROOT), __ -> new Object())) {
			stopServiceLocked(r, serviceName);
		}
	}

	private void stopServiceLocked(StopService r, String serviceName) throws InterruptedException {
		var process = processes.remove(serviceName);
		if (null == process) {
			// 条目缺失先解析盘上身份再判 not-running（三态结局对领养句柄同样成立）。
			// 不装账直接停：stop 语义本就是"移除并终止"，装回账里反而制造停机窗口的假 Running。
			var orphan = resolveRunPid(serviceName);
			if (null == orphan) {
				r.Result.setServiceName(serviceName);
				r.Result.setState(STATE_STOPPED);
				r.Result.setPs("not-running");
				return;
			}
			process = new AdoptedProcess(orphan);
			logger.info("stopService {} adopting orphan pid={} to stop", serviceName, process.pid());
		}
		r.Result.setServiceName(serviceName);
		if (!process.isAlive()) {
			// 死条目（自然退出/外部杀死后onExit未及清理）：结局已定，汇报自然退出码
			r.Result.setState(STATE_STOPPED);
			r.Result.setPs(deadPs(process));
			deleteRunPidIfOwn(serviceName, process);
			return;
		}

		if (!r.Argument.isForce() && supportsNormalTermination(process)) {
			process.destroy();
			try {
				if (process.waitFor(STOP_GRACEFUL_SECONDS, TimeUnit.SECONDS)) {
					r.Result.setState(STATE_STOPPED);
					r.Result.setPs(deadPs(process));
					deleteRunPidIfOwn(serviceName, process);
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
				r.Result.setPs(deadPs(process));
				deleteRunPidIfOwn(serviceName, process);
				return;
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
		// 强杀限期后仍存活（不可杀子进程残留等极端情形）：句柄交GC收殓，不阻塞派发线程
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

	// 直构测试 seam：注入/读取进程记账（模拟 onExit 回调未及清理的死句柄窗口）。
	// 领养条目是 AdoptedProcess（instanceof 即领养形态），亦经同一 seam 可观测。
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
