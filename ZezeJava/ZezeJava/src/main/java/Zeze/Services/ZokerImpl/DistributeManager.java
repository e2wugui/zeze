package Zeze.Services.ZokerImpl;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
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

	public boolean closeAndVerify(String serviceName, String fileName, Binary md5, AsyncSocket sender)
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
		if (null != fileBin) {
			fileBin.close();
			var md5Local = fileBin.md5Digest();
			return Arrays.compare(md5Local, md5.bytesUnsafe()) == 0;
		}
		return true;
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
		if (!isSafePathSegment(serviceName) || !isSafePathSegment(versionNo)) {
			logger.error("commitService rejected: unsafe serviceName='{}' versionNo='{}'", serviceName, versionNo);
			return err(Zoker.eCommitFail);
		}
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
		try {
			switchCurrent(svcDir, versionNo);
		} catch (IOException ex) {
			// 现役未动；新版本目录已装好，重试同参数走"目标已存在"分支直接再切，收敛。
			logger.error("commitService switch current fail: services/{} version={}", serviceName, versionNo, ex);
			return err(Zoker.eCommitFail);
		}
		pruneVersions(svcDir, versionNo);
		return 0;
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
	 */
	void pruneVersions(File svcDir, String currentVersion) {
		var keep = keepVersions;
		if (keep <= 0)
			return; // 全保留
		var listFiles = svcDir.listFiles();
		if (null == listFiles)
			return;
		ArrayList<File> candidates = new ArrayList<>();
		for (var f : listFiles) {
			// 只把版本目录纳入清理面：current指针是文件天然排除；名字碰巧等于现役版本的目录不存在（构造上互斥）。
			if (f.isDirectory() && !f.getName().equals(currentVersion))
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
