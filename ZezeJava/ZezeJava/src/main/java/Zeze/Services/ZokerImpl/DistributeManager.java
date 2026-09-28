package Zeze.Services.ZokerImpl;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import Zeze.Builtin.Zoker.CommitService;
import Zeze.IModule;
import Zeze.Net.AsyncSocket;
import Zeze.Net.Binary;
import Zeze.Services.Zoker;
import Zeze.Util.AtomicFileWriter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

/**
 * 管理文件。
 *
 * <p>服务目录布局（版本目录+current 指针）：</p>
 * <pre>
 * services/&lt;svc&gt;/&lt;versionNo&gt;/...   版本目录（服务文件整包）
 * services/&lt;svc&gt;/current            小文件指针，内容=现役版本号（UTF-8 文本）
 * </pre>
 * <p>commitService 的两步：distributes/&lt;svc&gt; rename 到 services/&lt;svc&gt;/&lt;versionNo&gt;
 * （纯新增，不动现役），然后原子切换 current（同目录写临时文件+rename，单步原子）。
 * 旧版本目录留作回滚点，由保留策略（{@link #keepVersions}）清理最老的。
 * 失败恢复：装版本失败无副作用（现役未动）；切换失败现役未动（新版本已装好，重试直接进入切换收敛）；
 * 重试幂等：目标版本目录已存在（上次中断的残留）= 跳过安装直接切换，同为成功。</p>
 */
public class DistributeManager {
	private static final Logger logger = LogManager.getLogger(DistributeManager.class);

	/** services/&lt;svc&gt;/ 下的现役版本指针文件名，内容为版本号文本。 */
	static final String CURRENT_NAME = "current";
	/** commit 后保留的版本目录数（含现役），超过的最老版本被清理；&lt;=0 表示全保留。 */
	static final int KEEP_VERSIONS_DEFAULT = 3;

	private final @Nullable Zoker zoker;
	private final File distributeDir;
	private final File serviceDir;
	// 键为distributeDir内实体文件的canonical路径：键与实体位置一致（FileBin.getCanonicalFile()），
	// commitService按服务目录前缀回收、CWD无关。
	private final ConcurrentHashMap<String, FileBin> files = new ConcurrentHashMap<>();
	// 每个agent连接打开的文件键：agent在OpenFile之后、CloseFile之前断链时按连接回收FileBin，
	// 否则RandomAccessFile句柄常驻泄漏，Windows上还锁住distributes下的文件使commit的rename失败。
	private final ConcurrentHashMap<AsyncSocket, Set<String>> filesBySocket = new ConcurrentHashMap<>();
	// 同服务 commit 串行化锁（services/<svc> 粒度）。键为 foldVersionName(serviceName) 折叠
	// （serviceName 已过 isSafePathSegment 校验；折叠可能并键的仅尾点/空格与大小写变体，
	// 过度串行化有界），条目数以（折叠后的）服务名为界，无攻击面放大。跨服务不受影响。
	private final ConcurrentHashMap<String, Object> commitLocks = new ConcurrentHashMap<>();
	// commit 进行中的服务前缀（distributes/<svc> 的 canonical 路径+分隔符）：open 的入账
	// 原子段内检查命中即拒绝——closeUnder 清账与 renameTo 之间新开的 FileBin 会漏出回收面。
	// 条目数=并发 commit 数，随 commit 结束摘除。Set按canonical前缀去重；commitLocks 键
	// 已统一 foldVersionName 折叠，canonical 同一的 svc 名（如 Windows "svc"与"svc."）必得
	// 同一把锁，后继 commit 必在前者 finally 摘 barrier 之后才进入——先结束者提前摘 barrier
	// 的边缘不复存在，无需计数化。
	private final Set<String> committingPrefixes = ConcurrentHashMap.newKeySet();
	private volatile int keepVersions = KEEP_VERSIONS_DEFAULT;

	public DistributeManager(Zoker zoker) {
		this.zoker = zoker;
		this.distributeDir = zoker.getDistributeDir();
		this.serviceDir = zoker.getServiceDir();
	}

	/** 直构测试形态：不依赖 Zoker（网络服务），直接给定两个布局根目录。 */
	DistributeManager(File distributeDir, File serviceDir) {
		this.zoker = null;
		this.distributeDir = distributeDir;
		this.serviceDir = serviceDir;
	}

	public @Nullable Zoker getZoker() {
		return zoker;
	}

	/** 保留策略配置：commit 后按安装时间保留最近 keepVersions 个版本目录（现役永不删除）。 */
	public void setKeepVersions(int keepVersions) {
		this.keepVersions = keepVersions;
	}

	public int getKeepVersions() {
		return keepVersions;
	}

