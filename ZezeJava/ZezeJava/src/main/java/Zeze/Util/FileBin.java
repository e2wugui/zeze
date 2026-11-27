package Zeze.Util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.function.UnaryOperator;
import Zeze.Net.Binary;

/**
 * 单个分发文件的传输载体（供 Zoker DistributeManager 使用）：基于 RandomAccessFile 支持断点续传，
 * 边写边维护全量 MD5 供校验。所有状态读写方法以实例锁串行化（叶子锁），支持多连接安全共享。
 */
public class FileBin {
	private final String relativeCanonicalFileName;
	private final File canonicalFile;
	private final RandomAccessFile randFile;
	private final OutputStream os;
	private final MessageDigest md5;

	public FileBin(String relativeCanonicalFileName, File baseDir, String path) {
		this(relativeCanonicalFileName, baseDir, path, BufferedOutputStream::new);
	}

	FileBin(String relativeCanonicalFileName, File baseDir, String path, UnaryOperator<OutputStream> outputWrapper) {
		this.relativeCanonicalFileName = relativeCanonicalFileName;
		RandomAccessFile openRandFile = null;
		try {
			// 先确保父目录与文件存在再计算初始 MD5
			canonicalFile = new File(baseDir, path).getCanonicalFile();
			var parent = canonicalFile.getParentFile();
			if (parent != null)
				//noinspection ResultOfMethodCallIgnored
				parent.mkdirs();
			openRandFile = new RandomAccessFile(canonicalFile, "rw");
			md5 = MessageDigest.getInstance("MD5");
			md5CurrentData();
			os = outputWrapper.apply(new FileOutputStream(openRandFile.getFD()));
		} catch (Exception ex) {
			// 构造失败立即关闭句柄，避免 Windows 下长期锁住文件导致后续 rename 失败
			if (null != openRandFile) {
				try {
					openRandFile.close();
				} catch (IOException closeEx) {
					ex.addSuppressed(closeEx);
				}
			}
			throw Task.forceThrow(ex);
		}
		randFile = openRandFile;
	}

	public String getRelativeCanonicalFileName() {
		return relativeCanonicalFileName;
	}

	public File getCanonicalFile() {
		return canonicalFile;
	}

	public synchronized long getLength() throws IOException {
		return randFile.getChannel().size();
	}

	/** 调整文件长度并同步摘要；后续 append/CloseFile 校验必须以截断后的内容为基准。 */
	public synchronized void truncate(long offset) throws IOException {
		os.flush();
		randFile.seek(offset);
		randFile.setLength(offset);
		md5CurrentData();
	}

	private void md5CurrentData() throws IOException {
		md5.reset();
		var buffer = new byte[16 * 1024];
		try (var input = new BufferedInputStream(new FileInputStream(canonicalFile))) {
			var len = 0;
			while ((len = input.read(buffer)) >= 0) {
				md5.update(buffer, 0, len);
			}
		}
	}

	public synchronized void append(long offset, Binary data) throws IOException, NoSuchAlgorithmException {
		// 先 flush 缓冲再读 channel 长度，避免小块写入滞留缓冲导致 size 滞后
		os.flush();
		var length = randFile.getChannel().size();
		if (offset > length)
			throw new IOException("append out of range. " + offset + " " + length);
		if (offset < length) {
			truncate(offset);
			length = randFile.getChannel().size();
		} else {
			randFile.seek(offset);
		}
		var newLength = offset + data.size();
		os.write(data.bytesUnsafe(), data.getOffset(), data.size());
		// 写后立即 flush，确保 getLength/md5/后续 append 看到一致长度
		os.flush();
		if (newLength > length) {
			var newDataLength = (int)(newLength - length);
			// 写盘与 flush 成功后再更新增量 MD5
			md5.update(data.bytesUnsafe(), data.getOffset() + (int)(length - offset), newDataLength);
		}
	}

	/** 惰性收口：digest() 终结化共享 digest，必须与 append 的 update 序化（在 close 之后调用也安全）。 */
	public synchronized byte[] md5Digest() {
		return md5.digest();
	}

	public synchronized void close() throws IOException {
		// os.close() 失败时仍须关闭 randFile，避免 Windows 句柄泄漏
		IOException primary = null;
		try {
			os.close();
		} catch (IOException ex) {
			primary = ex;
		}
		try {
			randFile.close();
		} catch (IOException ex) {
			if (null == primary)
				primary = ex;
			else
				primary.addSuppressed(ex);
		}
		if (null != primary)
			throw primary;
	}
}
