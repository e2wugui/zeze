package Zeze.Services.ZokerImpl;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Locale;
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
 * <p>服务目录布局（GE-D02 版本目录+current 指针）：</p>
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
	// GE-C04：同服务 commit 串行化锁（services/<svc> 粒度）。键先过 isSafePathSegment 校验，
	// 条目数以服务名为界，无攻击面放大。跨服务不受影响。
	private final ConcurrentHashMap<String, Object> commitLocks = new ConcurrentHashMap<>();
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
		// 建表与socket记账必须原子（增量审R1-04）：锁外两步之间断链清账会把新建的FileBin
		// 漏出回收面（句柄泄漏+死socket映射永驻）。closeAndVerify/closeBySocket持同锁清账。
		// FileBin构造含md5读IO，持锁窗口为部署级QPS可接受。
		FileBin fileBin;
		synchronized (filesBySocket) {
			fileBin = files.computeIfAbsent(relativeCanonicalFileName,
					(key) -> new FileBin(key, distributeDir, path));
			if (sender != null)
				filesBySocket.computeIfAbsent(sender, __ -> ConcurrentHashMap.newKeySet()).add(relativeCanonicalFileName);
		}
		return fileBin;
	}

	public void append(String serviceName, String fileName, long offset, Binary data)
			throws IOException, NoSuchAlgorithmException {
		var fileBin = files.get(fileKey(new File(serviceName, fileName).getPath()));
		if (null == fileBin)
			throw new IOException("file not opened: " + serviceName + "/" + fileName); // 与Hot版一致，未Open直接Append会NPE且无上下文
		fileBin.append(offset, data);
	}

	/**
	 * 关闭并校验，三态返回协议错误码（GE-D04 方案A）：
	 * 0=校验一致（文件保留，等待commit消费）；
	 * eNotOpened=文件不在传输中（未Open/已收尾/断链回收后补发的close），不再谎报成功；
	 * eMd5Mismatch=校验失败，close 后删除物理文件（暂存区损坏中间产物无保留价值），
	 * 下次 OpenFile 从 0 续传，状态机闭合——坏起点不再占位把续传打回人工清理。
	 */
	public long closeAndVerify(String serviceName, String fileName, Binary md5, AsyncSocket sender)
			throws IOException {
		var relativeCanonicalFileName = fileKey(new File(serviceName, fileName).getPath());
		FileBin fileBin;
		// 清账与并发open的记账原子（增量审R1-04同源）：isEmpty判定remove与重开窗口互斥。
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
		// 摘除记账与并发open原子（增量审R1-04），close在锁外（FileBin.close含md5读）。
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
	 * GE-C01（FND21）：versionNo 与容器根保留字（current 指针、run.pid 身份文件）的碰撞判别
	 * ——先剥尾部点/空格，再忽略大小写比较。Windows(Win32) 路径解析大小写不敏感且规范化剥
	 * 尾部点/空格："Current"/"CURRENT" 与 current 是同一物理名字（exists 跨大小写命中，本机
	 * 探针实证），"current." 的 renameTo 落盘名就是 current——前者首次部署可把版本目录 rename
	 * 进指针固有位置（此后该服务一切 commit 恒 AccessDenied，容器报废），指针已存在时则命中
	 * 指针文件跳过安装、switchCurrent 覆盖指针造成"返回 0 但 currentVersionDir 恒 null"的假
	 * 成功；尾部点形态同链路（探针实证落盘名脱点后占位）。Linux（大小写敏感 FS）上 "Current"
	 * 本是合法版本名，一并排除零成本且跨平台同一 versionNo 得到同一裁决——两端都闭合。
	 * GE-D01（FND21）：run.pid 同族扩入——它是容器根的进程身份文件（与 current 同层同碰撞面），
	 * 版本目录 rename 占据该位置后 startService 的身份落盘（AtomicFileWriter 对目录目标
	 * rename）恒失败 → 按"写盘失败=不交付"一切 start 恒 eStartFail（服务永不可启动，无自愈）。
	 * 仅用于 versionNo：serviceName 与保留字无碰撞面（services/Current、services/run.pid
	 * 都是合法服务容器名——保留字在容器<b>之内</b>，容器名本身单段即安全），不得套用。
	 * GE-C01(FND22)：折叠判据抽为 {@link #foldVersionName} 单点，与 pruneVersions 的
	 * 现役保护、commitLocked 的指针规范化共用同一语义。
	 */
	static boolean isReservedVersionName(String name) {
		var folded = foldVersionName(name);
		return folded.equals(foldVersionName(CURRENT_NAME))
				|| folded.equals(foldVersionName(ServiceManager.RUN_PID_NAME));
	}

	/**
	 * 版本名的盘上解析折叠（GE-C01(FND22)，单点判据）：剥尾部点/空格 + 忽略大小写
	 * （toLowerCase(Locale.ROOT)）。Windows(Win32) 路径解析大小写不敏感且规范化剥尾部点/空格
	 * （FND21 GE-C01 修复轮本机探针实证：跨大小写 exists 命中、renameTo 落盘名脱尾点占位），
	 * 即"请求文本"与"盘上实际目录名"可能是同一物理实体的两个拼写。所有需要"请求名与盘上名
	 * 判同"的位置（保留字碰撞 {@link #isReservedVersionName}、现役保护 pruneVersions、
	 * 指针规范化 commitLocked）必须统一用本折叠，不得裸 equals——分叉即现役目录落入清理面。
	 * Linux（大小写敏感 FS）上折叠会把 "V1"/"v1" 判同——过度保护（多保一个目录，有界），
	 * 对齐 commitLocks 键折叠的同款裁量：版本清理非正确性路径，可接受。
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
	 * GE-D02 版本目录+current 指针的提交流程（包内可见供直构测试）。
	 * 两步各自失败/重试的收敛性见类注释。eServiceOldExists/eMoveOldFail 是旧
	 * servicesOld 布局的错误码，随布局移除检查路径后不再返回（死码留待 Gen 批清理协议定义）。
	 */
	long commit(String serviceName, String versionNo) {
		// GE-C02(FND20)/GE-C01(FND21)：versionNo 排除保留字 current——services/<svc>/current
		// 是现役指针文件的固有位置，版本目录 rename 占据该位置后（首次部署=指针尚不存在的
		// 常态即可 rename 成功），switchCurrent 的原子 rename 对目录目标必失败：此后对该服务的
		// 一切 commit 恒失败、currentVersionDir 恒 null、pruneVersions 永远执行不到（不自愈），
		// 需人工删目录。FND21 GE-C01：原精确 equals 只挡逐字节的 "current"，Windows 大小写
		// 不敏感 FS 上 "Current"/"CURRENT"/"current." 变体照旧碰撞（见 isReservedVersionName）。
		if (!isSafePathSegment(serviceName) || !isSafePathSegment(versionNo)
				|| isReservedVersionName(versionNo)) {
			logger.error("commitService rejected: unsafe serviceName='{}' versionNo='{}'", serviceName, versionNo);
			return err(Zoker.eCommitFail);
		}
		// GE-C04：同服务 commit 串行化（对齐 R1-04 的对象锁形态，services/<svc> 粒度）。
		// commit 三步（install→switch→prune）间无自洽性，CommitService 为 Normal 派发可并发：
		// keepVersions=1 时 A 的 prune 可删除并发 B 已 install 未 switch 的版本目录，B 随后
		// switch 使 current 指向已删除目录（返回 0 但服务永远无法启动，无自愈路径）。
		// 案卷的替代方案"prune 按 beginMillis 只删本次开始前安装的版本"留有交错洞：B 先于 A
		// 进入、install 晚于 A 的 install 时，B 的 mtime 早于 A 的 cutoff，仍会被 A 删——
		// 时间戳过滤只能缩窄窗口，互斥才能闭合。锁内为纯本地 FS 操作（rename/fsync/delete），
		// 不持其他锁（closeUnder 迭代 files 不取 filesBySocket 锁），无锁序环。
		// GE-C02（FND21）：锁键大小写折叠（与 GE-C01 同一判据）——裸 serviceName 作键时，
		// Windows(NTFS) 大小写不敏感解析下 "svc"/"Svc" 指向同一物理容器却各持一把锁，互斥失效，
		// 上述竞态经大小写变体复活。Linux（大小写敏感 FS）上折叠会过度串行化两个真不同的服务：
		// commit 非热路径，可接受；canonical 路径作键在目录尚不存在（首次 commit，恰是竞态
		// 高危形态）时不折叠大小写，弃用。条目数仍以（折叠后的）服务名为界，无攻击面放大。
		synchronized (commitLocks.computeIfAbsent(serviceName.toLowerCase(Locale.ROOT), __ -> new Object())) {
			return commitLocked(serviceName, versionNo);
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
				// renameTo不创建父目录：services/<svc>/这层缺失时renameTo必false（对齐R1 GE-C04的修复点）
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
		// GE-C01(FND22)：指针与 prune 参数用盘上实际目录名，不用请求原样文本。Win32 解析下
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
	 * 把请求 versionNo 规范化为 services/&lt;svc&gt; 下折叠同名的<b>盘上实际目录名</b>
	 * （GE-C01(FND22)）：找不到折叠命中的实体时原样返回（首次安装未落盘/目标是文件等场景）。
	 * commit 全程持 commitLocks（折叠键），listFiles 与 install/switch/prune 之间无并发 commit
	 * 交错；跨服务无关。找不到命中却已过 exists 跳装的情形只在非目录实体占位时出现——
	 * 原样返回与旧行为等价（currentVersionDir 的 isDirectory 校验兜底）。
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
	 * 原子切换现役指针：内容=版本号，经 AtomicFileWriter（fsync+原子 rename 换版，I1 规约）
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
	 * （GE-C01：入口校验在 {@code ServiceManager.startService}，这里不重复设防）。</p>
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
	 * <p>现役保护按 {@link #foldVersionName} 折叠比对（GE-C01(FND22)）：参数来自 commit 的
	 * 请求 versionNo，而 Win32 解析下请求文本与盘上目录名可能分叉（"V1"vs"v1"、"v1."vs"v1"）
	 * ——裸 equals 使物理现役目录落入清理面被删（current 悬空）。折叠后指针文本与盘上真名
	 * 两个来处都命中保护；commitLocked 的指针规范化已使正常路径同名，此处折叠是独立的第二道
	 * 防（直调/存量分叉指针亦闭合）。Linux 上折叠=过度保护（同名变体目录都被保，有界），
	 * 与锁键折叠同款裁量。</p>
	 */
	void pruneVersions(File svcDir, String currentVersion) {
		var keep = keepVersions;
		if (keep <= 0)
			return; // 全保留
		var listFiles = svcDir.listFiles();
		if (null == listFiles)
			return;
		var foldedCurrent = foldVersionName(currentVersion);
		ArrayList<File> candidates = new ArrayList<>();
		for (var f : listFiles) {
			// 只把版本目录纳入清理面：current指针是文件天然排除；名字碰巧等于现役版本的目录不存在（构造上互斥）。
			if (f.isDirectory() && !foldVersionName(f.getName()).equals(foldedCurrent))
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
		for (var e : files.entrySet()) {
			var key = e.getKey();
			if (!key.startsWith(prefix))
				continue;
			if (files.remove(key, e.getValue())) {
				try {
					e.getValue().close();
				} catch (IOException ex) {
					logger.error("closeUnder {}", key, ex);
				}
			}
		}
	}
}