	public FileBin open(String serviceName, String fileName, AsyncSocket sender) throws IOException {
		var path = new File(serviceName, fileName).getPath();
		// serviceName/fileName直接来自网络rpc（OpenFile请求），必须限制在distributeDir之内，
		// 拒绝"../"逃逸和绝对路径，防止越界写/截断任意文件。
		checkInsideDir(distributeDir, path);
		var relativeCanonicalFileName = fileKey(path);
		// commit窗口的无锁预检：命中即拒，省掉锁外构造的全量md5读盘白工（续传GB级残留可达
		// 数十秒）；判据权威仍是锁内原子段的复检——预检通过到建账之间commit开始的情形由锁内检查闭合。
		if (isCommitting(relativeCanonicalFileName))
			throw new IOException("open file rejected, service committing: " + path);
		var fileBin = files.get(relativeCanonicalFileName);
		if (null == fileBin) {
			// FileBin构造对已存在文件全量读盘算md5（断点续传的GB级残留、慢盘可达数十秒），
			// 不得在filesBySocket锁内执行：closeBySocket在selector/tick线程取同一把锁，
			// 锁内IO会把该event loop上全部连接的读写与心跳检查停摆成串行点。锁外构造、
			// 锁内putIfAbsent决胜，并发open同文件时败者关闭丢弃。
			//
			// zoker-01 目录世代锚点（构造之前取得）：commit 的 rename 单元是 distributes/<一级段>
			// 目录（真实流量 fileName="svc/lib/x.jar"，serviceName 恒为空串——一级段从解析后的
			// 相对路径取）。构造期间 commit 完整起止时 barrier 已摘除、isCommitting 复检失效，
			// candidate 的 fd 已落在被 rename 搬进 services/<svc>/<v> 的旧目录里——锁内复检
			// 世代身份（{@link #dirGeneration}）不一致即拒绝，客户端重试走新目录。
			// 锚点必须取在构造之前：构造后再取，"rename 后他方在同路径重建的新目录"会冒充锚点
			// （旧目录身份回不来，比对必假）；先幂等建目录（FileBin 构造器的 mkdirs 本就会建
			// 同一父链，仅提前到锚点之前，保证锚点身份可得）。文件直接位于顶层（一级段即文件本身，
			// commit 的 rename 只搬目录，永远搬不动它）时不设锚点，保持原行为。
			var base = distributeDir.toPath().toAbsolutePath().normalize();
			var target = base.resolve(path).normalize();
			Path serviceRoot = null;
			String genAnchor = null;
			if (target.getNameCount() > base.getNameCount() + 1) {
				serviceRoot = base.resolve(target.subpath(base.getNameCount(), base.getNameCount() + 1));
				Files.createDirectories(serviceRoot);
				genAnchor = dirGeneration(serviceRoot);
			}
			var candidate = new FileBin(relativeCanonicalFileName, distributeDir, path);
			FileBin winner = candidate;
			var rejectReason = "";
			synchronized (filesBySocket) {
				if (isCommitting(relativeCanonicalFileName))
					// commit清账窗口（barrier在closeUnder之前设置）：新开句柄不得跨越rename——
					// Windows阻塞rename必败；Linux rename成功则已提交版本内容继续被上传写改。
					rejectReason = "service committing";
				else if (null != genAnchor && (genAnchor.isEmpty()
						|| !genAnchor.equals(dirGeneration(serviceRoot))))
					// 世代换代=构造期间发生过rename（commit搬走锚点目录）：candidate的fd指向
					// 已提交版本目录内的文件，入表后append将直接写改现役版本内容（断点续传下
					// 客户端与服务端md5同步增长，校验可通过——静默污染）。空锚点（建目录后微秒内
					// 即被搬走、无法锚定）同样拒绝——拒绝方向安全，重试即锚到新目录。
					// stat是微秒级syscall（对照：closeUnder的close含flush IO禁入锁内），锁内这一下
					// 是"复检与建账"的线性化点，不可外移（锁外复检与建账之间的rename窗口无法闭合）。
					rejectReason = "service directory generation changed (commit raced during construction)";
				else {
					// 建表与socket记账必须原子：锁外两步之间断链清账会把新建的FileBin
					// 漏出回收面（句柄泄漏+死socket映射永驻）。closeAndVerify/closeBySocket持同锁清账。
					var existing = files.putIfAbsent(relativeCanonicalFileName, candidate);
					if (null != existing)
						winner = existing;
					else if (null != sender)
						filesBySocket.computeIfAbsent(sender, __ -> ConcurrentHashMap.newKeySet()).add(relativeCanonicalFileName);
				}
			}
			if (!rejectReason.isEmpty()) {
				closeDiscard(candidate);
				throw new IOException("open file rejected, " + rejectReason + ": " + path);
			}
			if (winner == candidate)
				return candidate;
			closeDiscard(candidate);
			accountOpened(sender, relativeCanonicalFileName);
			return winner;
		}
		accountOpened(sender, relativeCanonicalFileName);
		return fileBin;
	}

