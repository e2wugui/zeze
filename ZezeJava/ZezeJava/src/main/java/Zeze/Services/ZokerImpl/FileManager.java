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
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import Zeze.Net.Binary;
import Zeze.Util.Task;

/**
 * 管理文件
 */
public class FileManager {
	public final String baseDir;
	public final ConcurrentHashMap<String, FileBin> files = new ConcurrentHashMap<>();

	public FileManager(String baseDir) {
		this.baseDir = baseDir;
	}

	public String getBaseDir() {
		return baseDir;
	}

	public FileBin open(String fileName) throws IOException {
		var relativeCanonicalFileName = new File(fileName).getCanonicalFile().toString();
		return files.computeIfAbsent(relativeCanonicalFileName, (key) -> new FileBin(key, baseDir, fileName));
	}

	public void append(String fileName, long offset, Binary data) throws IOException, NoSuchAlgorithmException {
		var relativeCanonicalFileName = new File(fileName).getCanonicalFile().toString();
		var fileBin = files.get(relativeCanonicalFileName);
		fileBin.append(offset, data);
	}

	public boolean closeAndVerify(String fileName, Binary md5) throws IOException {
		var relativeCanonicalFileName = new File(fileName).getCanonicalFile().toString();
		var fileBin = files.remove(relativeCanonicalFileName);
		if (null != fileBin) {
			fileBin.close();
			var md5Local = fileBin.md5Digest();
			return Arrays.compare(md5Local, md5.bytesUnsafe()) == 0;
		}
		return true;
	}

}
