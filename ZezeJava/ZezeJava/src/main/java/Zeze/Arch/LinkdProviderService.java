package Zeze.Arch;

import java.io.FileOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.concurrent.ConcurrentHashMap;
import Zeze.Application;
import Zeze.Builtin.Provider.AnnounceLinkInfo;
import Zeze.Builtin.Provider.Bind;
import Zeze.Builtin.Provider.Subscribe;
import Zeze.Net.AsyncSocket;
import Zeze.Net.Protocol;
import Zeze.Net.ProtocolDispatch;
import Zeze.Net.ProtocolHandle;
import Zeze.Serialize.ByteBuffer;
import Zeze.Services.HandshakeServer;
import Zeze.Util.Task;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.NonNull;

public class LinkdProviderService extends HandshakeServer {
	private static final Logger logger = LogManager.getLogger(LinkdProviderService.class);
	private static final String dumpFilename = System.getProperty("dumpProviderInput");
	private static final boolean enableDump = dumpFilename != null;

	protected LinkdApp linkdApp;
	protected final ConcurrentHashMap<String, ProviderSession> providerSessions = new ConcurrentHashMap<>();
	protected FileOutputStream dumpFile;
	protected AsyncSocket dumpSocket;
	// 私有锁对象: 避免暴露this监视器,Service实例被外部广泛共享,synchronized(this)会与外部同步块互相干扰
	private final Object dumpLock = new Object();
	private boolean dumpClosed;

	public LinkdProviderService(String name, Application zeze) {
		super(name, zeze);
		setNoProcedure(true);
	}

	// dumpLock: 懒初始化存在check-then-act竞争,且多IO线程并发写需要串行化(仅调试属性开启时生效)
	protected void tryDump(AsyncSocket s, ByteBuffer input) throws IOException {
		synchronized (dumpLock) {
			if (dumpClosed)
				return;
			if (dumpFile == null) {
				dumpFile = new FileOutputStream(dumpFilename);
				dumpSocket = s;
			}
			if (dumpSocket == s)
				dumpFile.write(input.Bytes, input.ReadIndex, input.size());
		}
	}

	@Override
	public void stop() throws Exception {
		super.stop();
		// dumpClosed: 关闭后不再重建,重建的FileOutputStream会截断已dump的文件
		synchronized (dumpLock) {
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
		}
	}

	@Override
	public boolean OnSocketProcessInputBuffer(@NotNull AsyncSocket s, @NotNull ByteBuffer input) throws Exception {
		if (enableDump)
			tryDump(s, input);
		return super.OnSocketProcessInputBuffer(s, input);
	}

	// 重载需要的方法。
	@Override
	public void dispatchProtocol(long typeId, @NonNull ByteBuffer bb, @NonNull ProtocolFactoryHandle<?> factoryHandle, AsyncSocket so) throws Exception {
		// 不支持事务。
		var p = decodeProtocol(typeId, bb, factoryHandle, so);
		p.dispatch(this, factoryHandle);
	}

	@Override
	public void dispatchProtocol(@NotNull Protocol<?> p, @NotNull ProtocolFactoryHandle<?> factoryHandle) throws Exception {
		if (p.getTypeId() == Bind.TypeId_ || p.getTypeId() == Subscribe.TypeId_) {
			// Bind 的处理需要同步等待ServiceManager的订阅成功，时间比较长，
			// 不要直接在io-thread里面执行。
			ProtocolDispatch.ofFunc(() -> p.handle(this, factoryHandle), p).dispatchMode(factoryHandle.Mode).runNow();
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
	public <P extends Protocol<?>> void dispatchRpcResponse(@NotNull P rpc, @NotNull ProtocolHandle<P> responseHandle,
															@NotNull ProtocolFactoryHandle<?> factoryHandle) throws Exception {
		// 不支持事务
		ProtocolDispatch.ofFunc(() -> responseHandle.handle(rpc), rpc).call();
	}

	@SuppressWarnings("MethodMayBeStatic")
	public LinkdProviderSession newSession(AsyncSocket so) {
		return new LinkdProviderSession(so.getSessionId());
	}

	@Override
	public void OnSocketAccept(@NonNull AsyncSocket so) throws Exception {
		so.setUserState(newSession(so));
		super.OnSocketAccept(so);
	}

	@Override
	public void OnHandshakeDone(@NonNull AsyncSocket so) throws Exception {
		super.OnHandshakeDone(so);

		var announce = new AnnounceLinkInfo();
		so.Send(announce);
	}

	@Override
	public void OnSocketClose(@NonNull AsyncSocket so, Throwable e) throws Exception {
		// 先unbind。这样避免有时间窗口。
		linkdApp.linkdProvider.onProviderClose(so);
		super.OnSocketClose(so, e);
	}

	@Override
	public void onServerSocketBind(@NonNull ServerSocket ss) {
		linkdApp.providerPort = ss.getLocalPort();
	}
}