	/** 已入表实例的追加记账：建立者的putIfAbsent+记账已在同一原子段完成，此处只补本连接。 */
	private void accountOpened(AsyncSocket sender, String relativeCanonicalFileName) {
		if (null == sender)
			return;
		synchronized (filesBySocket) {
			filesBySocket.computeIfAbsent(sender, __ -> ConcurrentHashMap.newKeySet()).add(relativeCanonicalFileName);
		}
	}

	private static void closeDiscard(FileBin fileBin) {
		try {
			fileBin.close();
		} catch (IOException ex) {
			logger.warn("discard unrecorded FileBin fail: {}", fileBin.getCanonicalFile(), ex);
		}
	}

	private boolean isCommitting(String relativeCanonicalFileName) {
		for (var prefix : committingPrefixes)
			if (relativeCanonicalFileName.startsWith(prefix))
				return true;
		return false;
	}

	public void append(String serviceName, String fileName, long offset, Binary data)
			throws IOException, NoSuchAlgorithmException {
		var fileBin = files.get(fileKey(new File(serviceName, fileName).getPath()));
		if (null == fileBin)
			throw new IOException("file not opened: " + serviceName + "/" + fileName); // 与Hot版一致，未Open直接Append会NPE且无上下文
		fileBin.append(offset, data);
	}

	/**
	 * 关闭并校验，三态返回协议错误码：
	 * 0=校验一致（文件保留，等待commit消费）；
	 * eNotOpened=文件不在传输中（未Open/已收尾/断链回收后补发的close），不谎报成功；
	 * eMd5Mismatch=校验失败，close 后删除物理文件（暂存区损坏中间产物无保留价值），
	 * 下次 OpenFile 从 0 续传，状态机闭合——坏起点不再占位把续传打回人工清理。
	 */
	public long closeAndVerify(String serviceName, String fileName, Binary md5, AsyncSocket sender)
			throws IOException {
		var relativeCanonicalFileName = fileKey(new File(serviceName, fileName).getPath());
		FileBin fileBin;
		// 清账与并发open的记账原子：isEmpty判定remove与重开窗口互斥。
		synchronized (filesBySocket) {
			fileBin = files.remove(relativeCanonicalFileName);
			if (sender != null) {
				var opened = filesBySocket.get(sender);
				if (opened != null) {
					opened.remove(relativeCanonicalFileName);
					if (opened.isEmpty())
						filesBySocket.remove(sender, opened);
				}
			}
		}
		if (null == fileBin)
			return err(Zoker.eNotOpened);
		fileBin.close();
		var md5Local = fileBin.md5Digest();
		if (Arrays.compare(md5Local, md5.bytesUnsafe()) != 0) {
			// 删除失败仅告警：文件已close无句柄占用，正常不会失败；残留等下次md5失败重试删除
			if (!fileBin.getCanonicalFile().delete())
				logger.error("closeAndVerify md5 mismatch, delete corrupt file fail: {}", fileBin.getCanonicalFile());
			return err(Zoker.eMd5Mismatch);
		}
		return 0;
	}

	/** agent断链（ZokerService.OnSocketClose）时回收该连接打开的全部FileBin。 */
	public void closeBySocket(AsyncSocket socket) {
		ArrayList<String> keys;
		// 摘除记账与并发open原子，close在锁外（FileBin.close含md5读）。
		synchronized (filesBySocket) {
			var opened = filesBySocket.remove(socket);
			if (opened == null)
				return;
			keys = new ArrayList<>(opened);
		}
		for (var key : keys) {
			var fileBin = files.remove(key);
			if (fileBin != null) {
				try {
					fileBin.close();
				} catch (IOException ex) {
					logger.error("closeBySocket {}", key, ex);
				}
			}
		}
	}

	/** 关闭并清除全部打开的FileBin（Zoker.stop收尾）。 */
	public void closeAll() {
		for (var e : files.entrySet()) {
			if (files.remove(e.getKey(), e.getValue())) {
				try {
					e.getValue().close();
				} catch (IOException ex) {
					logger.error("closeAll {}", e.getKey(), ex);
				}
			}
		}
	}

	private String fileKey(String path) throws IOException {
		return new File(distributeDir, path).getCanonicalFile().toString();
	}

