package Zeze.Util;

import org.jetbrains.annotations.NotNull;

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
import java.nio.file.attribute.PosixFileAttributeView;
import java.nio.file.attribute.PosixFileAttributes;

/**
 * AtomicFileWriter.openOutput返回的流式写句柄。三态生命周期：close=fsync+原子rename
 * 换版生效；abort=丢弃temp；都未调而进程死亡=留.tmp由启动清扫。close/abort幂等；
 * 句柄单线程使用。
 */
public final class AtomicOutputFile extends OutputStream {
	private final Path target;
	private final Path temp;
	private final FileChannel channel;
	private final OutputStream buffered;
	private boolean finished;

	AtomicOutputFile(@NotNull Path target) throws IOException {
		this.target = target.toAbsolutePath().normalize();
		// temp必须与target同目录：rename不跨文件系统才可能原子。
		this.temp = Files.createTempFile(this.target.getParent(),
				this.target.getFileName() + ".", ".tmp");
		this.channel = FileChannel.open(temp,
				StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
		this.buffered = new BufferedOutputStream(Channels.newOutputStream(channel));
	}

	@Override
	public void write(int b) throws IOException {
		buffered.write(b);
	}

	@Override
	public void write(byte[] b, int off, int len) throws IOException {
		buffered.write(b, off, len);
	}

	/** 丢弃temp，不算失败：这次换版没有发生，磁盘保持旧版。 */
	public void abort() {
		if (finished)
			return;
		finished = true;
		try {
			buffered.close();
		} catch (IOException e) {
			// 关闭失败不改变"作废"语义
		} finally {
			deleteTempBestEffort();
		}
	}

	/**
	 * force必须先于move：新内容未落盘前名字不允许翻过去——此顺序是安全前提，
	 * 改动须逐字评审（AGENTS.md I1规约）。任何失败（非崩溃：flush/force/move）立即
	 * 清理temp并关闭channel——周期性失败的调用点不得累积tmp与句柄；进程死亡残留的
	 * tmp才由启动清扫收口。
	 */
	@Override
	public void close() throws IOException {
		if (finished)
			return;
		finished = true;
		try {
			buffered.flush();
			channel.force(true);
			inheritPosixAttributesBestEffort(target, temp);
			try {
				Files.move(temp, target, StandardCopyOption.ATOMIC_MOVE);
			} catch (AtomicMoveNotSupportedException e) {
				Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
			}
		} catch (IOException failure) {
			deleteTempBestEffort();
			throw failure;
		} finally {
			channel.close();
		}
		fsyncDirBestEffort(target.getParent());
	}

	private void deleteTempBestEffort() {
		try {
			Files.deleteIfExists(temp);
		} catch (IOException e) {
			// Windows句柄残留：留给启动清扫
		}
	}

	// temp继承既有目标的POSIX权限：createTempFile的0600会随rename落到目标。Windows跳过。
	private static void inheritPosixAttributesBestEffort(@NotNull Path target, @NotNull Path temp) {
		try {
			if (null == Files.getFileAttributeView(target, PosixFileAttributeView.class))
				return;
			Files.setPosixFilePermissions(temp,
					Files.readAttributes(target, PosixFileAttributes.class).permissions());
		} catch (IOException | UnsupportedOperationException e) {
			// best effort
		}
	}

	// POSIX尽力；Windows打不开目录句柄，静默降级。
	private static void fsyncDirBestEffort(@NotNull Path dir) {
		try (FileChannel dirChannel = FileChannel.open(dir, StandardOpenOption.READ)) {
			dirChannel.force(true);
		} catch (IOException | UnsupportedOperationException e) {
			// best effort
		}
	}
}
