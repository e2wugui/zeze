package Zeze.Arch;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.concurrent.ConcurrentHashMap;
import Zeze.Builtin.Provider.AnnounceLinkInfo;
import Zeze.Builtin.Provider.Bind;
import Zeze.Builtin.Provider.Subscribe;
import Zeze.Net.AsyncSocket;
import Zeze.Net.Protocol;
import Zeze.Net.ProtocolHandle;
import Zeze.Net.Service;
import Zeze.Serialize.ByteBuffer;
import Zeze.Util.Task;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class LinkdProviderService extends Zeze.Services.HandshakeServer {
	private static final Logger logger = LogManager.getLogger(LinkdProviderService.class);
	private static final String dumpFilename = System.getProperty("dumpProviderInput");
	private static final boolean enableDump = dumpFilename != null;

	protected LinkdApp linkdApp;
	protected final ConcurrentHashMap<String, ProviderSession> providerSessions = new ConcurrentHashMap<>();
	protected FileOutputStream dumpFile;
	protected AsyncSocket dumpSocket;

	public LinkdProviderService(String name, Zeze.Application zeze) {
		super(name, zeze);
	}

	protected void tryDump(AsyncSocket s, ByteBuffer input) throws IOException {
		if (dumpFile == null) {
			dumpFile = new FileOutputStream(dumpFilename);
			dumpSocket = s;
		}
		if (dumpSocket == s)
			dumpFile.write(input.Bytes, input.ReadIndex, input.size());
	}

	@Override
	public void OnSocketProcessInputBuffer(AsyncSocket s, ByteBuffer input) throws Exception {
		if (enableDump)
			tryDump(s, input);
		super.OnSocketProcessInputBuffer(s, input);
	}

	@Override
	public <P extends Protocol<?>> void DispatchRpcResponse(P rpc, ProtocolHandle<P> responseHandle,
															ProtocolFactoryHandle<?> factoryHandle) {
		Task.runRpcResponseUnsafe(() -> responseHandle.handle(rpc), rpc, factoryHandle.Mode);
	}

	// 重载需要的方法。
	@Override
	public void dispatchProtocol(long typeId, ByteBuffer bb, ProtocolFactoryHandle<?> factoryHandle, AsyncSocket so) {
		var p = decodeProtocol(typeId, bb, factoryHandle, so);
		if (p.getTypeId() == Bind.TypeId_ || p.getTypeId() == Subscribe.TypeId_) {
			// Bind 的处理需要同步等待ServiceManager的订阅成功，时间比较长，
			// 不要直接在io-thread里面执行。
			Task.runUnsafe(() -> p.handle(this, factoryHandle), p, null, null, factoryHandle.Mode);
		} else {
			// 不启用新的Task，直接在io-thread里面执行。因为其他协议都是立即处理的，
			// 直接执行，少一次线程切换。
			try {
				var isRequestSaved = p.isRequest();
				var result = p.handle(this, factoryHandle);
				Task.logAndStatistics(null, result, p, isRequestSaved);
			} catch (Exception ex) {
				logger.error("Protocol.Handle Exception: {}", p, ex);
			}
		}
	}

	@SuppressWarnings("MethodMayBeStatic")
	public LinkdProviderSession newSession(AsyncSocket so) {
		return new LinkdProviderSession(so.getSessionId());
	}

	@Override
	public void OnSocketAccept(AsyncSocket so) throws Exception {
		so.setUserState(newSession(so));
		super.OnSocketAccept(so);
	}

	@Override
	public void OnHandshakeDone(AsyncSocket so) throws Exception {
		super.OnHandshakeDone(so);

		var announce = new AnnounceLinkInfo();
		so.Send(announce);
	}

	@Override
	public void OnSocketClose(AsyncSocket so, Throwable e) throws Exception {
		// 先unbind。这样避免有时间窗口。
		linkdApp.linkdProvider.onProviderClose(so);
		super.OnSocketClose(so, e);
	}

	@Override
	public void onServerSocketBind(ServerSocket ss) {
		linkdApp.providerPort = ss.getLocalPort();
	}
}