	/**
	 * 目录世代身份（zoker-01）：commit 把 distributes/&lt;svc&gt; rename 走后他方可在同路径
	 * 重建新目录，"路径相同"不再表示"同一目录"——世代身份取目录物理实体的可辨识属性，
	 * 锚点（open 构造前）与锁内复检两次取值相同=期间未发生过 rename。取值链（实测
	 * Windows11/JDK26：basic:fileKey 为 null、creationTime 可靠且 rename 保留原值、
	 * 子文件增删不变；Linux：fileKey=dev+ino 非空可靠，被 rename 的目录 inode 仍存活于
	 * services/ 下、不会复用给同路径的新建目录）：
	 * <ol>
	 * <li>"id:"+fileKey——POSIX/macOS 的 inode+dev，首选；Windows 上 JDK 不提供（null）。</li>
	 * <li>"ct:"+creationTime——NTFS 创建时间（目录一生不变；同路径重建目录得到新值）。</li>
	 * <li>"?"——两者皆不可得（fileKey null 且 creationTime 为纪元值）的退化平台：比对
	 * 退化为恒等（无世代防护）。Linux/Windows 主流平台均不落入，属残留风险。</li>
	 * <li>""——目录不存在：rename 已把它搬走的最直接证据（调用方视作不可锚定/换代）。</li>
	 * </ol>
	 * lastModifiedTime 不可用作身份：子文件增删即变（并发上传常态），会把合法世代误判为换代。
	 */
	private static String dirGeneration(Path dir) {
		BasicFileAttributes attrs;
		try {
			attrs = Files.readAttributes(dir, BasicFileAttributes.class);
		} catch (IOException e) {
			return ""; // 不存在（或不可stat）——目录已被搬走
		}
		var fileKey = attrs.fileKey();
		if (null != fileKey)
			return "id:" + fileKey;
		var creationTime = attrs.creationTime();
		if (creationTime.toMillis() > 0)
			return "ct:" + creationTime;
		return "?";
	}

	/**
	 * 校验合成相对路径（serviceName/fileName）规范化（normalize）后仍位于 distributeDir 之内。
	 * 越界（含"../"逃逸与绝对路径）时抛出 IOException 拒绝。
	 */
	static void checkInsideDir(File distributeDir, String path) throws IOException {
		var base = distributeDir.toPath().toAbsolutePath().normalize();
		var target = base.resolve(path).normalize();
		if (!target.startsWith(base)) {
			logger.error("open file rejected: path='{}' escape distributeDir='{}'", path, distributeDir);
			throw new IOException("open file rejected, escape distributeDir: " + path);
		}
	}

	/**
	 * 用作目录路径段的名字（commitService的serviceName/versionNo）必须是单段：
	 * 拒绝空、"."、".."、路径分隔符与驱动器冒号，防止把renameTo指到目标目录之外。
	 */
	static boolean isSafePathSegment(String name) {
		return name != null && !name.isEmpty() && !name.equals(".") && !name.equals("..")
				&& name.indexOf('/') < 0 && name.indexOf('\\') < 0 && name.indexOf(':') < 0;
	}

	/**
	 * versionNo 与容器根保留字（current 指针、run.pid 身份文件）的碰撞判别
	 * ——先剥尾部点/空格，再忽略大小写比较。Windows(Win32) 路径解析大小写不敏感且规范化剥
	 * 尾部点/空格："Current"/"CURRENT" 与 current 是同一物理名字（exists 跨大小写命中），
	 * "current." 的 renameTo 落盘名就是 current——前者首次部署可把版本目录 rename
	 * 进指针固有位置（此后该服务一切 commit 恒 AccessDenied，容器报废），指针已存在时则命中
	 * 指针文件跳过安装、switchCurrent 覆盖指针造成"返回 0 但 currentVersionDir 恒 null"的假
	 * 成功；尾部点形态同链路（落盘名脱点后占位）。Linux（大小写敏感 FS）上 "Current"
	 * 本是合法版本名，一并排除零成本且跨平台同一 versionNo 得到同一裁决——两端都闭合。
	 * run.pid 同族扩入——它是容器根的进程身份文件（与 current 同层同碰撞面），
	 * 版本目录 rename 占据该位置后 startService 的身份落盘（AtomicFileWriter 对目录目标
	 * rename）恒失败 → 按"写盘失败=不交付"一切 start 恒 eStartFail（服务永不可启动，无自愈）。
	 * 仅用于 versionNo：serviceName 与保留字无碰撞面（services/Current、services/run.pid
	 * 都是合法服务容器名——保留字在容器<b>之内</b>，容器名本身单段即安全），不得套用。
	 * 折叠判据抽为 {@link #foldVersionName} 单点，与 pruneVersions 的
	 * 现役保护、commitLocked 的指针规范化共用同一语义。
	 */
	static boolean isReservedVersionName(String name) {
		var folded = foldVersionName(name);
		return folded.equals(foldVersionName(CURRENT_NAME))
				|| folded.equals(foldVersionName(ServiceManager.RUN_PID_NAME));
	}

