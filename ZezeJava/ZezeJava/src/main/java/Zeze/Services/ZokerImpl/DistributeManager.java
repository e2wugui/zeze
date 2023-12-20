package Zeze.Services.ZokerImpl;

import java.io.File;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import Zeze.Builtin.Zoker.CommitService;
import Zeze.Net.Binary;
import Zeze.Services.Zoker;

/**
 * 管理文件
 */
public class DistributeManager {
	private final Zoker zoker;
	private final ConcurrentHashMap<String, FileBin> files = new ConcurrentHashMap<>();

	public DistributeManager(Zoker zoker) {
		this.zoker = zoker;
	}

	public Zoker getZoker() {
		return zoker;
	}

	public FileBin open(String serviceName, String fileName) throws IOException {
		var file = new File(serviceName, fileName);
		var relativeCanonicalFileName = file.getCanonicalFile().toString();
		return files.computeIfAbsent(relativeCanonicalFileName,
				(key) -> new FileBin(key, zoker.getDistributeDir(), file.getPath()));
	}

	public void append(String serviceName, String fileName, long offset, Binary data)
			throws IOException, NoSuchAlgorithmException {
		var file = new File(serviceName, fileName);
		var relativeCanonicalFileName = file.getCanonicalFile().toString();
		var fileBin = files.get(relativeCanonicalFileName);
		fileBin.append(offset, data);
	}

	public boolean closeAndVerify(String serviceName, String fileName, Binary md5) throws IOException {
		var file = new File(serviceName, fileName);
		var relativeCanonicalFileName = file.getCanonicalFile().toString();
		var fileBin = files.remove(relativeCanonicalFileName);
		if (null != fileBin) {
			fileBin.close();
			var md5Local = fileBin.md5Digest();
			return Arrays.compare(md5Local, md5.bytesUnsafe()) == 0;
		}
		return true;
	}

	public long commitService(CommitService r) {
		var serviceFrom = new File(zoker.getDistributeDir(), r.Argument.getServiceName());
		var serviceTo = new File(zoker.getServiceDir(), r.Argument.getServiceName());
		var serviceOld = new File(new File(zoker.getServiceOldDir(), r.Argument.getServiceName()), r.Argument.getVersionNo());
		if (serviceTo.exists()) {
			if (serviceOld.exists())
				return zoker.errorCode(Zoker.eServiceOldExists);
			if (!serviceTo.renameTo(serviceOld))
				return zoker.errorCode(Zoker.eMoveOldFail);
		}
		if (!serviceFrom.renameTo(serviceTo))
			return zoker.errorCode(Zoker.eCommitFail);
		r.SendResult();
		return 0;
	}
}
