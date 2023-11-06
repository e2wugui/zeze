package Zeze.Services.ZokerImpl;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.file.Path;
import java.util.concurrent.ConcurrentHashMap;
import Zeze.Net.Binary;
import Zeze.Util.Task;
import com.amazonaws.services.dynamodbv2.xspec.B;

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

	public void append(String fileName, long offset, Binary data) throws IOException {
		var relativeCanonicalFileName = new File(fileName).getCanonicalFile().toString();
		var fileBin = files.get(relativeCanonicalFileName);
		fileBin.append(offset, data);
	}

	public void close(String fileName, Binary md5) throws IOException {
		var relativeCanonicalFileName = new File(fileName).getCanonicalFile().toString();
		var fileBin = files.remove(relativeCanonicalFileName);
		if (null != fileBin)
			fileBin.close(md5);
	}

	public static class FileBin {
		private String relativeCanonicalFileName;
		private File CanonicalFile;
		private RandomAccessFile randFile;
		private OutputStream os;

		public FileBin(String relativeCanonicalFileName, String baseDir, String path) {
			try {
				this.relativeCanonicalFileName = relativeCanonicalFileName;
				CanonicalFile = Path.of(baseDir, path).toFile().getCanonicalFile();
				randFile = new RandomAccessFile(CanonicalFile, "w");
				os = new BufferedOutputStream(new FileOutputStream(randFile.getFD()));
			} catch (IOException ex) {
				Task.forceThrow(ex);
			}
		}

		public String getRelativeCanonicalFileName() {
			return relativeCanonicalFileName;
		}

		public File getCanonicalFile() {
			return CanonicalFile;
		}

		public long getLength() throws IOException {
			return randFile.getChannel().size();
		}

		public void seek(long offset) throws IOException {
			randFile.seek(offset);
			os = new BufferedOutputStream(new FileOutputStream(randFile.getFD()));
		}

		public void append(long offset, Binary data) throws IOException {
			var length = randFile.getChannel().size();
			if (offset > length)
				throw new IOException("append out of range. " + offset + " " + length);
			if (offset < length)
				seek(offset);
			os.write(data.bytesUnsafe(), data.getOffset(), data.size());
		}

		public void close(Binary md5) throws IOException {
			// todo md5 verify
			os.close();
		}
	}
}
