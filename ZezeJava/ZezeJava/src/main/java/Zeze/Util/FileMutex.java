package Zeze.Util;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.Nullable;

/**
 * 跨进程文件互斥锁（RandomAccessFile + OS 排它 {@link FileLock}）：持有期间拒绝同 JVM 其他实例与跨进程获取者，
 * 冲突或 IO 异常均转为 {@link IllegalStateException}；进程退出后由 OS 自动释放。
 * 锁文件应放于被守护目录同级且释放后不删除；{@link #close()} 幂等且异常只记日志不抛出。
 */
public final class FileMutex implements AutoCloseable {
	private static final Logger logger = LogManager.getLogger(FileMutex.class);

	private @Nullable RandomAccessFile lockFile;
	private @Nullable FileLock fileLock;

	/**
	 * 获取文件排它锁；同 JVM 冲突、跨进程占用或 IO 异常均抛 {@link IllegalStateException}。
	 *
	 * @param ownerDesc 持有者描述，拼入异常消息便于定位
	 */
	public static FileMutex acquire(String lockFilePath, String ownerDesc) {
		var lockPath = new File(lockFilePath);
		var lock = new FileMutex();
		try {
			lock.lockFile = new RandomAccessFile(lockPath, "rw");
			lock.fileLock = lock.lockFile.getChannel().tryLock();
		} catch (OverlappingFileLockException e) {
			lock.closeLockFileQuietly();
			throw new IllegalStateException(ownerDesc + " lock held by another instance in this jvm: "
					+ lockPath.getAbsolutePath());
		} catch (IOException e) {
			lock.closeLockFileQuietly();
			throw new IllegalStateException("acquire " + lockPath.getAbsolutePath() + " failed: "
					+ ownerDesc, e);
		}
		//noinspection ConstantConditions
		if (lock.fileLock == null) {
			lock.closeLockFileQuietly();
			throw new IllegalStateException(ownerDesc + " lock held by another process: "
					+ lockPath.getAbsolutePath());
		}
		return lock;
	}

	@Override
	public void close() {
		try {
			if (fileLock != null) {
				fileLock.release();
				fileLock = null;
			}
		} catch (IOException e) {
			logger.error("release file mutex exception:", e);
		}
		closeLockFileQuietly();
	}

	private void closeLockFileQuietly() {
		try {
			if (lockFile != null) {
				lockFile.close();
				lockFile = null;
			}
		} catch (IOException e) {
			logger.error("close file mutex lock file exception:", e);
		}
	}
}
