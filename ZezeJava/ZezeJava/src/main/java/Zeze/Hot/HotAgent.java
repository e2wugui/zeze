package Zeze.Hot;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.MessageDigest;
import Zeze.Builtin.HotDistribute.Commit;
import Zeze.Builtin.HotDistribute.Commit2;
import Zeze.Builtin.HotDistribute.PrepareDistribute;
import Zeze.Builtin.HotDistribute.TryDistribute;
import Zeze.Builtin.HotDistribute.TryRollback;
import Zeze.Builtin.Zoker.AppendFile;
import Zeze.Builtin.Zoker.CloseFile;
import Zeze.Builtin.Zoker.OpenFile;
import Zeze.Config;
import Zeze.IModule;
import Zeze.Net.Binary;
import Zeze.Net.Connector;
import Zeze.Net.Service;
import Zeze.Util.OutObject;

public class HotAgent extends AbstractHotAgent {
	public static class HotAgentService extends Service {
		public HotAgentService() {
			super("Zeze.HotAgent", (Config)null);
		}
	}

	private final HotAgentService hotAgentService;
	private final String peer;
	private final Connector connector;

	public HotAgent(String ipPort) {
		var ipPorts = ipPort.split("[_:]");
		var ip = ipPorts[0];
		var port = Integer.parseInt(ipPorts[1]);
		this.peer = ipPort;

		hotAgentService = new HotAgentService();
		RegisterProtocols(hotAgentService);

		var out = new OutObject<Connector>();
		hotAgentService.getConfig().tryGetOrAddConnector(ip, port, true, out);
		connector = out.value;
	}

	public String getPeer() {
		return peer;
	}

	public HotAgent(String ip, int port) {
		hotAgentService = new HotAgentService();
		RegisterProtocols(hotAgentService);
		this.peer = ip + "_" + port;

		var out = new OutObject<Connector>();
		hotAgentService.getConfig().tryGetOrAddConnector(ip, port, true, out);
		connector = out.value;
	}

	public void start() {
		connector.start();
		connector.WaitReady();
	}

	public void stop() {
		connector.stop();
	}

	public HotAgentService getHotAgentService() {
		return hotAgentService;
	}

	public long openFile(String fileName) {
		var hotManager = connector.TryGetReadySocket();
		var r = new OpenFile();
		r.Argument.setFileName(fileName);
		r.SendForWait(hotManager).await();
		if (r.getResultCode() != 0)
			throw new RuntimeException("open file error. " + IModule.getErrorCode(r.getResultCode()));

		// 更多返回结果，修改 BOpenFileResult 并修改返回值。
		return r.Result.getOffset();
	}

	public void appendFile(String fileName, long offset, byte[] data) {
		appendFile(fileName, offset, data, 0, data.length);
	}

	public void appendFile(String fileName, long offset, byte[] data, int dataOffset, int dataLength) {
		var hotManager = connector.TryGetReadySocket();
		var r = new AppendFile();
		r.Argument.setFileName(fileName);
		r.Argument.setOffset(offset);
		r.Argument.setChunk(new Binary(data, dataOffset, dataLength));
		r.SendForWait(hotManager).await();
		if (r.getResultCode() != 0)
			throw new RuntimeException("append file error. " + IModule.getErrorCode(r.getResultCode()));
		// 需要返回结果，修改 BAppendFileResult
	}

	public void closeFile(String fileName, Binary md5) {
		var hotManager = connector.TryGetReadySocket();
		var r = new CloseFile();
		r.Argument.setFileName(fileName);
		r.Argument.setMd5(md5);
		r.SendForWait(hotManager).await();
		if (r.getResultCode() != 0)
			throw new RuntimeException("close file error. " + IModule.getErrorCode(r.getResultCode()));
	}

	public void prepareDistribute(long id) {
		var hotManager = connector.TryGetReadySocket();
		var r = new PrepareDistribute();
		r.Argument.setDistributeId(id);
		r.SendForWait(hotManager).await();
		if (r.getResultCode() != 0)
			throw new RuntimeException("prepareDistribute error " + IModule.getErrorCode(r.getResultCode()));
	}

	public TryDistribute tryDistribute(long id, boolean atomicAll) {
		var hotManager = connector.TryGetReadySocket();
		var r = new TryDistribute();
		r.Argument.setDistributeId(id);
		r.Argument.setAtomicAll(atomicAll);
		r.SendForWait(hotManager);
		return r;
	}

	public Commit commit(long id) {
		var hotManager = connector.TryGetReadySocket();
		var r = new Commit();
		r.Argument.setDistributeId(id);
		r.SendForWait(hotManager);
		return r;
	}

	public void commit2(long id) {
		var hotManager = connector.TryGetReadySocket();
		var r = new Commit2();
		r.Argument.setDistributeId(id);
		r.SendForWait(hotManager).await();
		if (r.getResultCode() != 0)
			throw new RuntimeException("commit2=" + IModule.getErrorCode(r.getResultCode()));
	}

	public void tryRollback(long id) {
		var hotManager = connector.TryGetReadySocket();
		var r = new TryRollback();
		r.Argument.setDistributeId(id);
		r.SendForWait(hotManager).await();
		if (r.getResultCode() != 0) {
			throw new RuntimeException("tryRollback=" + IModule.getErrorCode(r.getResultCode()));
		}
	}

	public void distribute(File distributeDir) throws Exception {
		var files = distributeDir.listFiles();
		if (null == files)
			return;

		for (var file : files) {
			if (file.isDirectory()) {
				//distributeFiles(distributeDir);
				continue;
			}
			//if (!file.getName().endsWith(".jar"))
			//	continue;
			var fileRelativeName = distributeDir.toPath().relativize(file.toPath()).toString().replace("\\", "/");
			var md5 = MessageDigest.getInstance("MD5");
			var fileOffset = openFile(fileRelativeName);
			try (var bis = new BufferedInputStream(new FileInputStream(file))) {
				md5To(md5, bis, fileOffset);
				var buffer = new byte[16 * 1024];
				var rc = 0;
				while ((rc = bis.read(buffer)) >= 0) {
					md5.update(buffer, 0, rc);
					appendFile(fileRelativeName, fileOffset, buffer, 0, rc);
					fileOffset += rc;
				}
			} finally {
				closeFile(fileRelativeName, new Binary(md5.digest()));
			}
		}
	}

	private static void md5To(MessageDigest md5, BufferedInputStream bis, long toOffset) throws IOException {
		var buffer = new byte[16 * 1024];
		while (toOffset > 0) {
			var tryReadLen = (int)(toOffset > buffer.length ? buffer.length : toOffset);
			var rc = bis.read(buffer, 0, tryReadLen);
			if (rc >= 0) {
				toOffset -= rc;
				md5.update(buffer, 0, rc);
				continue;
			}
			throw new RuntimeException("md5To not enough data");
		}
	}

}
