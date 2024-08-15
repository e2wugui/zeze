package Zeze.Hot;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import Zeze.Net.Binary;
import Zeze.Services.ZokerImpl.FileBin;

/**
 * 管理发布文件，支持命令行直接发布并最终提交。
 */
public class DistributeManager {
	private final HotManager hotManager;
	private final ConcurrentHashMap<String, FileBin> files = new ConcurrentHashMap<>();

	public DistributeManager(HotManager hot) {
		this.hotManager = hot;
	}

	public HotManager getHotManager() {
		return hotManager;
	}

	public FileBin open(String fileName) throws IOException {
		var file = new File(fileName);
		var relativeCanonicalFileName = file.getCanonicalFile().toString();
		return files.computeIfAbsent(relativeCanonicalFileName,
				(key) -> new FileBin(key, new File(hotManager.getDistributeDir()), file.getPath()));
	}

	public void append(String fileName, long offset, Binary data)
			throws IOException, NoSuchAlgorithmException {
		var file = new File(fileName);
		var relativeCanonicalFileName = file.getCanonicalFile().toString();
		var fileBin = files.get(relativeCanonicalFileName);
		fileBin.append(offset, data);
	}

	public boolean closeAndVerify(String fileName, Binary md5) throws IOException {
		var file = new File(fileName);
		var relativeCanonicalFileName = file.getCanonicalFile().toString();
		var fileBin = files.remove(relativeCanonicalFileName);
		if (fileBin != null) {
			fileBin.close();
			var md5Local = fileBin.md5Digest();
			return Arrays.compare(md5Local, md5.bytesUnsafe()) == 0;
		}
		return true;
	}

	public void commitDistribute() throws IOException {
		var ready = Path.of(hotManager.getDistributeDir(), "ready");
		Files.createFile(ready);
	}
}
