package Zeze.Hot;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import Zeze.Net.Binary;
import Zeze.Services.ZokerImpl.FileBin;

/**
 * 管理发布文件，支持命令行直接发布并最终提交。
 */
public class DistributeManager {
	private static final Logger logger = LogManager.getLogger(DistributeManager.class);

	private final HotManager hotManager;
	private final ConcurrentHashMap<String, FileBin> files = new ConcurrentHashMap<>();

	public DistributeManager(HotManager hot) {
		this.hotManager = hot;
	}

	public HotManager getHotManager() {
		return hotManager;
	}

	public FileBin open(String fileName) throws IOException {
		var file = new File(fileName);
		var relativeCanonicalFileName = file.getCanonicalFile().toString();
		// fileName 直接来自网络rpc（OpenFile请求），必须限制在 distributeDir 之内，
		// 拒绝"../"逃逸和绝对路径，防止越界写/截断任意文件（FND-G1-3）。
		checkFileNameInsideDir(hotManager.getDistributeDir(), fileName);
		return files.computeIfAbsent(relativeCanonicalFileName,
				(key) -> new FileBin(key, new File(hotManager.getDistributeDir()), file.getPath()));
	}

	/**
	 * 校验发布文件名规范化（normalize）后仍位于 distributeDir 之内。
	 * 越界（含"../"逃逸与绝对路径）时抛出 IOException 拒绝。
	 */
	static Path checkFileNameInsideDir(String distributeDir, String fileName) throws IOException {
		var baseDir = Path.of(distributeDir).toAbsolutePath().normalize();
		var target = baseDir.resolve(fileName).normalize();
		if (!target.startsWith(baseDir)) {
			logger.error("open file rejected: fileName='{}' escape distributeDir='{}'", fileName, distributeDir);
			throw new IOException("open file rejected, escape distributeDir: " + fileName);
		}
		return target;
	}

	public void append(String fileName, long offset, Binary data)
			throws IOException, NoSuchAlgorithmException {
		var file = new File(fileName);
		var relativeCanonicalFileName = file.getCanonicalFile().toString();
		var fileBin = files.get(relativeCanonicalFileName);
		if (null == fileBin)
			throw new IOException("file not opened: " + fileName);
		fileBin.append(offset, data);
	}

	public boolean closeAndVerify(String fileName, Binary md5) throws IOException {
		var file = new File(fileName);
		var relativeCanonicalFileName = file.getCanonicalFile().toString();
		var fileBin = files.remove(relativeCanonicalFileName);
		if (fileBin != null) {
			fileBin.close();
			var md5Local = fileBin.md5Digest();
			return Arrays.compare(md5Local, md5.bytesUnsafe()) == 0;
		}
		return true;
	}

	public void commitDistribute() throws IOException {
		var ready = Path.of(hotManager.getDistributeDir(), "ready");
		Files.createFile(ready);
	}

	/**
	 * 关闭并清除全部打开的 FileBin。
	 * 发布会话边界调用（setPrepare 新会话开始 / setIdle 会话结束）：
	 * 控制台在 OpenFile 之后、CloseFile 之前崩溃/断链时，残留的 RandomAccessFile
	 * 没有任何超时清理路径——句柄常驻泄漏，Windows 上还锁住 distributes 下的文件，
	 * 使 renameDistributes/安装的 rename 失败。发布不能并发，会话边界回收是安全的。
	 */
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
}
