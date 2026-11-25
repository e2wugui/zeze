package Zeze.Util;

import java.io.BufferedOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import Zeze.Net.Binary;
import harness.Fast;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

@Fast
public class TestFileBinTruncateDigest {
	@TempDir
	Path directory;

	@Test
	public void truncateThenAppendHashesActualContent() throws Exception {
		var bin = new FileBin("payload", directory.toFile(), "payload");
		try {
			bin.append(0, new Binary("abcdef".getBytes(StandardCharsets.UTF_8)));
			bin.truncate(2);
			bin.append(2, new Binary("XY".getBytes(StandardCharsets.UTF_8)));
		} finally {
			bin.close();
		}
		assertArrayEquals("abXY".getBytes(StandardCharsets.UTF_8), Files.readAllBytes(directory.resolve("payload")));
		assertArrayEquals(MessageDigest.getInstance("MD5").digest(Files.readAllBytes(directory.resolve("payload"))),
				bin.md5Digest());
	}

	@Test
	public void truncateAloneHashesRetainedPrefix() throws Exception {
		var bin = new FileBin("prefix", directory.toFile(), "prefix");
		try {
			bin.append(0, new Binary("abcdef".getBytes(StandardCharsets.UTF_8)));
			bin.truncate(3);
		} finally {
			bin.close();
		}
		assertEquals(3, Files.size(directory.resolve("prefix")));
		assertArrayEquals(MessageDigest.getInstance("MD5").digest("abc".getBytes(StandardCharsets.UTF_8)), bin.md5Digest());
	}

	@Test
	public void appendZeroByteWriteFailureDoesNotCorruptMd5OnRetry() throws Exception {
		var failNextWrite = new AtomicBoolean(false);
		var bin = new FileBin("retry-zero", directory.toFile(), "retry-zero",
				raw -> new BufferedOutputStream(new FilterOutputStream(raw) {
					@Override
					public void write(byte[] b, int off, int len) throws IOException {
						if (failNextWrite.compareAndSet(true, false))
							throw new IOException("simulated disk full (0 bytes written)");
						out.write(b, off, len);
					}
				}));
		byte[] prefix = "prefix-data".getBytes(StandardCharsets.UTF_8);
		byte[] chunk = new byte[16 * 1024];
		Arrays.fill(chunk, (byte)'Z');
		try {
			bin.append(0, new Binary(prefix));
			failNextWrite.set(true);
			assertThrows(IOException.class, () -> bin.append(prefix.length, new Binary(chunk)));
			assertEquals(prefix.length, bin.getLength());
			// 写盘失败后同 offset 重试：增量 MD5 不得重复 hash 失败那次的数据
			bin.append(prefix.length, new Binary(chunk));
		} finally {
			bin.close();
		}
		byte[] disk = Files.readAllBytes(directory.resolve("retry-zero"));
		byte[] expected = new byte[prefix.length + chunk.length];
		System.arraycopy(prefix, 0, expected, 0, prefix.length);
		System.arraycopy(chunk, 0, expected, prefix.length, chunk.length);
		assertArrayEquals(expected, disk);
		assertArrayEquals(MessageDigest.getInstance("MD5").digest(disk), bin.md5Digest());
	}

	@Test
	public void appendPartialWriteFailureRecoversMd5OnRetry() throws Exception {
		var failNextWrite = new AtomicBoolean(false);
		var bin = new FileBin("retry-partial", directory.toFile(), "retry-partial",
				raw -> new BufferedOutputStream(new FilterOutputStream(raw) {
					@Override
					public void write(byte[] b, int off, int len) throws IOException {
						if (failNextWrite.compareAndSet(true, false)) {
							out.write(b, off, len / 2);
							throw new IOException("simulated partial write failure");
						}
						out.write(b, off, len);
					}
				}));
		byte[] prefix = "prefix-data".getBytes(StandardCharsets.UTF_8);
		byte[] chunk = new byte[16 * 1024];
		Arrays.fill(chunk, (byte)'P');
		try {
			bin.append(0, new Binary(prefix));
			failNextWrite.set(true);
			assertThrows(IOException.class, () -> bin.append(prefix.length, new Binary(chunk)));
			assertEquals(prefix.length + chunk.length / 2, bin.getLength());
			// 部分写入后同 offset 重试走 truncate + md5 重算，最终内容与摘要仍须一致
			bin.append(prefix.length, new Binary(chunk));
		} finally {
			bin.close();
		}
		byte[] disk = Files.readAllBytes(directory.resolve("retry-partial"));
		byte[] expected = new byte[prefix.length + chunk.length];
		System.arraycopy(prefix, 0, expected, 0, prefix.length);
		System.arraycopy(chunk, 0, expected, prefix.length, chunk.length);
		assertArrayEquals(expected, disk);
		assertArrayEquals(MessageDigest.getInstance("MD5").digest(disk), bin.md5Digest());
	}

	@Test
	public void getLengthDoesNotObserveTransientTruncatedSizeDuringAppend() throws Exception {
		byte[] payload = new byte[4096];
		Arrays.fill(payload, (byte)'L');
		var bin = new FileBin("sync-length", directory.toFile(), "sync-length");
		var readerStarted = new CountDownLatch(1);
		var stop = new AtomicBoolean(false);
		var unexpectedLength = new AtomicLong(-1);
		var readerFailure = new AtomicReference<@Nullable Throwable>();
		try {
			bin.append(0, new Binary(payload));
			var reader = new Thread(() -> {
				readerStarted.countDown();
				try {
					while (!stop.get()) {
						long len = bin.getLength();
						if (len != payload.length) {
							unexpectedLength.compareAndSet(-1, len);
							break;
						}
					}
				} catch (Throwable t) {
					readerFailure.compareAndSet(null, t);
				}
			}, "filebin-length-reader");
			reader.setDaemon(true);
			reader.start();
			try {
				assertTrue(readerStarted.await(3, TimeUnit.SECONDS));
				for (int i = 0; i < 200 && unexpectedLength.get() == -1; i++)
					bin.append(0, new Binary(payload));
			} finally {
				stop.set(true);
				reader.join(5000);
				assertFalse(reader.isAlive());
			}
			assertNull(readerFailure.get());
			assertEquals(-1, unexpectedLength.get(), "getLength observed transient truncated length mid-append");
		} finally {
			bin.close();
		}
	}
}