	/**
	 * 版本名的盘上解析折叠（单点判据）：剥尾部点/空格 + 忽略大小写
	 * （toLowerCase(Locale.ROOT)）。Windows(Win32) 路径解析大小写不敏感且规范化剥尾部点/空格
	 * （跨大小写 exists 命中、renameTo 落盘名脱尾点占位），
	 * 即"请求文本"与"盘上实际目录名"可能是同一物理实体的两个拼写。所有需要"请求名与盘上名
	 * 判同"的位置（保留字碰撞 {@link #isReservedVersionName}、现役保护 pruneVersions、
	 * 指针规范化 commitLocked、commitLocks 键）必须统一用本折叠，不得裸 equals/裸
	 * toLowerCase——分叉即互斥面击穿或现役目录落入清理面。
	 * Linux（大小写敏感 FS）上折叠会把 "V1"/"v1" 判同——过度保护（多保一个目录）与
	 * commitLocks 键的过度串行化同款裁量：版本清理非正确性路径、commit 非热路径，可接受。
	 */
	static String foldVersionName(String name) {
		var end = name.length();
		while (end > 0) {
			var c = name.charAt(end - 1);
			if (c != '.' && c != ' ')
				break;
			end--;
		}
		return name.substring(0, end).toLowerCase(Locale.ROOT);
	}

	// 错误码与 zoker.errorCode 同构（ModuleId*编码）；直构形态（zoker==null）下也要能返回协议错误码。
	private static long err(int code) {
		return IModule.errorCode(Zoker.ModuleId, code);
	}

	public long commitService(CommitService r) {
		var rc = commit(r.Argument.getServiceName(), r.Argument.getVersionNo());
		if (rc != 0)
			return rc;
		r.SendResult();
		return 0;
	}

	/**
	 * 版本目录+current 指针的提交流程（包内可见供直构测试）。
	 * 两步各自失败/重试的收敛性见类注释。eServiceOldExists/eMoveOldFail 是旧
	 * servicesOld 布局的错误码，检查路径已随布局移除，不会返回（死码留待 Gen 批清理协议定义）。
	 */
	long commit(String serviceName, String versionNo) {
		// versionNo 排除保留字 current——services/<svc>/current
		// 是现役指针文件的固有位置，版本目录 rename 占据该位置后（首次部署=指针尚不存在的
		// 常态即可 rename 成功），switchCurrent 的原子 rename 对目录目标必失败：此后对该服务的
		// 一切 commit 恒失败、currentVersionDir 恒 null、pruneVersions 永远执行不到（不自愈），
		// 需人工删目录。精确 equals 只挡逐字节的 "current"，Windows 大小写
		// 不敏感 FS 上 "Current"/"CURRENT"/"current." 变体仍会碰撞（见 isReservedVersionName）。
		if (!isSafePathSegment(serviceName) || !isSafePathSegment(versionNo)
				|| isReservedVersionName(versionNo)) {
			logger.error("commitService rejected: unsafe serviceName='{}' versionNo='{}'", serviceName, versionNo);
			return err(Zoker.eCommitFail);
		}
		// 同服务 commit 串行化（services/<svc> 粒度）。
		// commit 三步（install→switch→prune）间无自洽性，CommitService 为 Normal 派发可并发：
		// keepVersions=1 时 A 的 prune 可删除并发 B 已 install 未 switch 的版本目录，B 随后
		// switch 使 current 指向已删除目录（返回 0 但服务永远无法启动，无自愈路径）。
		// 替代方案"prune 按 beginMillis 只删本次开始前安装的版本"留有交错洞：B 先于 A
		// 进入、install 晚于 A 的 install 时，B 的 mtime 早于 A 的 cutoff，仍会被 A 删——
		// 时间戳过滤只能缩窄窗口，互斥才能闭合。锁内为纯本地 FS 操作（rename/fsync/delete）
		// 加 closeUnder 取 filesBySocket（清账与 open 建账互斥）：锁序 commitLocks→filesBySocket
		// 单向嵌套，open/close 路径只取 filesBySocket，无反向持锁，无锁序环。
		// 锁键折叠直接复用 foldVersionName（剥尾点/空格+小写）——裸 serviceName 或仅小写折叠时，
		// Windows(Win32) 路径规范化（大小写不敏感+剥尾点/空格）下 "svc"/"Svc"/"svc." 指向同一
		// 物理容器却各持一把锁，互斥失效，上述竞态经变体名复活。Linux（大小写敏感 FS）上折叠会
		// 过度串行化两个真不同的服务（含尾点/空格变体）：commit 非热路径，可接受；canonical
		// 路径作键在目录尚不存在（首次 commit，恰是竞态高危形态）时不折叠，弃用。条目数仍以
		// （折叠后的）服务名为界，无攻击面放大。
		synchronized (commitLocks.computeIfAbsent(foldVersionName(serviceName), __ -> new Object())) {
			// barrier 须在 closeUnder 之前设置：此后 open 对该前缀的入账在原子段内被拒，
			// 更早完成入账的必被 closeUnder 收殓（sweep 取同一把锁且 CHM 迭代可见先完成的
			// put）——杜绝 sweep 与 renameTo 之间新开 FileBin 漏出回收面。
			// barrier 只覆盖"commit 进行中"的交错；"commit 完整起止于 open 的 FileBin 构造窗内"
			// （barrier 已摘、isCommitting 复检不命中）由 open 锁内的目录世代复检拒绝
			// （zoker-01，见 open 与 dirGeneration）。
			var serviceFrom = new File(distributeDir, serviceName);
			String prefix;
			try {
				prefix = serviceFrom.getCanonicalPath() + File.separator;
			} catch (IOException ex) {
				logger.error("commitService canonical {}", serviceFrom, ex);
				return err(Zoker.eCommitFail);
			}
			committingPrefixes.add(prefix);
			try {
				return commitLocked(serviceName, versionNo);
			} finally {
				committingPrefixes.remove(prefix);
			}
		}
	}

