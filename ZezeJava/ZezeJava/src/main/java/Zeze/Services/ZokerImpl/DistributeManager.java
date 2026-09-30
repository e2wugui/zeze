package Zeze.Services.ZokerImpl;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
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
 * 失败恢复：装版本失败无副作用（现役未动：验货前置，隔离腾位后的安装失败经回滚恢复
 * 原位，回滚失败为声明残余）；切换失败在换装路径回滚（新内容退回 distributes、旧内容
 * 复位版本名——同版本重部署形态下 current 指针文本未变，复位即现役内容恢复，重试重走
 * 换装收敛），纯新增路径现役未动（新版本目录已装好，重试直接进入切换收敛）；回滚的
 * rename 失败为声明残余。重试幂等：目标版本目录已存在（上次中断的残留，经完整性校验：
 * 自身清单齐全/legacy下限/新内容与已装版本逐文件内容一致——大小短路之上复算 md5，
 * legacy 形态与暂存区源文件逐一比对）= 跳过安装直接切换，同为成功
 * ——"存在=完整"由构造保证：安装是原子rename，删除（prune与隔离换装）先原子改名进暂存
 * 删除名再清树，版本名位置不出现残缺目录；校验不过的残缺目录不可收养：有新内容→隔离
 * 换装（残缺目录改名腾位），无新内容→eCommitFail。</p>
 */
public class DistributeManager {
	private static final Logger logger = LogManager.getLogger(DistributeManager.class);

	/** services/&lt;svc&gt;/ 下的现役版本指针文件名，内容为版本号文本。 */
	static final String CURRENT_NAME = "current";
	/** services/&lt;svc&gt;/ 下的暂存删除名前缀：{@code .zoker-deleting.<原目录名>.<millis>}。
	 * prune 的 victim 与 commit 的隔离换装共用：先同容器原子 rename 再整树删除——deleteTree
	 * 的部分失败只可能残缺暂存名，不再制造"存在但不完整"的版本名目录。暂存名排除出保留
	 * 计数与候选，上一轮残留由下一轮 prune 入口先清扫；versionNo 折叠后以前缀开头的按
	 * 保留字拒绝（isReservedVersionName）。 */
	static final String DELETING_STAGE_PREFIX = ".zoker-deleting.";
	/** distributes/&lt;svc&gt;/ 下的集合完整性清单文件名：部署方在全部文件
	 * CloseFile 收口后补传，行=各文件相对 distributeDir 根的路径（与 OpenFile 寻址同根）。
	 * commit 据此校验清单内文件齐全并清退清单外残留（见 {@link #verifyDistributeManifest}）；
	 * 该文件随版本目录成版，跳装分支以它为安装完成标志消费（start/prune 不读）。
	 * 主形态为版本限定名 {@link #distributeManifestName}（清单按部署归属，同名服务并发
	 * 分发互不覆盖），本裸名为旧客户端清单与兼容副本的回落消费名。 */
	public static final String DISTRIBUTE_MANIFEST_NAME = ".zoker-manifest";
	/** commit 后保留的版本目录数（含现役），超过的最老版本被清理；&lt;=0 表示全保留。 */
	static final int KEEP_VERSIONS_DEFAULT = 3;

