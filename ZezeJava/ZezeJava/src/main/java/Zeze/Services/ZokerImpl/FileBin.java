package Zeze.Services.ZokerImpl;

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
import Zeze.Net.Binary;
import Zeze.Util.Task;

/**
 * 单个分发文件在 Zoker 侧的传输载体：RandomAccessFile 断点续传，边写边维护全量 md5 供 CloseFile 校验。
 */
public class FileBin {
	private final String relativeCanonicalFileName;
	private final File canonicalFile;
	private final RandomAccessFile randFile;
	private OutputStream os;
	private MessageDigest md5;

	public FileBin(String relativeCanonicalFileName, File baseDir, String path) {
		this.relativeCanonicalFileName = relativeCanonicalFileName;
		RandomAccessFile openRandFile = null;
		try {
			// 先确保文件存在（含创建父目录）再计算md5：md5CurrentData对不存在的文件必抛
			// FileNotFoundException，全新文件的首次OpenFile会直接失败；而嵌套路径
			// （如server/lib/x.jar）的父目录RandomAccessFile("rw")也不会创建。
			canonicalFile = new File(baseDir, path).getCanonicalFile();
			var parent = canonicalFile.getParentFile();
			if (parent != null)
				//noinspection ResultOfMethodCallIgnored
				parent.mkdirs();
			openRandFile = new RandomAccessFile(canonicalFile, "rw");
			md5CurrentData();
			os = new BufferedOutputStream(new FileOutputStream(openRandFile.getFD()));
		} catch (Exception ex) {
			// 构造失败必须关闭已打开的句柄：泄漏的RandomAccessFile在Windows上锁住
			// distributes下的文件，后续commit的renameTo对该文件恒失败（直到GC/进程重启），
			// 调用方每次重试再泄一个。
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

	public long getLength() throws IOException {
		return randFile.getChannel().size();
	}

	public void truncate(long offset) throws IOException {
		randFile.seek(offset);
		randFile.setLength(offset);
		os = new BufferedOutputStream(new FileOutputStream(randFile.getFD()));
	}

	private void md5CurrentData() throws IOException, NoSuchAlgorithmException {
		md5 = MessageDigest.getInstance("MD5");
		var buffer = new byte[16 * 1024];
		try (var input = new BufferedInputStream(new FileInputStream(canonicalFile))) {
			var len = 0;
			while ((len = input.read(buffer)) >= 0) {
				md5.update(buffer, 0, len);
			}
		}
	}

	public void append(long offset, Binary data) throws IOException, NoSuchAlgorithmException {
		// 先flush缓冲数据再读channel长度：BufferedOutputStream对len<8192的写只进缓冲，
		// 不flush时channel.size()滞后，后续小块append会误判offset越界，或truncate重建os丢弃未落盘的缓冲。
		os.flush();
		var length = randFile.getChannel().size();
		if (offset > length)
			throw new IOException("append out of range. " + offset + " " + length);
		if (offset < length) {
			truncate(offset);
			length = randFile.getChannel().size(); // truncate will change length
			md5CurrentData();
		} else if (offset == length)
			// os挂在共享FD的当前指针上写入：新建FileBin指针停留在0，等值续传（断点续传的常态）不seek会从文件头覆写
			randFile.seek(offset);
		var newLength = offset + data.size();
		if (newLength > length) {
			var newDataLength = (int)(newLength - length);
			// 只hash新增部分：是data的尾部newDataLength字节（data前面length-offset字节属于已存在的旧数据）。
			md5.update(data.bytesUnsafe(), data.getOffset() + (int)(length - offset), newDataLength);
		}
		os.write(data.bytesUnsafe(), data.getOffset(), data.size());
		// 写后也flush：append返回即数据在FD上，getLength()/md5()/后续append读到的长度恒一致
		//（仅flush在读长一侧的话，最后一次小块append仍滞留缓冲，调用方立刻读文件会看到旧长度）。
		os.flush();
	}

	public byte[] md5Digest() {
		return md5.digest();
	}

	public void close() throws IOException {
		os.close(); // 关闭前flush缓冲数据
		randFile.close();
	}
}