	private long commitLocked(String serviceName, String versionNo) {
		var serviceFrom = new File(distributeDir, serviceName);
		var svcDir = new File(serviceDir, serviceName);
		var versionTo = new File(svcDir, versionNo);
		// 消费distributes/<svc>前先关闭其下仍打开的FileBin：正常流程CloseFile已收尾，
		// 这里兜底跳过CloseFile的连接，同时释放Windows上阻塞rename的文件句柄。
		closeUnder(serviceFrom);
		if (!versionTo.exists()) {
			// 先验源存在再动目标目录：step1失败（distributes/<svc>缺失——重复提交/超时重试的
			// 典型情形）时连services/<svc>容器目录也不创建，彻底无副作用。
			if (!serviceFrom.isDirectory()) {
				logger.error("commitService no distribute content: {}", serviceFrom);
				return err(Zoker.eCommitFail);
			}
			try {
				// renameTo不创建父目录：services/<svc>/这层缺失时renameTo必false
				Files.createDirectories(svcDir.toPath());
			} catch (IOException ex) {
				logger.error("commitService createDirectories {}", svcDir, ex);
				return err(Zoker.eCommitFail);
			}
			if (!serviceFrom.renameTo(versionTo)) {
				logger.error("commitService install version fail: {} -> {}", serviceFrom, versionTo);
				return err(Zoker.eCommitFail);
			}
			// rename保留源目录的最后修改时间（=上传时间），盖写为安装时间，作为保留策略的排序依据。
			if (!versionTo.setLastModified(System.currentTimeMillis()))
				logger.warn("commitService setLastModified fail: {}", versionTo);
		}
		// 指针与 prune 参数用盘上实际目录名，不用请求原样文本。Win32 解析下
		// "V1"跨大小写命中 v1 跳装、"v1."renameTo 落盘为 v1——原样文本写指针后 currentVersionDir
		// 靠跨规范化解析侥幸能启动，但 pruneVersions 的现役保护面对"盘上真名 vs 请求文本"分叉
		// 时物理现役目录落入清理面被删（keep=3 三版本存量即 wedge：current 悬空、start 恒
		// eNoServiceProperties）。规范化后指针、prune 参数、盘上目录三者同名，分叉源头闭合。
		var installed = onDiskVersionName(svcDir, versionNo);
		try {
			switchCurrent(svcDir, installed);
		} catch (IOException ex) {
			// 现役未动；新版本目录已装好，重试同参数走"目标已存在"分支直接再切，收敛。
			logger.error("commitService switch current fail: services/{} version={}", serviceName, versionNo, ex);
			return err(Zoker.eCommitFail);
		}
		pruneVersions(svcDir, installed);
		return 0;
	}