	private final @Nullable Zoker zoker;
	private final File distributeDir;
	private final File serviceDir;
	// 键为distributeDir内实体文件的canonical路径经foldBarrierPath折叠（FND26 zoker-02，单点fileKey）：
	// canonical不归一大小写（仅剥尾点/尾空格），Windows上"Svc"/"svc"同一物理文件的变体拼写裸canonical
	// 分叉两键——closeUnder裸前缀扫不到（句柄幸存使renameTo恒false，eCommitFail无自愈直至持有方断链）、
	// 同物理文件双FileBin双写者（实例监视器互斥失效）、md5失败删除被孪生句柄钉死（删档重传的协议
	// 契约破裂成死循环）。折叠并键三面同闭。物理位置仍由FileBin.getCanonicalFile()承载——键折叠后
	// 不可作盘上路径用；commitService按（折叠）服务目录前缀回收、CWD无关（canonical基座不变）。
	// 平台边界：Linux上被并键的是不同物理文件（"Svc"/"svc"真两目录）——变体拼写并发操作同一部署
	// 属病态输入（与commitLocks/opsLocks/processes键折叠同款裁量），并键退化为共用一个FileBin与
	// 暂存面，内容冲突由CloseFile的md5收口挡在commit之前（可见失败，不静默错账）。
	private final ConcurrentHashMap<String, FileBin> files = new ConcurrentHashMap<>();
	// 每个agent连接打开的文件键（=files的折叠记账键）：agent在OpenFile之后、CloseFile之前断链时
	// 按连接回收FileBin，否则RandomAccessFile句柄常驻泄漏，Windows上还锁住distributes下的文件
	// 使commit的rename失败。
	private final ConcurrentHashMap<AsyncSocket, ConcurrentHashMap<String, FileBin>> filesBySocket
			= new ConcurrentHashMap<>();
	// 同服务 commit 串行化锁（services/<svc> 粒度）。键为 foldVersionName(serviceName) 折叠
	// （serviceName 已过 isSafePathSegment 校验；折叠可能并键的仅尾点/空格与大小写变体，
	// 过度串行化有界），条目数以（折叠后的）服务名为界，无攻击面放大。跨服务不受影响。
	private final ConcurrentHashMap<String, Object> commitLocks = new ConcurrentHashMap<>();
	// commit 进行中的服务前缀：distributes/<svc> 的 canonical 路径+分隔符，经 foldBarrierPath
	// 折叠存储（zoker-03）——canonical 不归一大小写/尾点，open 键的变体拼写（Windows 上
	// "svc"/"Svc"/"svc." 同一物理目录）裸 startsWith 分叉使变体绕过 barrier（新开 FileBin 跨越
	// rename，Windows 阻塞 rename 使 commit 瞬时失败）。open 的入账原子段内检查命中即拒绝
	// ——closeUnder 清账与 renameTo 之间新开的 FileBin 会漏出回收面。条目数=并发 commit 数，
	// 随 commit 结束摘除。Set按折叠前缀去重；commitLocks 键已统一 foldVersionName 折叠，
	// canonical 同一的 svc 名必得同一把锁，后继 commit 必在前者 finally 摘 barrier 之后才进入
	// ——先结束者提前摘 barrier 的边缘不复存在，无需计数化。
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
		var path = synthPath(serviceName, fileName);
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
				else if (null != sender && sender.isClosed())
					// zoker-01：为已死 socket 不建账。断链回收（closeBySocket，OnSocketClose 内）与
					// 本临界区取同一把 filesBySocket 锁，两序必居其一：先于本段执行则 close 回调已跑完、
					// 此刻再建的 socket 集合此后无任何路径回收（永驻死记账+持有已关 AsyncSocket）；
					// 后于本段执行则 isClosed 必为 false（markClosed 先于 OnSocketClose，终态不回退），
					// 建账条目由该次 closeBySocket 正常摘除。isClosed 在锁内的这一次复检是两种结局的
					// 线性化判别。拒绝方向安全：客户端断链收不到应答，重连重试即得活连接的账。
					rejectReason = "sender closed";
				else {
					// 建表与socket记账必须原子：锁外两步之间断链清账会把新建的FileBin
					// 漏出回收面（句柄泄漏+死socket映射永驻）。closeAndVerify/closeBySocket持同锁清账。
					var existing = files.putIfAbsent(relativeCanonicalFileName, candidate);
					if (null != existing)
						winner = existing;
					else if (null != sender)
						filesBySocket.computeIfAbsent(sender, __ -> new ConcurrentHashMap<>())
								.put(relativeCanonicalFileName, candidate);
				}
			}
			if (!rejectReason.isEmpty()) {
				closeDiscard(candidate);
				throw new IOException("open file rejected, " + rejectReason + ": " + path);
			}
			if (winner == candidate)
				return candidate;
			closeDiscard(candidate);
			accountOpened(sender, relativeCanonicalFileName, winner);
			return winner;
		}
		accountOpened(sender, relativeCanonicalFileName, fileBin);
		return fileBin;
	}

	/**
	 * 已入表实例的追加记账：建立者的putIfAbsent+记账已在同一原子段完成，此处只补本连接。
	 * zoker-01同治：锁内复检sender已死则不补——其close回调已跑完（或即将，届时摘的是既有条目），
	 * 补上的集合条目无回收路径（快路径 files.get 命中与 putIfAbsent 败者两处调用同暴露）。
	 */
	private void accountOpened(AsyncSocket sender, String relativeCanonicalFileName, FileBin fileBin) {
		if (null == sender)
			return;
		synchronized (filesBySocket) {
			if (sender.isClosed() || files.get(relativeCanonicalFileName) != fileBin)
				return;
			filesBySocket.computeIfAbsent(sender, __ -> new ConcurrentHashMap<>())
					.put(relativeCanonicalFileName, fileBin);
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
		// 键经 foldBarrierPath 折叠后与折叠存储的前缀比对（zoker-03）：变体拼写不再绕过
		// barrier。串操作无 IO，open 每次两查（锁外预检+锁内复检）开销可忽略。
		var folded = foldBarrierPath(relativeCanonicalFileName);
		for (var prefix : committingPrefixes)
			if (folded.startsWith(prefix))
				return true;
		return false;
	}

	public void append(String serviceName, String fileName, long offset, Binary data)
			throws IOException, NoSuchAlgorithmException {
		var fileBin = files.get(fileKey(synthPath(serviceName, fileName)));
		if (null == fileBin)
			throw new IOException("file not opened: " + serviceName + "/" + fileName); // 与Hot版一致，未Open直接Append会NPE且无上下文
		fileBin.append(offset, data);
	}

	// 测试 seam（closeAndVerify 交错注入点）：md5 失配判定后、清场删除前注入暂停/并发
	// 动作；生产恒 null。
	private volatile Runnable closeVerifyBeforeDeleteHookForTest;

	void setCloseVerifyBeforeDeleteHookForTest(Runnable hook) {
		closeVerifyBeforeDeleteHookForTest = hook;
	}

	/**
	 * 关闭并校验，三态返回协议错误码：
	 * 0=校验一致（文件保留，等待commit消费）；
	 * eNotOpened=文件不在传输中（未Open/已收尾/断链回收后补发的close），不谎报成功；
	 * eMd5Mismatch=校验失败，close 后删除物理文件（暂存区损坏中间产物无保留价值），
	 * 下次 OpenFile 从 0 续传，状态机闭合——坏起点不再占位把续传打回人工清理。
	 *
	 * <p><b>摘账→close→校验→删除全程持同服务 {@link #commitLocks}</b>（折叠服务段，与
	 * commit 同锁）：commit 的清单校验（isFile）与 renameTo 之间无复查，不持锁时并发的
	 * md5 失配清场删除落进该窗口会提交出缺文件/混合内容的版本并回执 0 切 current。同锁
	 * 后删除与 commit 两序必居其一，均收敛 eCommitFail（删除先于清单校验见缺文件；或
	 * commit 先行、closeUnder 在同锁内清账删掉在途产物）。锁序 commitLocks→filesBySocket
	 * 与 commit 同向无环；close/md5 的 IO 在纯本地 FS 操作锁 commitLocks 内，阻塞面仅
	 * 同服务并发 commit/close（closeBySocket 不取 commitLocks：摘账与 remove 亚微秒相邻
	 * 无删除窗口，且 event loop 线程等锁会停摆该 loop 全部连接读写）。</p>
	 */
	public long closeAndVerify(String serviceName, String fileName, Binary md5, AsyncSocket sender)
			throws IOException {
		var path = synthPath(serviceName, fileName);
		var relativeCanonicalFileName = fileKey(path);
		FileBin fileBin;
		synchronized (commitLockOf(path)) {
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
				var hook = closeVerifyBeforeDeleteHookForTest;
				if (null != hook)
					hook.run();
				// 删除失败仅告警：文件已close无句柄占用，正常不会失败；残留等下次md5失败重试删除
				if (!fileBin.getCanonicalFile().delete())
					logger.error("closeAndVerify md5 mismatch, delete corrupt file fail: {}", fileBin.getCanonicalFile());
				return err(Zoker.eMd5Mismatch);
			}
			return 0;
		}
	}

	/**
	 * closeAndVerify 的 commit 同锁键：真实流量 serviceName 为空串、服务段取自合成相对
	 * 路径首段（与 {@link #open} 的目录世代锚点同判据），经 {@link #foldVersionName} 折叠
	 * 后与 commit 的锁键（foldVersionName(RPC serviceName)）在真实流量与变体拼写下必同
	 * 键。文件直接位于 distributes 顶层（一级段即文件本身）时返回一次性空对象（无互斥）：
	 * commit 的 rename 单元是 distributes/&lt;一级段&gt; 目录，顶层文件永不被搬运；越界
	 * 拼写（checkInsideDir 会拒的形态）同样无账可摘，取空对象即原行为。
	 */
	private Object commitLockOf(String path) {
		var base = distributeDir.toPath().toAbsolutePath().normalize();
		var target = base.resolve(path).normalize();
		if (!target.startsWith(base) || target.getNameCount() <= base.getNameCount() + 1)
			return new Object();
		return commitLocks.computeIfAbsent(
				foldVersionName(target.getName(base.getNameCount()).toString()), __ -> new Object());
	}

	/** agent断链（ZokerService.OnSocketClose）时回收该连接打开的全部FileBin。 */
	public void closeBySocket(AsyncSocket socket) {
		var victims = new ArrayList<FileBin>();
		// 摘除记账与并发open原子，close在锁外（FileBin.close含md5读）。
		synchronized (filesBySocket) {
			var opened = filesBySocket.remove(socket);
			if (opened == null)
				return;
			// 记账携带实例身份；旧上传者的迟到断链不能摘掉同路径的新一轮上传。
			for (var entry : opened.entrySet()) {
				if (files.remove(entry.getKey(), entry.getValue()))
					victims.add(entry.getValue());
			}
		}
		for (var fileBin : victims) {
			try {
				fileBin.close();
			} catch (IOException ex) {
				logger.error("closeBySocket {}", fileBin.getRelativeCanonicalFileName(), ex);
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

	// 记账键单点（files/filesBySocket存取与closeUnder扫描共用的键形态，FND26 zoker-02）：
	// canonical路径经foldBarrierPath折叠——理由与平台边界见files字段注释；对已折叠键再折叠幂等。
	private String fileKey(String path) throws IOException {
		return foldBarrierPath(new File(distributeDir, path).getCanonicalFile().toString());
	}

	/**
	 * serviceName/fileName 到相对路径的合成单点。ZokerAgent 的三个文件 RPC 从不设置
	 * ServiceName（bean 默认空串），文件相对路径（含服务名首段）整体放在 FileName 里——
	 * 空 serviceName 是真实流量形态。{@code new File("", child)} 的空父目录会被 JDK 替换为
	 * 默认父（Windows "\"、Linux "/"），得到根相对/绝对路径，被 checkInsideDir 判为越界
	 * 逃逸拒绝——故空 serviceName 时直接采用 fileName（纯相对路径，边界守卫仍由
	 * checkInsideDir 承担）；非空 serviceName 保持既有合成。open/append/closeAndVerify
	 * 的路径与记账键必须同走本单点，保证三处键一致。
	 */
	private static String synthPath(String serviceName, String fileName) {
		return serviceName.isEmpty() ? fileName : new File(serviceName, fileName).getPath();
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
	 * 另拒绝折叠后为空串的名字（zoker-01单点收口）："..."、" "、".. "等通过上方字面检查，
	 * 但Win32解析剥尾点/空格后不构成物理段名——services/&lt;svc&gt;/...解析为容器本身：
	 * commit的versionTo.exists()恒真跳过安装，current指针被覆盖为该文本（假成功），
	 * pruneVersions的现役保护（foldedCurrent=""对任何真实版本目录的折叠值都不等）全数失配，
	 * 存量版本被清理、全新服务的内容子目录被当版本目录删除。收口在segment判别单点
	 * （serviceName与versionNo同面：serviceName="..."同样把容器根当部署目录），
	 * pruneLocked/onDiskVersionName等下游折叠比对无需变更即闭合。Linux上"..."/" "是
	 * 合法字面名，一并拒绝属折叠判据的既定过度保护裁量（与commitLocks键折叠同款）。
	 */
	static boolean isSafePathSegment(String name) {
		return name != null && !name.isEmpty() && !name.equals(".") && !name.equals("..")
				&& name.indexOf('/') < 0 && name.indexOf('\\') < 0 && name.indexOf(':') < 0
				&& !foldVersionName(name).isEmpty();
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
	 * 暂存删除名族（{@link #DELETING_STAGE_PREFIX} 前缀）同面扩入：跳装/指针收养一个
	 * deleteTree 半途残缺的暂存目录=收养残缺版本（跳装完整性判据的破坏面）。
	 * 折叠判据抽为 {@link #foldVersionName} 单点，与 pruneVersions 的
	 * 现役保护、commitLocked 的指针规范化共用同一语义。
	 */
	static boolean isReservedVersionName(String name) {
		var folded = foldVersionName(name);
		return folded.equals(foldVersionName(CURRENT_NAME))
				|| folded.equals(foldVersionName(ServiceManager.RUN_PID_NAME))
				|| isDeletingStageName(name);
	}

	/** 暂存删除名判别（排除/入口清扫与保留字增补共用的单点）：折叠后
	 * （剥尾点/空格+小写，与盘上解析同判据）以 {@link #DELETING_STAGE_PREFIX} 开头——
	 * 暂存名由本类生成，折叠只影响 Windows 变体拼写判同。 */
	static boolean isDeletingStageName(String name) {
		return foldVersionName(name).startsWith(DELETING_STAGE_PREFIX);
	}

	/**
	 * 版本名的盘上解析折叠（单点判据）：剥尾部点/空格 + 忽略大小写
	 * （toLowerCase(Locale.ROOT)）。Windows(Win32) 路径解析大小写不敏感且规范化剥尾部点/空格
	 * （跨大小写 exists 命中、renameTo 落盘名脱尾点占位），
	 * 即"请求文本"与"盘上实际目录名"可能是同一物理实体的两个拼写。所有需要"请求名与盘上名
	 * 判同"的位置（保留字碰撞 {@link #isReservedVersionName}、现役保护 pruneVersions、
	 * 指针规范化 commitLocked、commitLocks 键、ServiceManager 的 opsLocks/processes 记账键
	 * （serviceKey 单点）、files/filesBySocket 记账键（fileKey 单点，FND26 zoker-02）、
	 * barrier 前缀比对的段折叠 {@link #foldBarrierPath}、清单残留清理判同
	 * pruneUnlistedFiles（FND29 zoker-01））必须统一用本折叠，
	 * 不得裸 equals/裸 toLowerCase——分叉即互斥面击穿或现役目录落入清理面。
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

	/**
	 * 整路径的段级折叠（zoker-03 起为 barrier 匹配；FND26 zoker-02 起亦为 files/filesBySocket
	 * 记账键与 closeUnder 扫描前缀）：按 '/'/'\\' 切分后每段过
	 * {@link #foldVersionName}（剥尾点/空格+小写，判据同源单点不另立），分隔符统一 '/'，
	 * 保留首尾空段（前导根符号与尾随分隔符——后者是前缀比对不误吞相邻段
	 * （"…/svc/"不得命中"…/svc2/x"）的关键）。committingPrefixes 的存键、isCommitting 的键
	 * 折叠、fileKey 的记账键与 closeUnder 的扫描前缀两侧共用本函数：canonical
	 * （getCanonicalPath/getCanonicalFile）不做大小写归一、不剥尾点，Windows(Win32) 解析下
	 * 同物理目录的变体拼写（distributes\Svc\… vs distributes\svc.\）裸 equals/裸 startsWith
	 * 分叉——barrier 被绕过、记账键分叉成同物理文件双 FileBin（实例监视器互斥失效、
	 * closeUnder 漏扫阻塞 rename、md5 失败删除被孪生句柄钉死）；折叠后必同键、必匹配。
	 * Linux 上折叠把变体（真不同物理目录/文件）判为同键或提交中——barrier 方向是过度拒绝
	 * （barrier 本是省工预检，权威防线是锁内复检+closeUnder sweep+世代锚点），瞬时失败重试
	 * 即过；记账并键方向是共用一个 FileBin 与暂存面，内容冲突由 CloseFile 的 md5 收口挡在
	 * commit 之前（可见失败）。FND25 曾以"Linux 折叠并键合并不同物理文件"裁定记账键保持
	 * canonical——本波推翻该裁定：Windows 上折叠变体=同一物理文件（并键正是修复），Linux 上
	 * 并键触发面（变体拼写并发操作同一部署）与 commitLocks/opsLocks 键折叠同款属病态输入，
	 * 两平台权衡后统一折叠（详见 files 字段注释）。
	 */
	static String foldBarrierPath(String path) {
		var segments = path.split("[/\\\\]", -1); // -1保留尾空段=尾随分隔符
		for (var i = 0; i < segments.length; i++)
			segments[i] = foldVersionName(segments[i]);
		return String.join("/", segments);
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
				// 折叠存储（zoker-03）：与 isCommitting 的键折叠共用 foldBarrierPath——
				// 裸 canonical 前缀对变体拼写（Windows 同物理目录的 "Svc"/"svc."）不命中。
				prefix = foldBarrierPath(serviceFrom.getCanonicalPath() + File.separator);
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
		// 跳装分支的完整性判据："存在=完整"由构造保证（原子rename安装+暂存名删除），
		// 残缺目录不可收养（无条件跳装=current切到残缺目录的假成功）。判不可收养：
		// (a)自带清单列的文件缺失（清单=安装完成标志）；(b)legacy下限不过（空壳）；
		// (c)新上传内容与已装版本目录字节不一致（存在≠内容：清单条目逐文件比对
		// 大小+md5，legacy 形态与暂存区源文件逐一比对）。
		// 处置：有新内容→隔离换装（残缺目录原子改名进暂存删除名腾位，新内容落正常
		// 安装分支）；无新内容→eCommitFail。
		var swapNeeded = versionTo.exists()
				&& !(installedVersionHealthy(versionTo, serviceName) && distributesManifestSubsetOf(serviceFrom, versionTo));
		// 验货前置：新内容的全部前置校验（源目录存在、清单校验与残留清退、空目录拒绝）
		// 先于任何腾位执行，且只读写 distributes/<svc>——先证明新内容可安装，再动既有
		// 版本目录（跳装分支不清退不消费 distributes，不经此处，幂等语义不变）。
		if (swapNeeded || !versionTo.exists()) {
			if (!serviceFrom.isDirectory()) {
				if (swapNeeded)
					logger.error("commitService broken version residue and no distribute content: {}", versionTo);
				else
					logger.error("commitService no distribute content: {}", serviceFrom);
				return err(Zoker.eCommitFail);
			}
			// 集合级完整性屏障：文件级md5（CloseFile）与目录级搬运（rename）之间以
			// 部署方在全部文件收口后补传的集合清单为"本次发布集合已完整"的声明——
			// commit校验齐全+清退残留；无清单走legacy路径（外部部署工具/直构形态），
			// 由下方空目录拒绝兜底。
			var manifestRc = verifyDistributeManifest(serviceFrom, versionNo);
			if (manifestRc != 0)
				return manifestRc;
			// 空目录拒绝：closeUnder清掉在途未验证中间产物后目录可能为空——空版本
			// 切current后start恒eNoServiceProperties直到重新commit，不得成版。
			var remains = serviceFrom.listFiles();
			if (null == remains || 0 == remains.length) {
				logger.error("commitService empty distribute content: {}", serviceFrom);
				return err(Zoker.eCommitFail);
			}
		}
		// 隔离腾位紧邻安装：腾位与最终 rename 间的悬空窗（current==versionNo 停止态）
		// 收缩到相邻两句 rename，失败路径由 rollbackQuarantinedVersion 闭合（跨平台无
		// 目录原子交换 API，残余=回滚失败）。
		var quarantinedStage = new File[]{null};
		if (swapNeeded) {
			var processManager = null != zoker ? zoker.getProcessManager() : null;
			Runnable swapStep = () -> {
				if (null != processManager) {
					// 在用版本保护：run.pid指向的版本目录不隔离（服务不随current切换重启时仍从
					// 旧版本目录运行，隔离+暂存清扫=拆运行中服务的文件）。与start的
					// launch→writeRunPid窗口互斥（withServiceLock；锁序commitLocks→opsLocks单向
					// 嵌套）；版本不可知（旧格式身份空串）无法证明不是本版本，宁拒不删。
					var running = processManager.runningVersion(serviceName);
					if (null != running && (running.isEmpty()
							|| foldVersionName(running).equals(foldVersionName(versionNo)))) {
						logger.error("commitService refuse swap, version in use by running service: services/{} version={} running={}",
								serviceName, versionNo, running);
						return;
					}
				}
				quarantinedStage[0] = quarantineVersion(svcDir, versionTo);
				if (null == quarantinedStage[0])
					logger.error("commitService quarantine rename fail (retryable): {}", versionTo);
			};
			if (null != processManager)
				processManager.withServiceLock(serviceName, swapStep);
			else
				// 直构测试形态：无进程身份可锁/可保护。
				swapStep.run();
			if (null == quarantinedStage[0])
				return err(Zoker.eCommitFail); // 在用拒绝/隔离rename失败（Windows句柄占用）：可重试，现役未动
		}
		if (!versionTo.exists()) {
			// 先验源存在再动目标目录：step1失败（distributes/<svc>缺失——重复提交/超时重试的
			// 典型情形）时连services/<svc>容器目录也不创建，彻底无副作用（源目录判别已前置）。
			try {
				// renameTo不创建父目录：services/<svc>/这层缺失时renameTo必false
				Files.createDirectories(svcDir.toPath());
			} catch (IOException ex) {
				logger.error("commitService createDirectories {}", svcDir, ex);
				rollbackQuarantinedVersion(quarantinedStage[0], versionTo);
				return err(Zoker.eCommitFail);
			}
			if (!serviceFrom.renameTo(versionTo)) {
				logger.error("commitService install version fail: {} -> {}", serviceFrom, versionTo);
				rollbackQuarantinedVersion(quarantinedStage[0], versionTo);
				return err(Zoker.eCommitFail); // 腾位已回滚（全新安装则本无腾位）：可重试，现役未动
			}
			// rename保留源目录的最后修改时间（=上传时间），盖写为安装时间，作为保留策略的排序依据。
			if (!versionTo.setLastModified(System.currentTimeMillis()))
				logger.warn("commitService setLastModified fail: {}", versionTo);
		}
		// 指针与 prune 参数用盘上实际目录名，不用请求原样文本。Win32 解析下
		// "V1"跨大小写命中 v1 跳装、"v1."renameTo 落盘为 v1——原样文本写指针后
		// pruneVersions 的现役保护面对"盘上真名 vs 请求文本"分叉时物理现役目录落入
		// 清理面被删。规范化后指针、prune 参数、盘上目录三者同名，分叉源头闭合。
		var installed = onDiskVersionName(svcDir, versionNo);
		try {
			switchCurrent(svcDir, installed);
		} catch (IOException ex) {
			// 换装路径（腾位+安装已成功）补回滚：新内容退回 distributes、旧内容复位版本名
			//（见 rollbackSwitchFailure）；纯新增安装无腾位，current 未动，重试跳装再切收敛。
			rollbackSwitchFailure(serviceName, serviceFrom, versionTo, quarantinedStage[0]);
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
	 * 跳装分支的完整性判据：版本名目录的存在不无条件等价于完整。
	 * <ul>
	 * <li>自带 {@link #DISTRIBUTE_MANIFEST_NAME}（随版本成版的自身清单）=安装完成标志：
	 * 清单列的文件须全部在版本目录内（缺失=目录被削过=残缺）。</li>
	 * <li>无清单（legacy/外部部署工具）：下限=目录树内至少一个常规文件（完全空壳不可
	 * 收养：switch过去start恒eNoServiceProperties）。</li>
	 * <li>非目录实体（文件占位版本名）恒不可收养（跳装切换=currentVersionDir恒null的
	 * 假成功面）。</li>
	 * </ul>
	 */
	/** 版本限定的清单文件名：{@link #DISTRIBUTE_MANIFEST_NAME}.&lt;versionNo&gt;——清单按部署
	 * 归属，同名服务并发分发互不覆盖，commit 只消费本次版本号对应的清单（versionNo 已过
	 * isSafePathSegment，限定名不构成路径注入）。 */
	public static String distributeManifestName(String versionNo) {
		return DISTRIBUTE_MANIFEST_NAME + '.' + versionNo;
	}

	/** 清单文件定位单点（已安装版本目录的收养判据消费，installedVersionHealthy）：
	 * 优先版本限定名，缺失回落裸名——版本目录内的裸名是安装方自己的兼容副本（随
	 * 版本成版的密封快照，无并发面）。两者皆缺时返回裸名 File（isFile=false，调用
	 * 方按 legacy 处置）。distributes 暂存区的消费（安装校验/跳装比对）走
	 * {@link #stagedManifestOf}（回落带归属防线）。 */
	private static File manifestFileOf(File dir, String versionNo) {
		var versioned = new File(dir, distributeManifestName(versionNo));
		return versioned.isFile() ? versioned : new File(dir, DISTRIBUTE_MANIFEST_NAME);
	}

	/** 暂存区清单定位（安装校验/跳装比对两处消费面共用）：优先本次版本限定名；缺失时
	 * 若暂存区存在<b>他版本</b>的限定清单（并发分发会话在途、或先到 commit 的清退已删除
	 * 本次限定清单），裸名内容无法证明归属本次部署——消费裸名即校验他方条目、以自己的
	 * versionNo 成版切 current，静默错版。此形态拒绝回落返回 null，调用方响亮失败（重传
	 * 补齐自己的限定清单后正常消费）；无任何限定清单的 legacy 形态回落语义不变。 */
	private static @Nullable File stagedManifestOf(File serviceFrom, String versionNo) {
		var versioned = new File(serviceFrom, distributeManifestName(versionNo));
		if (versioned.isFile())
			return versioned;
		if (hasForeignVersionedManifest(serviceFrom, versionNo))
			return null;
		return new File(serviceFrom, DISTRIBUTE_MANIFEST_NAME);
	}

	/** 暂存区内是否存在他版本的限定清单：文件名折叠（变体拼写判同，与幸免面/保留字
	 * 同判据）后以 {@link #DISTRIBUTE_MANIFEST_NAME} 加点开头且不等于本次限定名。 */
	private static boolean hasForeignVersionedManifest(File dir, String versionNo) {
		var listFiles = dir.listFiles();
		if (null == listFiles)
			return false;
		var foldedPrefix = DISTRIBUTE_MANIFEST_NAME + '.';
		var foldedOwn = foldVersionName(distributeManifestName(versionNo));
		for (var f : listFiles) {
			if (f.isFile()) {
				var folded = foldVersionName(f.getName());
				if (folded.startsWith(foldedPrefix) && !folded.equals(foldedOwn))
					return true;
			}
		}
		return false;
	}

	private static boolean installedVersionHealthy(File versionTo, String serviceName) {
		if (!versionTo.isDirectory())
			return false;
		var manifest = manifestFileOf(versionTo, versionTo.getName());
		if (manifest.isFile())
			return manifestEntriesAllPresent(manifest, versionTo, serviceName);
		try (var walk = Files.walk(versionTo.toPath())) {
			return walk.anyMatch(Files::isRegularFile);
		} catch (IOException ex) {
			logger.error("installed version health walk fail: {}", versionTo, ex);
			return false;
		}
	}

	/**
	 * 跳装分支的新内容比对：带新清单时条目须全部已存在于既有版本目录且<b>内容一致</b>
	 * ——大小短路之上逐文件复算两侧全量 md5（CloseFile 的 md5 不落盘、commit 期现算），
	 * 同内容重提快速跳装幂等（不覆盖已装版本、不动其 mtime）；任何不一致（含同大小
	 * 不同字节=部署陈旧字节）判不可收养走隔离换装（在用版本被 run.pid 保护挡住）。
	 * 无清单（legacy重提）：与 distributes 源文件逐一内容比对；暂存区不存在（无新内容
	 * 的幂等重试）比对空过，跳装语义不变。
	 */
	private boolean distributesManifestSubsetOf(File serviceFrom, File versionTo) {
		// versionTo 即按本次 versionNo 构造，限定名归属本次部署。
		var manifest = stagedManifestOf(serviceFrom, versionTo.getName());
		if (null == manifest)
			// 裸名回落被拒（他版本限定清单在场=并发会话面）：不可经跳装收养，
			// 交安装分支的校验统一拒绝。
			return false;
		if (!manifest.isFile())
			return stagedLegacyFilesAllMatchInstalled(serviceFrom, versionTo);
		var serviceName = serviceFrom.getName();
		var base = versionTo.toPath().toAbsolutePath().normalize();
		var listed = 0;
		try (var reader = Files.newBufferedReader(manifest.toPath(), StandardCharsets.UTF_8)) {
			String line;
			while (null != (line = reader.readLine())) {
				if (line.isBlank())
					continue;
				listed++;
				var canonical = validatedManifestLine(line, serviceName);
				if (null == canonical)
					return false;
				var target = base.resolve(afterFirstSegment(canonical)).normalize();
				if (!target.startsWith(base) || !Files.isRegularFile(target))
					return false;
				// 内容判据（大小短路+md5）：暂存区新字节 vs 已装版本目录字节，任一
				// 不一致即判不可收养（宁换装不收养，方向与存在性判据同裁量）。
				if (!stagedFileEqualsInstalled(distributeDir.toPath().resolve(canonical), target))
					return false;
			}
		} catch (IOException ex) {
			logger.error("distributes manifest subset check read fail: {}", manifest, ex);
			return false;
		}
		return listed > 0;
	}

	/**
	 * legacy（无清单）形态的跳装比对：暂存区全部常规文件与已装版本目录对应文件逐一
	 * 内容比对（{@link #stagedFileEqualsInstalled}）。暂存区不存在（无新内容——重复
	 * 提交/超时重试的典型情形）比对空过返回 true，跳装语义不变；暂存区存在但任一
	 * 文件缺失/不等（含清单外新文件——已装版本没有对应物）即非同一内容，走隔离换装。
	 */
	private static boolean stagedLegacyFilesAllMatchInstalled(File serviceFrom, File versionTo) {
		if (!serviceFrom.isDirectory())
			return true;
		var stagedRoot = serviceFrom.toPath().toAbsolutePath().normalize();
		var base = versionTo.toPath().toAbsolutePath().normalize();
		try (var walk = Files.walk(stagedRoot)) {
			return walk.filter(Files::isRegularFile)
					.allMatch(file -> stagedFileEqualsInstalled(file, base.resolve(stagedRoot.relativize(
							file.toAbsolutePath().normalize()))));
		} catch (IOException ex) {
			logger.error("legacy staged content compare walk fail: {}", serviceFrom, ex);
			return false;
		}
	}

	/**
	 * 跳装比对的逐文件内容判据：两侧均存在为前提上先比大小（快速短路），相等再各自
	 * 流式复算全量 md5 比对。任一侧不可读/不可 stat 判不等（由安装分支的校验收口）。
	 */
	private static boolean stagedFileEqualsInstalled(Path stagedFile, Path installedFile) {
		try {
			if (!Files.isRegularFile(installedFile) || Files.size(stagedFile) != Files.size(installedFile))
				return false;
		} catch (IOException ex) {
			return false;
		}
		try {
			return Arrays.equals(md5Of(stagedFile), md5Of(installedFile));
		} catch (IOException | NoSuchAlgorithmException ex) {
			logger.error("skip-install md5 compare fail: {} vs {}", stagedFile, installedFile, ex);
			return false;
		}
	}

	/** 文件全量 md5 流式计算（与 FileBin 传输校验同算法、非同会话——CloseFile 会话
	 * 摘要不落盘，跳装比对在 commit 期对盘上两侧文件现算）。 */
	private static byte[] md5Of(Path file) throws IOException, NoSuchAlgorithmException {
		var digest = MessageDigest.getInstance("MD5");
		try (var input = Files.newInputStream(file)) {
			var buffer = new byte[32 * 1024];
			for (int n; (n = input.read(buffer)) >= 0; )
				digest.update(buffer, 0, n);
		}
		return digest.digest();
	}

	/**
	 * 清单条目齐全性（单点，跳装分支两处判据共用）：清单行经 {@link #canonicalManifestLine}
	 * 解析（非法形态=不齐全）且首段折叠等于本服务名（跨服务引用行不会被 rename 搬运，
	 * 收养即残缺版本），剥首段后须为 root 内常规文件且不逃逸（清单是数据不是可信输入）。
	 * 读失败与零条目清单按不齐全（宁隔离换装不收养，方向安全）。
	 */
	private static boolean manifestEntriesAllPresent(File manifest, File root, String serviceName) {
		var base = root.toPath().toAbsolutePath().normalize();
		var listed = 0;
		try (var reader = Files.newBufferedReader(manifest.toPath(), StandardCharsets.UTF_8)) {
			String line;
			while (null != (line = reader.readLine())) {
				if (line.isBlank())
					continue;
				listed++;
				var canonical = validatedManifestLine(line, serviceName);
				if (null == canonical)
					return false;
				var rel = afterFirstSegment(canonical);
				var target = base.resolve(rel).normalize();
				if (!target.startsWith(base) || !Files.isRegularFile(target))
					return false;
			}
		} catch (IOException ex) {
			logger.error("manifest entries check read fail: {}", manifest, ex);
			return false;
		}
		return listed > 0;
	}

	/** 清单行 canonical 解析+首段判同的合单点（校验/跳装两侧清单消费共用）：非法返回 null。 */
	private static @Nullable String validatedManifestLine(String line, String serviceName) {
		var canonical = canonicalManifestLine(line);
		return null != canonical && manifestLineFirstSegmentMatches(canonical, serviceName) ? canonical : null;
	}

	/**
	 * 清单行解析单点（校验/清退/跳装收养三侧判据同源）：合法形态唯一——相对 distributeDir
	 * 根、首段=服务名的多段相对路径。按 '/'/'\\' 切分后拒绝空段（首尾/双分隔符、绝对路径
	 * 与 UNC 根的前导空段）、"."与".."段及单段行（顶层文件不属于任何服务暂存区，rename
	 * 永不搬运）；变体行<b>拒绝而非消解</b>（消解会使校验侧与清退侧判据不同源——已列且
	 * 在盘的文件被当未列残留删除）。返回 '/' 连接的 canonical 形态；非法返回 null。
	 */
	static @Nullable String canonicalManifestLine(String line) {
		if (line.isEmpty())
			return null;
		var segments = line.split("[/\\\\]", -1);
		if (segments.length < 2)
			return null;
		for (var segment : segments) {
			if (segment.isEmpty() || segment.equals(".") || segment.equals(".."))
				return null;
		}
		return String.join("/", segments);
	}

	/** 清单行首段（服务名）判同（canonical 形态）：折叠比对——大小写/尾点空格变体是
	 * 同一物理部署的拼写分叉须通过；首段不同的行=跨服务引用，收养即成版却缺清单自
	 * 声明的文件。 */
	private static boolean manifestLineFirstSegmentMatches(String canonicalLine, String serviceName) {
		var first = canonicalLine.substring(0, canonicalLine.indexOf('/'));
		return foldVersionName(first).equals(foldVersionName(serviceName));
	}

	private static String afterFirstSegment(String line) {
		for (var i = 0; i < line.length(); i++) {
			var c = line.charAt(i);
			if (c == '/' || c == '\\')
				return line.substring(i + 1);
		}
		return "";
	}

	/**
	 * 隔离换装：不可收养的版本目录原子改名进暂存删除名（与 prune 的暂存删除同一前缀，
	 * 同容器rename）——版本名位置腾给新内容落正常安装分支；暂存名由下一次 pruneVersions
	 * 入口清扫。rename 失败（Windows句柄占用等）返回 null：eCommitFail 可重试，现役未动。
	 * 成功返回暂存名目录：安装期失败时由 {@link #rollbackQuarantinedVersion} 改回原位。
	 */
	private static @Nullable File quarantineVersion(File svcDir, File versionTo) {
		var stage = new File(svcDir, DELETING_STAGE_PREFIX + versionTo.getName() + '.' + System.currentTimeMillis());
		if (!versionTo.renameTo(stage))
			return null;
		logger.warn("commitService quarantined unadoptable version dir: {} -> {}", versionTo, stage);
		return stage;
	}

	/**
	 * 隔离换装的失败回滚：腾位后安装失败（容器创建/最终rename）时把暂存名目录
	 * best-effort 改回原版本名——current 指针内容未被触碰，指针→目录可达性即恢复
	 * （停止态现役重新可启动）。回滚失败（腾位与回滚之间外部句柄钉住暂存目录）为声明
	 * 残余：响亮 error 携带暂存名与原位置供人工抢救。staged 为 null（未腾位）时无操作。
	 */
	private static void rollbackQuarantinedVersion(@Nullable File staged, File versionTo) {
		if (null == staged)
			return;
		if (staged.renameTo(versionTo)) {
			logger.warn("commitService rollback: quarantined version restored {} -> {}", staged, versionTo);
			return;
		}
		logger.error("commitService rollback quarantined version FAIL, current dangling: {} -> {} (manual rescue)",
				staged, versionTo);
	}

	/**
	 * switchCurrent 失败的换装路径回滚（腾位+安装均已成功）：撤销安装——新内容 rename 回
	 * distributes/&lt;svc&gt;（撤销目标被并发 open 的残留占位时先清，barrier 在场只可能是被拒
	 * 候选的未验证产物），重试重走完整换装收敛，不产生"旧内容复位+暂存区空"的跳装假成功；
	 * 旧内容复位——暂存名改回版本名（{@link #rollbackQuarantinedVersion}），同版本重部署
	 * 形态下 current 指针文本本就等于 versionNo，版本名复位即现役内容恢复。新内容退不回
	 * （外部句柄钉住等）时退路=改名进暂存删除名腾出版本名并响亮 error（此形态重试跳装假
	 * 成功，需人工）。全程 best-effort：失败为声明残余，与安装期回滚同裁量。未腾位（纯新增
	 * 安装）无操作：current 未动，重试跳装再切收敛。与腾位同持 opsLocks：回滚两 rename
	 * 之间不与 start/stop 交错。
	 */
	private void rollbackSwitchFailure(String serviceName, File serviceFrom, File versionTo,
									   @Nullable File quarantinedStage) {
		if (null == quarantinedStage)
			return;
		var processManager = null != zoker ? zoker.getProcessManager() : null;
		Runnable rollbackStep = () -> {
			if (serviceFrom.exists() && !deleteTree(serviceFrom))
				logger.warn("commitService rollback clear distributes residue fail: {}", serviceFrom);
			if (!versionTo.renameTo(serviceFrom)) {
				var stage = new File(versionTo.getParent(),
						DELETING_STAGE_PREFIX + versionTo.getName() + '.' + System.currentTimeMillis());
				if (versionTo.renameTo(stage))
					logger.error("commitService rollback restage new content (retry adopts old version"
							+ " as installed, manual rescue): {} -> {}", versionTo, stage);
				else
					logger.error("commitService rollback uninstall FAIL, version name still holds new"
							+ " content (manual rescue): {}", versionTo);
			}
			rollbackQuarantinedVersion(quarantinedStage, versionTo);
		};
		if (null != processManager)
			processManager.withServiceLock(serviceName, rollbackStep);
		else
			// 直构测试形态：无进程身份可锁（与腾位 swapStep 同裁量）。
			rollbackStep.run();
	}

	/**
	 * 集合级完整性屏障：distributes/&lt;svc&gt;/ 下存在清单时校验并
	 * 清退残留，不存在走 legacy 路径返回0。清单定位优先版本限定名（归属本次部署，并发
	 * 分发互不覆盖，commit 只消费本次版本号对应的清单），缺失回落裸名（旧客户端/兼容
	 * 副本；他版本限定清单在场时拒绝回落，见 {@link #stagedManifestOf}）。清单行=各文件相对
	 * distributeDir 根的路径（与 OpenFile 寻址同根）。清单是
	 * 数据不是可信输入：逐行过 {@link #canonicalManifestLine} 形态解析与
	 * {@link #manifestLineFirstSegmentMatches} 首段判同（checkInsideDir 同款拒绝绝对
	 * 路径/../逃逸/盘符），坏清单响亮拒绝；清退判同集合用 canonical 形态。
	 */
	private long verifyDistributeManifest(File serviceFrom, String versionNo) {
		var manifest = stagedManifestOf(serviceFrom, versionNo);
		if (null == manifest) {
			logger.error("commitService refuse bare manifest fallback, other version manifests present "
					+ "(concurrent distribute?): {}", serviceFrom);
			return err(Zoker.eCommitFail);
		}
		if (!manifest.isFile())
			return 0; // legacy：无清单不设障（空目录拒绝另行兜底）
		var serviceName = serviceFrom.getName();
		var listed = new HashSet<String>();
		try (var reader = Files.newBufferedReader(manifest.toPath(), StandardCharsets.UTF_8)) {
			String line;
			while (null != (line = reader.readLine())) {
				if (line.isBlank())
					continue;
				var canonical = canonicalManifestLine(line);
				if (null == canonical) {
					logger.error("commitService manifest malformed entry: '{}' (dot/empty segment or single segment)", line);
					return err(Zoker.eCommitFail);
				}
				if (!manifestLineFirstSegmentMatches(canonical, serviceName)) {
					logger.error("commitService manifest entry not under this service: '{}'", line);
					return err(Zoker.eCommitFail);
				}
				try {
					checkInsideDir(distributeDir, canonical);
				} catch (IOException ex) {
					logger.error("commitService manifest unsafe entry: '{}'", line);
					return err(Zoker.eCommitFail);
				}
				if (!new File(distributeDir, canonical).isFile()) {
					logger.error("commitService manifest entry missing on disk: '{}' (upload interrupted?)", canonical);
					return err(Zoker.eCommitFail); // 部分集合不得成版：部署方重传后重commit
				}
				listed.add(canonical);
			}
		} catch (IOException ex) {
			logger.error("commitService read distribute manifest fail: {}", manifest, ex);
			return err(Zoker.eCommitFail);
		}
		if (listed.isEmpty()) {
			logger.error("commitService empty distribute manifest: {}", manifest);
			return err(Zoker.eCommitFail); // 零文件的版本不可启动，与空目录同拒
		}
		pruneUnlistedFiles(serviceFrom, listed, versionNo);
		return 0;
	}

	/**
 * 清退清单外残留（barrier+closeUnder 已收殓在途句柄，剩余未列文件=前次中断部署的
 * 残留）：删除使版本内容=清单声明的精确集合。listed 为 verifyDistributeManifest
	 * 产出的 canonical 行（形态与首段已校验）；判同（清单行 vs walk路径）两侧拼写不同源
	 * （上传侧 vs 提交侧+盘上实际名），统一过 {@link #foldBarrierPath} 段折叠——变体
	 * 拼写不折叠即已列文件落入清理面。删除失败仅warn；空子目录不递归清理（无消费者，无害）。
	 */
	private void pruneUnlistedFiles(File serviceFrom, Set<String> listed, String versionNo) {
		var root = distributeDir.toPath().toAbsolutePath().normalize();
		// 判同两侧拼写不同源：清单行=上传侧拼写，walk相对路径=提交侧服务名+盘上实际名。
		// Win32 变体拼写（Svc/svc.）指向同一物理文件时裸 contains 必失配——已列文件
		// 整体落入清理面被删。判同走段级折叠。
		var foldedListed = new HashSet<String>();
		for (var line : listed)
			foldedListed.add(foldBarrierPath(line));
		// 幸免面=控制文件：裸名清单（兼容副本）与本次版本限定清单——他版本的限定清单
		// 不是本次部署的控制文件，随清单外残留清退（该会话 commit 可见失败、重传收敛；
		// 清退引发的"限定清单缺失→裸名回落"错版面由 stagedManifestOf 拒绝闭合）。
		// 幸免面不得扩到全部限定清单：被幸免的他方数据将随 rename 混入本次版本内容，
		// 击穿"版本内容=清单精确集合"屏障。
		var spared = Set.of(
				foldBarrierPath(serviceFrom.getName() + "/" + DISTRIBUTE_MANIFEST_NAME),
				foldBarrierPath(serviceFrom.getName() + "/" + distributeManifestName(versionNo)));
		try (var walk = Files.walk(serviceFrom.toPath())) {
			walk.filter(Files::isRegularFile).forEach(file -> {
				var rel = root.relativize(file.toAbsolutePath().normalize()).toString().replace('\\', '/');
				var foldedRel = foldBarrierPath(rel);
				if (foldedListed.contains(foldedRel) || spared.contains(foldedRel))
					return;
				try {
					Files.delete(file);
					logger.info("commitService pruned unlisted residue: {}", rel);
				} catch (IOException ex) {
					logger.warn("commitService prune unlisted fail (committed with it, manual check): {}", rel, ex);
				}
			});
		} catch (IOException ex) {
			logger.warn("commitService prune walk fail: {}", serviceFrom, ex);
		}
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
 * 现役版本永不删除；超出的最老版本先原子改名进暂存删除名（{@link #DELETING_STAGE_PREFIX}）
 * 再整树删除——删除的部分失败只可能残缺暂存名（非版本名目录），暂存残留由下一轮
 * prune 入口清扫（"存在=完整"对版本名目录由构造保持，跳装判据的前提）。
 * listFiles的null（目录消失/权限）视为无事可做。
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
		// zoker-02：prune 段纳入与 start/stop 同粒度互斥（ServiceManager.opsLocks，键同
		// foldVersionName 折叠——ServiceManager.serviceKey 单点）。start 持锁的 launch→writeRunPid 窗口内
		// run.pid 是旧身份，本判据读到 null/旧版本就会把正在启动的版本目录当非在用删除
		// （进程炸/NoClassDefFound，两个 RPC 各自"成功"的静默错账）；共锁后 start 必先在锁内落盘
		// 新身份，prune 判得到它。锁序 commitLocks→opsLocks 单向嵌套（调用方 commit 全程持
		// commitLocks）：start/stop 不取 commitLocks，无反向持锁路径，无环。锁键（prune 传容器
		// 目录名=commit 请求的 serviceName 原样拼写）与 start/stop 的 RPC 名经同一 foldVersionName
		// 折叠后同键——Windows 同物理容器的尾点/空格/大小写变体不再分叉两把锁（首波 zoker-07
		// 笔记已记的未闭合族，本波闭合；Linux 变体过度串行化同既有裁量）。
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
		// 暂存删除名的入口清扫：上一轮prune/隔离换装的deleteTree失败残留。暂存名不在
		// 版本语义面内（不进keep计数/候选/现役保护），清扫是纯垃圾回收——不受keep<=0
		// （全保留）与在用版本未知跳过的约束。
		for (var f : listFiles) {
			if (f.isDirectory() && isDeletingStageName(f.getName()) && !deleteTree(f))
				logger.warn("pruneVersions sweep staged residue fail, retry next commit: {}", f);
		}
		if (keep <= 0)
			return; // 全保留（仅上面的暂存名清扫仍执行）
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
			// 暂存删除名排除：不在版本语义面内（入口已清扫/清扫失败残留等下一轮），
			// 也不得挤占keep名额把真实版本挤入清理面。
			if (f.isDirectory() && !isDeletingStageName(f.getName())
					&& !foldVersionName(f.getName()).equals(foldedCurrent)
					&& (null == foldedRunning || !foldVersionName(f.getName()).equals(foldedRunning)))
				candidates.add(f);
		}
		// 现役已占1个名额：非现役里保留最新的 keep-1 个，其余删除。
		if (candidates.size() <= keep - 1)
			return;
		candidates.sort(Comparator.comparingLong(File::lastModified).reversed()); // 新→旧
		for (int i = keep - 1; i < candidates.size(); i++) {
			var victim = candidates.get(i);
			// 暂存名先行：同容器原子rename后再整树删除——部分失败只可能残缺暂存名，版本名
			// 位置不再出现"存在但不完整"的目录。rename失败（victim被句柄钉住）=本轮跳过
			// 该victim，残留完整待下轮重试。
			var stage = new File(svcDir, DELETING_STAGE_PREFIX + victim.getName() + '.' + System.currentTimeMillis());
			if (!victim.renameTo(stage)) {
				logger.warn("pruneVersions stage rename fail, keep on next commit: {}", victim);
				continue;
			}
			if (!deleteTree(stage))
				logger.warn("pruneVersions delete staged fail, sweep on next commit: {}", stage);
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
			// 前缀与files记账键同经foldBarrierPath折叠（FND26 zoker-02）：canonical不归一大小写，
			// Windows上变体拼写（Svc vs svc，同一物理目录）的记账键裸前缀扫不到——句柄幸存使
			// renameTo恒false（eCommitFail无自愈）。FND25"精确匹配、不做变体推断"的前提是记账键
			// canonical原样；键既已折叠，扫描随之同折叠才与键空间一致（非推断，是同键判同）。
			prefix = foldBarrierPath(dir.getCanonicalPath() + File.separator);
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
				// N01（FND28）：在途（未经CloseFile md5收口）的传输中间产物不随rename卷入已提交
				// 版本目录——关闭句柄后按部署语义弃置（对齐closeAndVerify md5失配的删除口径：
				// 暂存区未验证中间产物无保留价值）。此前只关句柄不清内容，commit把另一传输流写了一半
				// 的文件整目录搬成services/<svc>/<v>并返回0——版本内容被污染而提交方无感知（md5只在
				// CloseFile校验，该文件从未走到）。barrier+锁内复检保证sweep与renameTo之间无新开
				// FileBin入表，删除面与摘账面一致；并发append持FileBin实例监视器，close串行在其后，
				// 删除不会与写交错。删除失败仅warn：句柄已释放，正常不失败，失败则该文件仍随目录
				// 提交（回归污染形态，靠warn暴露人工处置）。
				if (!e.getValue().getCanonicalFile().delete())
					logger.warn("closeUnder delete unverified in-flight file fail (committed with it, manual check): {}",
							e.getValue().getCanonicalFile());
			} catch (IOException ex) {
				logger.error("closeUnder {}", e.getKey(), ex);
			}
		}
	}
}
