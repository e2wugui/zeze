package Zeze.Util;


import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.PosixFileAttributes;

/**
 * 单文件原子替换：写temp→fsync→原子rename→目录fsync（POSIX尽力）。
 * 同一目标的并发写各自原子、终态其一；顺序和跨进程互斥由调用方保证。
 * 文件提交句柄的outputStream仅提供写入和刷新能力，
 * 关闭流不提交也不关闭底层channel；commit显式执行fsync+原子rename换版生效。
 * close/abort丢弃未提交内容；进程死亡留下的.tmp由启动清扫。句柄单线程使用。
 */
public final class AtomicOutputFile implements AutoCloseable {
	/** 便捷重载：全量内容一次换版（小文件用），语义与Stream版一致。 */
	public static void writeAtomically(Path target, byte[] content) throws IOException {
		writeAtomically(target, out -> out.write(content));
	}

	/**
	 * 作用域式原子写：writer正常返回后提交；writer抛出任何Throwable则丢弃temp、保留旧目标。
	 * writer收到的流close只刷新，可安全交给try-with-resources中的包装流；
	 * 包装流必须在writer返回前完成收尾，换版由外层作用域显式提交。
	 */
	public static void writeAtomically(Path target, Stream writer) throws IOException {
		try (var file = new AtomicOutputFile(target)) {
			writer.write(file.outputStream());
			file.commit();
		}
	}

	@FunctionalInterface
	public interface Stream {
		void write(OutputStream out) throws IOException;
	}

	private final Path target;
	private final Path temp;
	private final FileChannel channel;
	private final OutputStream buffered;
	private boolean finished;
	private boolean committed;
	// 即使调用方吞掉写入或刷新异常，commit也不得发布残缺内容。
	private boolean writeFailed;
	private final OutputStream output = new OutputStream() {
		@Override
		public void write(int b) throws IOException {
			ensureOpen();
			try {
				buffered.write(b);
			} catch (Throwable t) {
				writeFailed = true;
				throw t;
			}
		}

		@Override
		public void write(byte[] b, int off, int len) throws IOException {
			ensureOpen();
			try {
				buffered.write(b, off, len); // 保留批量写入，不逐字节转发。
			} catch (Throwable t) {
				writeFailed = true;
				throw t;
			}
		}

		@Override
		public void flush() throws IOException {
			ensureOpen();
			try {
				buffered.flush();
			} catch (Throwable t) {
				writeFailed = true;
				throw t;
			}
		}

		@Override
		public void close() throws IOException {
			flush(); // 包装流可安全关闭；底层资源和提交权限仍由句柄持有。
		}
	};

	/**
	 * 创建文件提交句柄，目标父目录必须已存在。
	 * 内容及包装流收尾完成后显式commit；close/abort丢弃未提交内容。
	 * 适合跨方法管理的长生命周期写入；作用域内写入优先用{@link #writeAtomically}。
	 */
	public AtomicOutputFile(Path target) throws IOException {
		this.target = target.toAbsolutePath().normalize();
		// temp必须与target同目录：rename不跨文件系统才可能原子。
		this.temp = Files.createTempFile(this.target.getParent(),
				this.target.getFileName() + ".", ".tmp");
		try {
			this.channel = FileChannel.open(temp,
					StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
		} catch (IOException e) {
			// createTempFile成功后open失败也必须清理，避免周期性失败累积tmp。
			deleteTempBestEffort();
			throw e;
		}
		this.buffered = new BufferedOutputStream(Channels.newOutputStream(channel));
	}

	/** 返回同一写入视图；close仅刷新，句柄结束后写入和刷新均被拒绝。 */
	public OutputStream outputStream() {
		return output;
	}

	private void ensureOpen() throws IOException {
		if (finished)
			throw new IOException("AtomicOutputFile is closed: " + target);
	}

	/** 未提交时丢弃temp；提交或放弃后重复关闭为no-op。 */
	@Override
	public void close() {
		abort();
	}

	/** 丢弃temp，不算失败：这次换版没有发生，磁盘保持旧版。 */
	public void abort() {
		if (finished)
			return;
		finished = true;
		try {
			channel.close(); // 放弃内容，无需刷新缓冲；Windows上先关句柄再删temp。
		} catch (IOException e) {
			// 关闭失败不改变"作废"语义
		} finally {
			deleteTempBestEffort();
		}
	}

	/**
	 * 显式提交。成功后重复提交为no-op；放弃或提交失败后不得再提交。
	 * force必须先于move：新内容未落盘前名字不允许翻过去。失败时先关channel再清理temp。
	 */
	public void commit() throws IOException {
		if (committed)
			return;
		ensureOpen();
		finished = true;
		try (channel) {
			if (writeFailed)
				throw new IOException("AtomicOutputFile: write failed earlier, discard temp instead of publish: " + target);
			buffered.flush();
			channel.force(true);
			inheritPosixAttributesBestEffort(target, temp);
			try {
				Files.move(temp, target, StandardCopyOption.ATOMIC_MOVE);
			} catch (AtomicMoveNotSupportedException e) {
				Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
			}
		} catch (Throwable failure) {
			deleteTempBestEffort();
			throw failure;
		}
		fsyncDirBestEffort(target.getParent());
		committed = true;
	}

	private void deleteTempBestEffort() {
		try {
			Files.deleteIfExists(temp);
		} catch (IOException e) {
			// Windows句柄残留：留给启动清扫
		}
	}

	// temp继承既有目标的POSIX权限：createTempFile的0600会随rename落到目标。Windows跳过。
	private static void inheritPosixAttributesBestEffort(Path target, Path temp) {
		try {
			Files.setPosixFilePermissions(temp,
					Files.readAttributes(target, PosixFileAttributes.class).permissions());
		} catch (IOException | UnsupportedOperationException e) {
			// best effort
		}
	}

	// POSIX尽力；Windows打不开目录句柄，静默降级。
	private static void fsyncDirBestEffort(Path dir) {
		try (FileChannel dirChannel = FileChannel.open(dir, StandardOpenOption.READ)) {
			dirChannel.force(true);
		} catch (IOException | UnsupportedOperationException e) {
			// best effort
		}
	}
}