	/**
	 * 把请求 versionNo 规范化为 services/&lt;svc&gt; 下折叠同名的<b>盘上实际目录名</b>：
	 * 找不到折叠命中的实体时原样返回（首次安装未落盘/目标是文件等场景）。
	 * commit 全程持 commitLocks（折叠键），listFiles 与 install/switch/prune 之间无并发 commit
	 * 交错；跨服务无关。找不到命中却已过 exists 跳装的情形只在非目录实体占位时出现——
	 * 原样返回语义不变（currentVersionDir 的 isDirectory 校验兜底）。
	 */
	private static String onDiskVersionName(File svcDir, String versionNo) {
		var listFiles = svcDir.listFiles();
		if (null != listFiles) {
			var folded = foldVersionName(versionNo);
			String foldedMatch = null;
			for (var f : listFiles) {
				if (!f.isDirectory())
					continue;
				// 精确名优先：Linux 上 "v1"/"V1" 可并存，精确命中保持请求语义不漂移；
				// 折叠命中兜底 Windows 的分叉形态（"V1"/"v1." vs 落盘 "v1"）。
				if (f.getName().equals(versionNo))
					return f.getName();
				if (null == foldedMatch && foldVersionName(f.getName()).equals(folded))
					foldedMatch = f.getName();
			}
			if (null != foldedMatch)
				return foldedMatch;
		}
		return versionNo;
	}

	/**
	 * 原子切换现役指针：内容=版本号，经 AtomicFileWriter（fsync+原子 rename 换版）
	 * 写 current——任何时刻读到的都是完整的旧版本号或新版本号，无截断/半内容中间态。
	 */
	private static void switchCurrent(File svcDir, String versionNo) throws IOException {
		AtomicFileWriter.replace(new File(svcDir, CURRENT_NAME).toPath(), versionNo.getBytes(StandardCharsets.UTF_8));
	}

	/**
	 * 解析 services/&lt;svc&gt;/current 指向的版本目录（startService 等读路径用）。
	 * 指针缺失/内容非法/指向不存在的版本时返回 null（服务从未 commit 或现场被破坏）。
	 *
	 * <p>调用方须保证容器目录在管理范围内（services/&lt;svc&gt;，经 isSafePathSegment 校验的
	 * 单段服务名拼出）——本方法只校验指针<b>内容</b>是单段名，不校验传入的容器目录自身边界
	 * （入口校验在 {@code ServiceManager.startService}，这里不重复设防）。</p>
	 */
	public static @Nullable File currentVersionDir(File serviceContainerDir) {
		var current = new File(serviceContainerDir, CURRENT_NAME);
		if (!current.isFile())
			return null;
		String version;
		try {
			version = Files.readString(current.toPath(), StandardCharsets.UTF_8).trim();
		} catch (IOException ex) {
			logger.error("read current pointer fail: {}", current, ex);
			return null;
		}
		// 指针内容由本机写入，这里仍做单段校验：现场被手工改坏时不把路径解析出容器之外。
		if (!isSafePathSegment(version))
			return null;
		var versionDir = new File(serviceContainerDir, version);
		return versionDir.isDirectory() ? versionDir : null;
	}

	/**
	 * 保留策略：按安装时间（版本目录mtime，commit时盖写）保留最近 keepVersions 个版本目录，
	 * 现役版本永不删除；超出的最老版本整树删除。删除失败仅告警，残留等待下次commit重试
	 * （纯空间回收，不影响正确性）。listFiles的null（目录消失/权限）视为无事可做。
	 *
	 * <p>现役保护按 {@link #foldVersionName} 折叠比对：参数来自 commit 的
	 * 请求 versionNo，而 Win32 解析下请求文本与盘上目录名可能分叉（"V1"vs"v1"、"v1."vs"v1"）
	 * ——裸 equals 使物理现役目录落入清理面被删（current 悬空）。折叠后指针文本与盘上真名
	 * 两个来处都命中保护；commitLocked 的指针规范化已使正常路径同名，此处折叠是独立的第二道
	 * 防（直调/存量分叉指针亦闭合）。Linux 上折叠=过度保护（同名变体目录都被保，有界），
	 * 与锁键折叠同款裁量。</p>
	 *
	 * <p><b>在用版本保护</b>：运行中服务的进程身份（run.pid 的 version 行，launch 时落盘）
	 * 指向的版本目录同样不进清理面——服务不随 current 切换重启时仍从旧版本目录运行，
	 * 误删即拆运行中服务的文件（Linux 惰性加载失败；Windows 句柄锁目录使 deleteTree
	 * 恒 false）。判据读取与 startService 的 writeRunPid 经 ServiceManager.opsLocks 互斥
	 * （见方法体，zoker-02）——launch→writeRunPid 窗口内 run.pid 还是旧身份，无互斥时
	 * 正在启动的版本目录会被当非在用删除。版本不可知（旧格式身份）时整体跳过本服务：
	 * 宁可不回收空间也不删在用目录。直构形态（zoker==null）无进程身份可查，保护不生效。</p>
	 */
	void pruneVersions(File svcDir, String currentVersion) {
		var keep = keepVersions;
		if (keep <= 0)
			return; // 全保留
		// zoker-02：prune 段纳入与 start/stop 同粒度互斥（ServiceManager.opsLocks，键同
		// toLowerCase 折叠）。start 持锁的 launch→writeRunPid 窗口内 run.pid 是旧身份，
		// 本判据读到 null/旧版本就会把正在启动的版本目录当非在用删除（进程炸/NoClassDefFound，
		// 两个 RPC 各自"成功"的静默错账）；共锁后 start 必先在锁内落盘新身份，prune 判得到它。
		// 锁序 commitLocks→opsLocks 单向嵌套（调用方 commit 全程持 commitLocks）：start/stop
		// 不取 commitLocks，无反向持锁路径，无环。锁键用容器目录名（=commit 请求的 serviceName
		// 原样拼写），与 start/stop 的 RPC 名同拼写经同一 toLowerCase 折叠后同键；大小写/尾点
		// 变体名的同型分叉与 opsLocks 既有键折叠面一致（首波 zoker-07 笔记已记的未闭合族）。
		var processManager = null != zoker ? zoker.getProcessManager() : null;
		if (null != processManager)
			processManager.withServiceLock(svcDir.getName(),
					() -> pruneLocked(svcDir, currentVersion, keep, processManager));
		else
			// 直构测试形态：无进程身份可锁/可保护（测试直调本方法验证清理面语义，原行为）。
			pruneLocked(svcDir, currentVersion, keep, null);
	}

