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
	// close失败终态记忆（key=canonical文件名）：校验不符或close异常后条目已从files移除，
	// "未打开"与"关闭验证失败"不能共用"缺失即成功"这一状态——重复CloseFile必须返回原失败
	// 结果，不得把失败伪装成成功（修复前重试true，坏文件留在分发目录）。重新open同名文件
	// 即新上传代际、重新验证；closeAll（会话边界setPrepare/setIdle）清空。
	private final ConcurrentHashMap<String, Boolean> failedCloses = new ConcurrentHashMap<>();

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
		// 拒绝"../"逃逸和绝对路径，防止越界写/截断任意文件。
		checkFileNameInsideDir(hotManager.getDistributeDir(), fileName);
		// 重新open即新上传代际：清除上一代的关闭失败记忆，允许重传后重新验证。
		failedCloses.remove(relativeCanonicalFileName);
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
			try {
				fileBin.close();
			} catch (IOException e) {
				// close的I/O失败同样登记终态：条目已移除，重试不得走"未打开即成功"。
				failedCloses.put(relativeCanonicalFileName, Boolean.TRUE);
				throw e;
			}
			if (Arrays.compare(fileBin.md5Digest(), md5.bytesUnsafe()) != 0) {
				failedCloses.put(relativeCanonicalFileName, Boolean.TRUE);
				return false;
			}
			failedCloses.remove(relativeCanonicalFileName);
			return true;
		}
		// 未打开：从未打开过（断线重试/服务端重启后的确认）不算失败，维持true；
		// 本会话内曾关闭失败则返回原失败结果，重试不得升级成成功。
		return !failedCloses.containsKey(relativeCanonicalFileName);
	}

	public void commitDistribute() throws IOException {
		// 发布入口校验：存在关闭验证失败的文件时不得创建ready——失败被提交升级成
		// 成功会安装坏包；重传成功（失败记忆清除）后才允许提交。
		if (!failedCloses.isEmpty())
			throw new IOException("commit distribute rejected: close-verify failed file(s) remain, re-upload before commit: "
					+ failedCloses.keySet());
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
		failedCloses.clear(); // 会话边界：新会话的"未打开"不再背负上一代的失败记忆
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
