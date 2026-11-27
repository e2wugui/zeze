package Zeze.Arch;

import java.util.Objects;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import Zeze.Application;
import Zeze.Builtin.Provider.AnnounceLinkInfo;
import Zeze.Builtin.Provider.Bind;
import Zeze.Builtin.Provider.Subscribe;
import Zeze.Net.AsyncSocket;
import Zeze.Net.Protocol;
import Zeze.Util.TaskSpec;
import Zeze.Net.ProtocolHandle;
import Zeze.Serialize.ByteBuffer;
import Zeze.Services.HandshakeServer;
import Zeze.Util.Task;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.Nullable;

/**
 * Linkd 上接受 Provider 连接的服务：处理 Provider 握手宣告与 Bind/Subscribe 协议派发。
 */
public class LinkdProviderService extends HandshakeServer {
	private static final Logger logger = LogManager.getLogger(LinkdProviderService.class);
	private static final @Nullable String dumpFilename = System.getProperty("dumpProviderInput");
	private static final boolean enableDump = dumpFilename != null;

	protected @Nullable LinkdApp linkdApp;

	protected final LinkdApp requireLinkdApp() {
		return Objects.requireNonNull(linkdApp, "linkd service is not attached to an application");
	}
	protected final ConcurrentHashMap<String, ProviderSession> providerSessions = new ConcurrentHashMap<>();
	protected @Nullable FileOutputStream dumpFile;
	protected @Nullable AsyncSocket dumpSocket;
	// 保护 dumpFile 懒初始化与并发写，使用 ReentrantLock 避免锁内文件 IO 阻塞虚拟线程载体。
	private final ReentrantLock dumpLock = new ReentrantLock();
	private boolean dumpClosed;

	public LinkdProviderService(String name, @Nullable Application zeze) {
		super(name, zeze);
		setNoProcedure(true);
	}

	protected void tryDump(AsyncSocket s, ByteBuffer input) throws IOException {
		dumpLock.lock();
		try {
			if (dumpClosed)
				return;
			if (dumpFile == null) {
				dumpFile = new FileOutputStream(Objects.requireNonNull(dumpFilename));
				dumpSocket = s;
			}
			if (dumpSocket == s)
				dumpFile.write(input.Bytes, input.ReadIndex, input.size());
		} finally {
			dumpLock.unlock();
		}
	}

	@Override
	public void stop() throws Exception {
		super.stop();
		// 标记关闭后不再重建，防止截断已写出的 dump 文件。
		dumpLock.lock();
		try {
			dumpClosed = true;
			dumpSocket = null;
			if (dumpFile != null) {
				try {
					dumpFile.close();
				} catch (IOException e) {
					logger.error("LinkdProviderService close dumpFile failed", e);
				}
				dumpFile = null;
			}
		} finally {
			dumpLock.unlock();
		}
	}

	@Override
	public boolean OnSocketProcessInputBuffer(AsyncSocket s, ByteBuffer input) throws Exception {
		if (enableDump)
			tryDump(s, input);
		return super.OnSocketProcessInputBuffer(s, input);
	}

	@Override
	public void dispatchProtocol(long typeId, ByteBuffer bb, ProtocolFactoryHandle<?> factoryHandle, @Nullable AsyncSocket so) throws Exception {
		// 不支持事务。
		var p = decodeProtocol(typeId, bb, factoryHandle, so);
		p.dispatch(this, factoryHandle);
	}

	@Override
	public void dispatchProtocol(Protocol<?> p, ProtocolFactoryHandle<?> factoryHandle) throws Exception {
		if (p.getTypeId() == Bind.TypeId_ || p.getTypeId() == Subscribe.TypeId_) {
			// Bind 的处理需要同步等待ServiceManager的订阅成功，时间比较长，
			// 不要直接在io-thread里面执行。
			TaskSpec.ofFunc(() -> p.handle(this, factoryHandle), p).dispatchMode(factoryHandle.Mode).runNow();
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

	@Override
	public <P extends Protocol<?>> void dispatchRpcResponse(P rpc, ProtocolHandle<P> responseHandle,
															ProtocolFactoryHandle<?> factoryHandle) throws Exception {
		// 不支持事务；runNow 内部捕获异常记日志，避免抛回 IO 线程断开 provider 连接。
		TaskSpec.ofFunc(() -> responseHandle.handle(rpc), rpc).dispatchMode(factoryHandle.Mode).runNow();
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
	public void OnSocketClose(AsyncSocket so, @Nullable Throwable e) throws Exception {
		// 先unbind。这样避免有时间窗口。
		requireLinkdApp().linkdProvider.onProviderClose(so);
		super.OnSocketClose(so, e);
	}

	@Override
	public void onServerSocketBind(ServerSocket ss) {
		requireLinkdApp().providerPort = ss.getLocalPort();
	}
}
