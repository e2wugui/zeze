package Zeze.Services.ZokerImpl;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import Zeze.Builtin.Zoker.CommitService;
import Zeze.Net.AsyncSocket;
import Zeze.Net.Binary;
import Zeze.Services.Zoker;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * 管理文件
 */
public class DistributeManager {
	private static final Logger logger = LogManager.getLogger(DistributeManager.class);

	private final Zoker zoker;
	// 键为distributeDir内实体文件的canonical路径：键与实体位置一致（FileBin.getCanonicalFile()），
	// commitService按服务目录前缀回收、CWD无关。
	private final ConcurrentHashMap<String, FileBin> files = new ConcurrentHashMap<>();
	// 每个agent连接打开的文件键：agent在OpenFile之后、CloseFile之前断链时按连接回收FileBin，
	// 否则RandomAccessFile句柄常驻泄漏，Windows上还锁住distributes下的文件使commit的rename失败。
	private final ConcurrentHashMap<AsyncSocket, Set<String>> filesBySocket = new ConcurrentHashMap<>();

	public DistributeManager(Zoker zoker) {
		this.zoker = zoker;
	}

	public Zoker getZoker() {
		return zoker;
	}

	public FileBin open(String serviceName, String fileName, AsyncSocket sender) throws IOException {
		var path = new File(serviceName, fileName).getPath();
		// serviceName/fileName直接来自网络rpc（OpenFile请求），必须限制在distributeDir之内，
		// 拒绝"../"逃逸和绝对路径，防止越界写/截断任意文件。
		checkInsideDir(zoker.getDistributeDir(), path);
		var relativeCanonicalFileName = fileKey(path);
		var fileBin = files.computeIfAbsent(relativeCanonicalFileName,
				(key) -> new FileBin(key, zoker.getDistributeDir(), path));
		if (sender != null)
			filesBySocket.computeIfAbsent(sender, __ -> ConcurrentHashMap.newKeySet()).add(relativeCanonicalFileName);
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
		var fileBin = files.remove(relativeCanonicalFileName);
		if (sender != null) {
			var opened = filesBySocket.get(sender);
			if (opened != null) {
				opened.remove(relativeCanonicalFileName);
				if (opened.isEmpty())
					filesBySocket.remove(sender, opened);
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
		var opened = filesBySocket.remove(socket);
		if (opened == null)
			return;
		for (var key : opened) {
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
		return new File(zoker.getDistributeDir(), path).getCanonicalFile().toString();
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

	public long commitService(CommitService r) {
		var serviceName = r.Argument.getServiceName();
		var versionNo = r.Argument.getVersionNo();
		if (!isSafePathSegment(serviceName) || !isSafePathSegment(versionNo)) {
			logger.error("commitService rejected: unsafe serviceName='{}' versionNo='{}'", serviceName, versionNo);
			return zoker.errorCode(Zoker.eCommitFail);
		}
		var serviceFrom = new File(zoker.getDistributeDir(), serviceName);
		var serviceTo = new File(zoker.getServiceDir(), serviceName);
		var serviceOld = new File(new File(zoker.getServiceOldDir(), serviceName), versionNo);
		// 消费distributes/<svc>前先关闭其下仍打开的FileBin：正常流程CloseFile已收尾，
		// 这里兜底跳过CloseFile的连接，同时释放Windows上阻塞rename的文件句柄。
		closeUnder(serviceFrom);
		if (serviceTo.exists()) {
			if (serviceOld.exists())
				return zoker.errorCode(Zoker.eServiceOldExists);
			try {
				// renameTo不创建父目录：servicesOld/<serviceName>/这层从无人创建，缺失时renameTo必false
				Files.createDirectories(serviceOld.getParentFile().toPath());
			} catch (IOException ex) {
				logger.error("commitService createDirectories {}", serviceOld.getParent(), ex);
				return zoker.errorCode(Zoker.eMoveOldFail);
			}
			if (!serviceTo.renameTo(serviceOld)) {
				logger.error("commitService moveOld fail: {} -> {}", serviceTo, serviceOld);
				return zoker.errorCode(Zoker.eMoveOldFail);
			}
		}
		if (!serviceFrom.renameTo(serviceTo)) {
			logger.error("commitService fail: {} -> {}", serviceFrom, serviceTo);
			return zoker.errorCode(Zoker.eCommitFail);
		}
		r.SendResult();
		return 0;
	}

	private void closeUnder(File serviceDir) {
		String prefix;
		try {
			prefix = serviceDir.getCanonicalPath() + File.separator;
		} catch (IOException ex) {
			logger.error("closeUnder canonical {}", serviceDir, ex);
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
