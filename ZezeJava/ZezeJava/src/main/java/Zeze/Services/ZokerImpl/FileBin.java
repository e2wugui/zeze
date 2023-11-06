package Zeze.Services.ZokerImpl;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import Zeze.Net.Binary;
import Zeze.Util.Task;

public class FileBin {
	private String relativeCanonicalFileName;
	private File canonicalFile;
	private RandomAccessFile randFile;
	private OutputStream os;
	private MessageDigest md5;

	public FileBin(String relativeCanonicalFileName, String baseDir, String path) {
		try {
			this.relativeCanonicalFileName = relativeCanonicalFileName;
			canonicalFile = Path.of(baseDir, path).toFile().getCanonicalFile();
			md5CurrentData();
			randFile = new RandomAccessFile(canonicalFile, "w");
			os = new BufferedOutputStream(new FileOutputStream(randFile.getFD()));
		} catch (Exception ex) {
			Task.forceThrow(ex);
		}
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
		var length = randFile.getChannel().size();
		if (offset > length)
			throw new IOException("append out of range. " + offset + " " + length);
		if (offset < length) {
			truncate(offset);
			length = randFile.getChannel().size(); // truncate will change length
			md5CurrentData();
		}
		var newLength = offset + data.size();
		if (newLength > length) {
			var newDataLength = (int)(newLength - length);
			md5.update(data.bytesUnsafe(), data.getOffset() + newDataLength, newDataLength);
		}
		os.write(data.bytesUnsafe(), data.getOffset(), data.size());
	}

	public byte[] md5Digest() {
		return md5.digest();
	}

	public void close() throws IOException {
		os.close();
	}
}
