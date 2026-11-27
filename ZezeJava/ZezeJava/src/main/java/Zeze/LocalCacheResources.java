package Zeze;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystemException;
import java.nio.file.AccessDeniedException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.DosFileAttributes;
import java.util.concurrent.TimeUnit;
import Zeze.Transaction.DatabaseRocksDb;
import Zeze.Util.FileMutex;
import org.jspecify.annotations.Nullable;

/**
 * 本地缓存库及其目录锁的唯一持有者。open/closeForStop 由 Application 的原有锁串行化。
 * 缓存库不加入 Application.getDatabases()，仅供表的本地镜像与 Checkpoint 使用。
 * open 失败保留已取得的资源，交由 stop 清理；database 非空不表示 open 已完成。
 */
final class LocalCacheResources {
	private static final boolean WINDOWS = File.separatorChar == '\\';
	private static final long DELETE_RETRY_NANOS = TimeUnit.SECONDS.toNanos(10);

	// 本地缓存库及目录删除的 I/O 操作边界，保持 FileMutex 互斥语义。
	static class Operations {
		DatabaseRocksDb createDatabase(Application application, Config.DatabaseConf conf) {
			return new DatabaseRocksDb(application, conf);
		}

		void deleteCacheDirectory(File directory) throws IOException, InterruptedException {
			LocalCacheResources.deleteDirectory(directory);
		}
	}

	private final Operations operations;
	private @Nullable FileMutex directoryMutex;
	private @Nullable DatabaseRocksDb database;

	LocalCacheResources() {
		this(new Operations());
	}

	LocalCacheResources(Operations operations) {
		this.operations = operations;
	}

	@Nullable DatabaseRocksDb database() {
		return database;
	}

	void open(Application application, int serverId) throws IOException, InterruptedException {
		var conf = new Config.DatabaseConf();
		conf.setName("zeze_cache_" + serverId);
		conf.setDatabaseUrl(conf.getName());
		// 锁覆盖两次目录删除及中间的整个使用期；锁失败不得触碰他人的目录。
		directoryMutex = FileMutex.acquire(conf.getDatabaseUrl() + ".lock",
				"zeze_cache dir (serverId=" + application.getConfig().getServerId() + ")");
		operations.deleteCacheDirectory(new File(conf.getDatabaseUrl()));
		conf.setDatabaseType(Config.DbType.RocksDb);
		database = operations.createDatabase(application, conf);
		// Database.open 会通过 Application getter 回访自己，必须先发布引用。
		database.open(application);
	}

	void closeForStop() {
		if (database != null) {
			var db = database;
			var directory = db.getDatabaseUrl();
			Application.stopStep("LocalRocksCacheDb.close", () -> {
				db.close();
				// close 失败时跳过删除；getter 在关闭和删除期间仍返回原引用。
				operations.deleteCacheDirectory(new File(directory));
			});
			database = null;
		}
		// 仅取得锁、尚未取得数据库的失败路径只释放锁，不再次删除目录。
		if (directoryMutex != null) {
			directoryMutex.close();
			directoryMutex = null;
		}
	}

	/** 不跟随符号链接或Windows目录联接；不存在视为已删除，遍历失败保留原始文件系统异常。 */
	static void deleteDirectory(File directory) throws IOException, InterruptedException {
		long deadline = System.nanoTime() + DELETE_RETRY_NANOS;
		try {
			Files.walkFileTree(directory.toPath(), new SimpleFileVisitor<>() {
				@Override
				public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
					if (isDirectoryLink(attrs)) {
						deletePath(dir, deadline);
						return FileVisitResult.SKIP_SUBTREE;
					}
					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
					deletePath(file, deadline);
					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult postVisitDirectory(Path dir, @Nullable IOException error) throws IOException {
					if (error != null)
						throw error;
					deletePath(dir, deadline);
					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult visitFileFailed(Path file, IOException error) throws IOException {
					// 失效或不可访问的目录联接在打开目标时会触发遍历失败；先检查条目自身再将 NoSuchFileException 视为不存在。
					if (WINDOWS) {
						BasicFileAttributes attrs = null;
						try {
							attrs = Files.readAttributes(file, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
						} catch (IOException ignored) {
							// 无法检查条目时保留遍历器的原始异常。
						}
						if (attrs != null && isDirectoryLink(attrs)) {
							deletePath(file, deadline);
							return FileVisitResult.CONTINUE;
						}
					}
					if (error instanceof NoSuchFileException)
						return FileVisitResult.CONTINUE;
					throw error;
				}
			});
		} catch (DeleteInterruptedException e) {
			throw e.interrupted;
		}
	}

	private static boolean isDirectoryLink(BasicFileAttributes attrs) {
		// Windows 目录联接同时标记为 directory 和 other，而非 symbolic link。
		return WINDOWS && attrs.isDirectory() && attrs.isOther();
	}

	// Windows 共享句柄或 delete-pending 文件可能暂时拒绝删除，整棵目录树共用重试期限退避重试。
	private static void deletePath(Path path, long deadline) throws IOException {
		for (long delayMillis = 10; ; delayMillis = Math.min(delayMillis * 2, 100)) {
			try {
				Files.deleteIfExists(path);
				return;
			} catch (FileSystemException e) {
				long remaining = deadline - System.nanoTime();
				if (!WINDOWS || remaining <= 0 || e instanceof AccessDeniedException && isReadOnlyFile(path))
					throw e;
				try {
					TimeUnit.NANOSECONDS.sleep(Math.min(TimeUnit.MILLISECONDS.toNanos(delayMillis), remaining));
				} catch (InterruptedException interrupted) {
					throw new DeleteInterruptedException(interrupted);
				}
			}
		}
	}

	private static boolean isReadOnlyFile(Path path) {
		try {
			var attrs = Files.readAttributes(path, DosFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
			return attrs.isRegularFile() && attrs.isReadOnly();
		} catch (IOException ignored) {
			return false; // 属性读取失败不覆盖原始删除异常，仍交给有界重试。
		}
	}

	// FileVisitor 只能抛 IOException；在边界还原中断，保持 open/stop 的中断处理契约。
	private static final class DeleteInterruptedException extends IOException {
		final InterruptedException interrupted;

		DeleteInterruptedException(InterruptedException interrupted) {
			super(interrupted);
			this.interrupted = interrupted;
		}
	}
}
