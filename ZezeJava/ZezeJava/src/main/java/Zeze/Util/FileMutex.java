package Zeze.Util;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 跨进程文件互斥：对指定锁文件上 OS 排它锁（RandomAccessFile + tryLock），
 * 持有期间拒绝同 JVM 其他实例与跨进程获取者，{@link #close()} 释放。
 *
 * <p>典型用法：守护"启动即删除重建"的目录——锁文件放目录同级（放目录内会随
 * 目录被删），且释放后不删除（删除锁文件与后续获取者存在竞态）。同 JVM 撞号
 * 时 tryLock 抛 {@link OverlappingFileLockException}（FileLock 按 JVM 计），
 * 跨进程由 OS 文件锁拒绝（tryLock 返回 null），两者均转
 * {@link IllegalStateException} 即时 fail-fast；进程崩溃 OS 自动释放。
 *
 * <p>{@link #close()} 幂等；释放/关流异常只记日志不抛——调用点常处于停机
 * 路径，不得因锁释放失败中断后续清理步骤。
 */
public final class FileMutex implements AutoCloseable {
	private static final @NotNull Logger logger = LogManager.getLogger(FileMutex.class);

	private @Nullable RandomAccessFile lockFile;
	private @Nullable FileLock fileLock;

	/**
	 * 获取失败即抛 IllegalStateException（同 JVM 撞号 / 跨进程占用 / IO 异常三态，
	 * 消息含 ownerDesc 与锁文件绝对路径），成功返回持有句柄。
	 *
	 * @param ownerDesc 持有者描述，拼入失败消息供定位（如 "zeze_cache dir (serverId=N)"）
	 */
	public static @NotNull FileMutex acquire(@NotNull String lockFilePath, @NotNull String ownerDesc) {
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