	private static void pruneLocked(File svcDir, String currentVersion, int keep, @Nullable ServiceManager processManager) {
		var listFiles = svcDir.listFiles();
		if (null == listFiles)
			return;
		var foldedCurrent = foldVersionName(currentVersion);
		// 在用版本判据取自盘上进程身份（run.pid），与内存记账无耦合：launch 时写、
		// 停毕/onExit 删、Alive-After-Force 保留——盘状态即"进程是否仍从该版本运行"。
		String foldedRunning = null;
		if (null != processManager) {
			var running = processManager.runningVersion(svcDir.getName());
			if (null != running && running.isEmpty()) {
				logger.warn("pruneVersions skip, service running with unknown version: {}", svcDir);
				return;
			}
			if (null != running)
				foldedRunning = foldVersionName(running);
		}
		ArrayList<File> candidates = new ArrayList<>();
		for (var f : listFiles) {
			// 只把版本目录纳入清理面：current指针是文件天然排除；名字碰巧等于现役版本的目录不存在（构造上互斥）。
			if (f.isDirectory() && !foldVersionName(f.getName()).equals(foldedCurrent)
					&& (null == foldedRunning || !foldVersionName(f.getName()).equals(foldedRunning)))
				candidates.add(f);
		}
		// 现役已占1个名额：非现役里保留最新的 keep-1 个，其余删除。
		if (candidates.size() <= keep - 1)
			return;
		candidates.sort(Comparator.comparingLong(File::lastModified).reversed()); // 新→旧
		for (int i = keep - 1; i < candidates.size(); i++) {
			var victim = candidates.get(i);
			if (!deleteTree(victim))
				logger.warn("pruneVersions delete fail, keep on next commit: {}", victim);
		}
	}

	private static boolean deleteTree(File dir) {
		var listFiles = dir.listFiles();
		if (null != listFiles) {
			for (var f : listFiles) {
				if (f.isDirectory()) {
					if (!deleteTree(f))
						return false;
				} else if (!f.delete())
					return false;
			}
		}
		return dir.delete();
	}

	private void closeUnder(File dir) {
		String prefix;
		try {
			prefix = dir.getCanonicalPath() + File.separator;
		} catch (IOException ex) {
			logger.error("closeUnder canonical {}", dir, ex);
			return;
		}
		// 摘账持filesBySocket锁（与open建账互斥，配合commit前缀barrier闭合sweep→rename窗口），
		// close在锁外：FileBin.close含flush IO，不得占全局锁。
		ArrayList<Map.Entry<String, FileBin>> victims = new ArrayList<>();
		synchronized (filesBySocket) {
			for (var e : files.entrySet()) {
				var key = e.getKey();
				if (!key.startsWith(prefix))
					continue;
				if (files.remove(key, e.getValue()))
					victims.add(e);
			}
		}
		for (var e : victims) {
			try {
				e.getValue().close();
			} catch (IOException ex) {
				logger.error("closeUnder {}", e.getKey(), ex);
			}
		}
	}
}
