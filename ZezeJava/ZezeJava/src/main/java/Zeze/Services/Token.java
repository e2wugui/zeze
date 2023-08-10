package Zeze.Services;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.SecureRandom;
import java.util.HashSet;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.LongAdder;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import Zeze.Builtin.Token.BGetTokenArg;
import Zeze.Builtin.Token.BGetTokenRes;
import Zeze.Builtin.Token.BNewTokenArg;
import Zeze.Builtin.Token.BNewTokenRes;
import Zeze.Builtin.Token.BPubTopic;
import Zeze.Builtin.Token.BTopic;
import Zeze.Builtin.Token.GetToken;
import Zeze.Builtin.Token.NewToken;
import Zeze.Builtin.Token.NotifyTopic;
import Zeze.Builtin.Token.PubTopic;
import Zeze.Builtin.Token.SubTopic;
import Zeze.Builtin.Token.TokenStatus;
import Zeze.Builtin.Token.UnsubTopic;
import Zeze.Config;
import Zeze.Net.Acceptor;
import Zeze.Net.AsyncSocket;
import Zeze.Net.Binary;
import Zeze.Net.Connector;
import Zeze.Net.ProtocolHandle;
import Zeze.Net.Rpc;
import Zeze.Net.Selectors;
import Zeze.Net.Service;
import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.DispatchMode;
import Zeze.Transaction.EmptyBean;
import Zeze.Transaction.Procedure;
import Zeze.Transaction.TransactionLevel;
import Zeze.Util.PerfCounter;
import Zeze.Util.ShutdownHook;
import Zeze.Util.Task;
import Zeze.Util.TaskCompletionSource;
import Zeze.Util.ThreadFactoryWithName;
import Zeze.Util.TimerFuture;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/*
TODO:
1. 避免token过多导致内存占用过高. 数据库化? 淘汰机制?
2. token续期服务. 跟初始设置的ttl如何兼顾? 覆盖还是选最大值?
*/
public final class Token extends AbstractToken {
	private static final Logger logger = LogManager.getLogger(Token.class);
	private static final int DEFAULT_PORT = 5003;
	private static final int TOKEN_CHAR_USED = 62; // 10+26+26
	private static final byte[] tokenCharTable = new byte[TOKEN_CHAR_USED];

	static {
		int i = 0;
		for (int b = '0'; b <= '9'; b++)
			tokenCharTable[i++] = (byte)b;
		for (int b = 'A'; b <= 'Z'; b++)
			tokenCharTable[i++] = (byte)b;
		for (int b = 'a'; b <= 'z'; b++)
			tokenCharTable[i++] = (byte)b;
	}

	// 生成24个字符的Token字符串. 每个字符只会出现半角的数字和字母共62种. 24个半角字符的字符串正好对齐64位,内存利用率高.
	private @NotNull String genToken() {
		var tokenBytes = new byte[24];
		var v = System.currentTimeMillis() / 1000;
		for (int i = 0; i < 5; i++, v /= TOKEN_CHAR_USED)
			tokenBytes[4 - i] = tokenCharTable[(int)(v % TOKEN_CHAR_USED)]; // 前5字节用来存秒单位的时间戳,避免过期后生成重复token的风险,29年内不会重复
		var tmp16 = new byte[16];
		tokenRandom.nextBytes(tmp16); // 一次生成16字节的安全随机数,下面分成2个64位整数使用
		v = ByteBuffer.ToLong(tmp16, 0) & Long.MAX_VALUE;
		for (int i = 0; i < 10; i++, v /= TOKEN_CHAR_USED)
			tokenBytes[5 + i] = tokenCharTable[(int)(v % TOKEN_CHAR_USED)]; // 接下来10字节存第1个64位整数
		v = ByteBuffer.ToLong(tmp16, 8) & Long.MAX_VALUE;
		for (int i = 0; i < 9; i++, v /= TOKEN_CHAR_USED)
			tokenBytes[15 + i] = tokenCharTable[(int)(v % TOKEN_CHAR_USED)]; // 最后9字节存第2个64位整数
		return new String(tokenBytes, StandardCharsets.ISO_8859_1);
	}

	public static class TokenClient extends Service {
		private Connector connector;
		private final ConcurrentHashMap<String, Consumer<NotifyTopic>> notifyTopicHandlers = new ConcurrentHashMap<>();

		public TokenClient(@Nullable Config config) {
			super("TokenClient", config);
			AddFactoryHandle(NewToken.TypeId_, new Zeze.Net.Service.ProtocolFactoryHandle<>(NewToken::new, null,
					TransactionLevel.None, DispatchMode.Normal));
			AddFactoryHandle(GetToken.TypeId_, new Zeze.Net.Service.ProtocolFactoryHandle<>(GetToken::new, null,
					TransactionLevel.None, DispatchMode.Normal));
			AddFactoryHandle(TokenStatus.TypeId_, new Zeze.Net.Service.ProtocolFactoryHandle<>(TokenStatus::new, null,
					TransactionLevel.None, DispatchMode.Normal));
			AddFactoryHandle(SubTopic.TypeId_, new Zeze.Net.Service.ProtocolFactoryHandle<>(SubTopic::new, null,
					TransactionLevel.None, DispatchMode.Normal));
			AddFactoryHandle(UnsubTopic.TypeId_, new Zeze.Net.Service.ProtocolFactoryHandle<>(UnsubTopic::new, null,
					TransactionLevel.None, DispatchMode.Normal));
			AddFactoryHandle(PubTopic.TypeId_, new Zeze.Net.Service.ProtocolFactoryHandle<>(PubTopic::new, null,
					TransactionLevel.None, DispatchMode.Normal));
			AddFactoryHandle(NotifyTopic.TypeId_, new Zeze.Net.Service.ProtocolFactoryHandle<>(NotifyTopic::new,
					this::ProcessNotifyTopic, TransactionLevel.None, DispatchMode.Normal));
		}

		public boolean registerNotifyTopicHandler(@NotNull String topic, @Nullable Consumer<NotifyTopic> handler) {
			return handler != null
					? notifyTopicHandlers.put(topic, handler) == null
					: notifyTopicHandlers.remove(topic) != null;
		}

		@Override
		public synchronized void start() throws Exception {
			if (connector != null)
				stop();
			var cfg = getConfig();
			int n = cfg.connectorCount();
			if (n != 1)
				throw new IllegalStateException("connectorCount = " + n + " != 1");
			cfg.forEachConnector(c -> this.connector = c);
			super.start();
		}

		public synchronized @NotNull TokenClient start(@NotNull String host, int port) {
			if (connector != null)
				stop();
			connector = new Connector(host, port, true);
			connector.SetService(this);
			connector.setAutoReconnect(true);
			connector.start();
			return this;
		}

		@Override
		public synchronized void stop() {
			if (connector != null) {
				connector.stop();
				connector = null;
			}
		}

		public void waitReady() {
			connector.WaitReady();
		}

		public @Nullable AsyncSocket getSocket() {
			return connector.getSocket();
		}

		public @NotNull TaskCompletionSource<BNewTokenRes.Data> newToken(@Nullable Binary context, long ttl) {
			return new NewToken(new BNewTokenArg.Data(context, ttl)).SendForWait(connector.getSocket());
		}

		public boolean newToken(@Nullable Binary context, long ttl,
								@NotNull ProtocolHandle<Rpc<BNewTokenArg.Data, BNewTokenRes.Data>> handler) {
			return new NewToken(new BNewTokenArg.Data(context, ttl)).Send(connector.getSocket(), handler);
		}

		public @NotNull TaskCompletionSource<BGetTokenRes.Data> getToken(@NotNull String token, long maxCount) {
			return new GetToken(new BGetTokenArg.Data(token, maxCount)).SendForWait(connector.getSocket());
		}

		public boolean getToken(@NotNull String token, long maxCount,
								@NotNull ProtocolHandle<Rpc<BGetTokenArg.Data, BGetTokenRes.Data>> handler) {
			return new GetToken(new BGetTokenArg.Data(token, maxCount)).Send(connector.getSocket(), handler);
		}

		public @NotNull TaskCompletionSource<EmptyBean.Data> subTopic(@NotNull String topic) {
			return new SubTopic(new BTopic.Data(topic)).SendForWait(connector.getSocket());
		}

		public boolean subTopic(@NotNull String topic,
								@NotNull ProtocolHandle<Rpc<BTopic.Data, EmptyBean.Data>> handler) {
			return new SubTopic(new BTopic.Data(topic)).Send(connector.getSocket(), handler);
		}

		public @NotNull TaskCompletionSource<EmptyBean.Data> unsubTopic(@NotNull String topic) {
			return new UnsubTopic(new BTopic.Data(topic)).SendForWait(connector.getSocket());
		}

		public boolean unsubTopic(@NotNull String topic,
								  @NotNull ProtocolHandle<Rpc<BTopic.Data, EmptyBean.Data>> handler) {
			return new UnsubTopic(new BTopic.Data(topic)).Send(connector.getSocket(), handler);
		}

		public @NotNull TaskCompletionSource<EmptyBean.Data> pubTopic(@NotNull String topic, @Nullable Binary content,
																	  boolean broadcast) {
			return new PubTopic(new BPubTopic.Data(topic, content, broadcast)).SendForWait(connector.getSocket());
		}

		public boolean pubTopic(@NotNull String topic, @Nullable Binary content, boolean broadcast,
								@NotNull ProtocolHandle<Rpc<BPubTopic.Data, EmptyBean.Data>> handler) {
			return new PubTopic(new BPubTopic.Data(topic, content, broadcast)).Send(connector.getSocket(), handler);
		}

		protected long ProcessNotifyTopic(NotifyTopic p) {
			var handler = notifyTopicHandlers.get(p.Argument.getTopic());
			if (handler == null)
				return Procedure.NotImplement;
			handler.accept(p);
			return Procedure.Success;
		}
	}

	private static final class TokenServer extends Service {
		private static final class Session {
			final @NotNull AsyncSocket so;
			final ReentrantLock lock = new ReentrantLock();
			final HashSet<String> subTopics = new HashSet<>(); // 该session已订阅的主题

			Session(@NotNull AsyncSocket so) {
				this.so = so;
			}
		}

		private final ConcurrentHashMap<String, CopyOnWriteArrayList<Session>> topicMap = new ConcurrentHashMap<>(); // key:topic

		TokenServer(Config config) {
			super("TokenServer", config);
		}

		boolean subTopic(@NotNull Session session, @NotNull String topic) {
			session.lock.lock();
			try {
				if (!session.subTopics.add(topic))
					return false;
				topicMap.computeIfAbsent(topic, __ -> new CopyOnWriteArrayList<>()).add(session);
			} finally {
				session.lock.unlock();
			}
			return true;
		}

		boolean unsubTopic(@NotNull Session session, @NotNull String topic) {
			session.lock.lock();
			try {
				if (!session.subTopics.remove(topic))
					return false;
				var sessions = topicMap.get(topic);
				if (sessions != null && sessions.remove(session) && sessions.isEmpty())
					topicMap.computeIfPresent(topic, (__, ss) -> ss.isEmpty() ? null : ss);
			} finally {
				session.lock.unlock();
			}
			return true;
		}

		void unsubAllTopics(@NotNull Session session) {
			session.lock.lock();
			try {
				for (var topic : session.subTopics) {
					var sessions = topicMap.get(topic);
					if (sessions != null && sessions.remove(session) && sessions.isEmpty())
						topicMap.computeIfPresent(topic, (__, ss) -> ss.isEmpty() ? null : ss);
				}
			} finally {
				session.lock.unlock();
			}
		}

		@Override
		public void OnHandshakeDone(@NotNull AsyncSocket so) throws Exception {
			so.setUserState(new Session(so));
		}

		@Override
		public void OnSocketClose(@NotNull AsyncSocket so, @Nullable Throwable e) throws Exception {
			super.OnSocketClose(so, e);
			var session = (Session)so.getUserState();
			if (session != null) {
				so.setUserState(null);
				unsubAllTopics(session);
			}
		}

		@Override
		public void dispatchProtocol(long typeId, ByteBuffer bb, ProtocolFactoryHandle<?> factoryHandle, AsyncSocket so)
				throws Exception {
			try {
				decodeProtocol(typeId, bb, factoryHandle, so).handle(this, factoryHandle); // 所有协议处理几乎无阻塞,可放心直接跑在IO线程上
			} catch (Throwable e) { // logger.error
				logger.error("dispatchProtocol exception:", e);
			}
		}
	}

	private static final class TokenState {
		final ReentrantLock lock = new ReentrantLock();
		final @Nullable InetSocketAddress remoteAddr;
		final @NotNull Binary context; // 绑定的上下文
		final long createTime; // 创建时间戳(毫秒)
		final long endTime; // 失效时间戳(毫秒)
		long count; // 已访问次数

		TokenState(@Nullable SocketAddress remoteAddr, @NotNull Binary context, long ttl) {
			this.remoteAddr = remoteAddr instanceof InetSocketAddress ? (InetSocketAddress)remoteAddr : null;
			this.context = context;
			this.createTime = System.currentTimeMillis();
			this.endTime = createTime + ttl;
		}

		TokenState(@NotNull ByteBuffer bb) throws UnknownHostException {
			int v = bb.ReadByte();
			if (v == 0)
				remoteAddr = null;
			else if (v == 1) {
				var ip = bb.ReadBytes();
				int port = bb.ReadInt();
				remoteAddr = new InetSocketAddress(InetAddress.getByAddress(ip), port);
			} else
				throw new IllegalStateException("unknown TokenState version = " + v);
			context = bb.ReadBinary();
			createTime = bb.ReadLong();
			endTime = bb.ReadLong();
			count = bb.ReadLong();
		}

		void encode(@NotNull ByteBuffer bb) {
			bb.WriteByte(remoteAddr != null ? 1 : 0);
			if (remoteAddr != null) {
				bb.WriteBytes(remoteAddr.getAddress().getAddress()); // getHostName()不靠谱,只记IP地址吧
				bb.WriteInt(remoteAddr.getPort());
			}
			bb.WriteBinary(context);
			bb.WriteLong(createTime);
			bb.WriteLong(endTime);
			bb.WriteLong(count);
		}

		@NotNull String getRemoteAddr() {
			var addr = remoteAddr != null ? remoteAddr.getAddress().getHostAddress() : null;
			return addr != null ? addr : "";
		}
	}

	private final Random tokenRandom = new SecureRandom();
	private final ConcurrentHashMap<String, TokenState> tokenMap = new ConcurrentHashMap<>();
	private final LongAdder newCounter = new LongAdder(); // 分配计数
	private TokenServer service;
	private TimerFuture<?> cleanTokenMapFuture;

	// 参数host,port优先; 如果传null/<=0则以conf为准; 如果conf也没配置则用默认值null/DEFAULT_PORT
	public synchronized Token start(@Nullable Config conf, @Nullable String host, int port) throws Exception {
		if (service != null)
			return this;

		PerfCounter.instance.tryStartScheduledLog();

		service = new TokenServer(conf != null ? conf : new Config().loadAndParse());
		RegisterProtocols(service);
		var sc = service.getConfig();
		if (sc.acceptorCount() == 0)
			sc.addAcceptor(new Acceptor(port > 0 ? port : DEFAULT_PORT, host));
		else {
			sc.forEachAcceptor2(acceptor -> {
				if (host != null)
					acceptor.setIp(host);
				if (port > 0)
					acceptor.setPort(port);
				return false;
			});
		}
		service.start();

		cleanTokenMapFuture = Task.scheduleUnsafe(1000, 1000, this::cleanTokenMap);
		return this;
	}

	public synchronized void stop() throws Exception {
		if (service != null) {
			cleanTokenMapFuture.cancel(true);
			tokenMap.clear();
			service.stop();
			service = null;
		}
	}

	private void cleanTokenMap() {
		var time = System.currentTimeMillis();
		for (var e : tokenMap.entrySet()) {
			var state = e.getValue();
			if (time >= state.endTime)
				tokenMap.remove(e.getKey(), state);
		}
	}

	public boolean tryLoadLatestSaveFile() {
		var maxTime = 0L;
		File maxFile = null;
		var files = new File(".").listFiles();
		if (files != null) {
			for (var file : files) {
				if (file.isFile()) {
					var fn = file.getName();
					if (fn.startsWith("token_") && fn.endsWith(".save")) {
						try {
							var time = Long.parseLong(fn.substring(6, fn.length() - 5));
							if (maxTime < time) {
								maxTime = time;
								maxFile = file;
							}
						} catch (Exception ignored) {
						}
					}
				}
			}
		}
		if (maxFile == null) {
			logger.info("tryLoadLatestSaveFile: not found any");
			return false;
		}
		try {
			decode(ByteBuffer.Wrap(Files.readAllBytes(maxFile.toPath())));
			logger.info("tryLoadLatestSaveFile('{}') OK ({} tokens)", maxFile.getAbsolutePath(), tokenMap.size());
			return true;
		} catch (Exception e) {
			logger.error("tryLoadLatestSaveFile('{}') failed ({} tokens loaded):",
					maxFile.getAbsolutePath(), tokenMap.size(), e);
			return false;
		}
	}

	public boolean trySaveFile() {
		var fn = "token_" + System.currentTimeMillis() + ".save";
		try (var fos = new FileOutputStream(fn)) {
			var bb = ByteBuffer.Allocate(1024);
			encode(bb, fos);
			fos.write(bb.Bytes, bb.ReadIndex, bb.size());
		} catch (Exception e) {
			logger.error("trySaveFile('{}') failed:", fn, e);
			return false;
		}
		logger.info("trySaveFile('{}') OK", fn);
		return true;
	}

	private void encode(@NotNull ByteBuffer bb, @NotNull OutputStream os) throws IOException {
		bb.WriteByte(1); // version
		bb.WriteLong(newCounter.sum());
		for (var e : tokenMap.entrySet()) {
			bb.WriteByte(1); // a new token record
			bb.WriteString(e.getKey());
			e.getValue().encode(bb);
			if (bb.size() >= 65536) { // 写到一定量就输出到os里,避免bb积累太多数据
				os.write(bb.Bytes, bb.ReadIndex, bb.size());
				bb.Reset();
			}
		}
		bb.WriteByte(0); // end of token records
	}

	private void decode(@NotNull ByteBuffer bb) throws UnknownHostException {
		tokenMap.clear();
		int v = bb.ReadByte();
		if (v != 1)
			throw new IllegalStateException("unknown Token version = " + v);
		newCounter.reset();
		newCounter.add(bb.ReadLong());
		while (bb.ReadByte() == 1) {
			var k = bb.ReadString();
			tokenMap.put(k, new TokenState(bb));
		}
	}

	@Override
	protected long ProcessNewTokenRequest(Zeze.Builtin.Token.NewToken r) {
		var arg = r.Argument;
		var ttl = arg.getTtl();
		if (ttl <= 0) {
			r.SendResultCode(-1);
			return Procedure.Success;
		}
		var remoteAddr = r.getSender().getRemoteAddress();
		String token;
		do {
			token = genToken();
		} while (tokenMap.putIfAbsent(token, new TokenState(remoteAddr, arg.getContext(), ttl)) != null);
		newCounter.increment();
		r.Result.setToken(token);
		r.SendResultCode(0);
		return Procedure.Success;
	}

	@Override
	protected long ProcessGetTokenRequest(Zeze.Builtin.Token.GetToken r) {
		var arg = r.Argument;
		var res = r.Result;
		var token = arg.getToken();
		var state = tokenMap.get(token);
		if (state == null) {
			res.setTime(-1);
			r.SendResultCode(0);
			return Procedure.Success;
		}
		res.setAddr(state.getRemoteAddr());
		var maxCount = arg.getMaxCount();
		state.lock.lock();
		try {
			var time = System.currentTimeMillis();
			if (time >= state.endTime) {
				tokenMap.remove(token, state);
				res.setTime(-2);
			} else {
				var count = state.count + 1;
				if (maxCount > 0 && count >= maxCount && !tokenMap.remove(token, state))
					res.setTime(-3);
				else {
					state.count = count;
					res.setContext(state.context);
					res.setCount(count);
					res.setTime(time - state.createTime);
				}
			}
		} finally {
			state.lock.unlock();
		}
		r.SendResultCode(0);
		return Procedure.Success;
	}

	@Override
	protected long ProcessTokenStatusRequest(TokenStatus r) {
		var res = r.Result;
		res.setNewCount(newCounter.sum());
		res.setCurCount(tokenMap.size());
		var s = service;
		res.setConnectCount(s != null ? s.getSocketCount() : -1);
		res.setPerfLog(PerfCounter.instance.getLastLog());
		r.SendResultCode(0);
		return Procedure.Success;
	}

	@Override
	protected long ProcessSubTopicRequest(SubTopic r) {
		var session = ((TokenServer.Session)r.getSender().getUserState());
		if (session == null) {
			r.SendResultCode(-1);
			return Procedure.Success;
		}
		var service = r.getService();
		if (!(service instanceof TokenServer)) {
			r.SendResultCode(-2);
			return Procedure.Success;
		}
		r.SendResultCode(((TokenServer)service).subTopic(session, r.Argument.getTopic()) ? 0 : 1);
		return Procedure.Success;
	}

	@Override
	protected long ProcessUnsubTopicRequest(UnsubTopic r) {
		var session = ((TokenServer.Session)r.getSender().getUserState());
		if (session == null) {
			r.SendResultCode(-1);
			return Procedure.Success;
		}
		var service = r.getService();
		if (!(service instanceof TokenServer)) {
			r.SendResultCode(-2);
			return Procedure.Success;
		}
		r.SendResultCode(((TokenServer)service).unsubTopic(session, r.Argument.getTopic()) ? 0 : 1);
		return Procedure.Success;
	}

	private static final boolean canLogNotifyTopic = AsyncSocket.ENABLE_PROTOCOL_LOG
			&& AsyncSocket.canLogProtocol(NotifyTopic.TypeId_);

	@Override
	protected long ProcessPubTopicRequest(PubTopic r) {
		var service = r.getService();
		if (!(service instanceof TokenServer)) {
			r.SendResultCode(-1);
			return Procedure.Success;
		}
		int n = 0;
		var arg = r.Argument;
		var sessions = ((TokenServer)service).topicMap.get(arg.getTopic());
		if (sessions != null) {
			var sb = canLogNotifyTopic ? new StringBuilder() : null;
			var p = new NotifyTopic(arg);
			var encoded = p.encode();
			if (arg.isBroadcast()) {
				for (var session : sessions) {
					var so = session.so;
					if (so.Send(encoded)) {
						n++;
						if (canLogNotifyTopic)
							sb.append(so.getSessionId()).append(',');
					}
				}
				if (canLogNotifyTopic && sb.length() > 0) {
					sb.setLength(sb.length() - 1);
					AsyncSocket.log("SEND", sb.toString(), p);
				}
			} else {
				int count = sessions.size();
				int i = count > 1 ? ThreadLocalRandom.current().nextInt(count) : 0;
				long sessionId = 0;
				for (int tryCount = count; tryCount > 0; tryCount--) {
					try {
						var so = sessions.get(i).so;
						if (so.Send(encoded)) {
							n = 1;
							sessionId = so.getSessionId();
							break;
						}
					} catch (IndexOutOfBoundsException ignored) { // 小概率事件
					}
					count = sessions.size();
					if (count <= 0)
						break;
					if (++i >= count)
						i = 0;
				}
				if (canLogNotifyTopic && n > 0)
					AsyncSocket.log("SEND", sessionId, p);
			}
		}
		r.SendResultCode(n);
		return Procedure.Success;
	}

	public static void main(String[] args) throws Exception {
		Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
			//noinspection CallToPrintStackTrace
			e.printStackTrace();
			logger.error("uncaught exception in {}:", t, e);
		});

		String host = null;
		int port = 0;
		int threadCount = 0;

		for (int i = 0; i < args.length; ++i) {
			switch (args[i]) {
			case "-host":
				host = args[++i];
				if (host.isBlank())
					host = null;
				break;
			case "-port":
				port = Integer.parseInt(args[++i]);
				break;
			case "-threads":
				threadCount = Integer.parseInt(args[++i]);
				break;
			default:
				throw new IllegalArgumentException("unknown argument: " + args[i]);
			}
		}

		if (threadCount < 1)
			threadCount = Runtime.getRuntime().availableProcessors();
		Task.initThreadPool(Task.newCriticalThreadPool("ZezeTaskPool"),
				Executors.newSingleThreadScheduledExecutor(new ThreadFactoryWithName("ZezeScheduledPool")));
		if (Selectors.getInstance().getCount() < threadCount)
			Selectors.getInstance().add(threadCount - Selectors.getInstance().getCount());

		var token = new Token();
		token.tryLoadLatestSaveFile();
		ShutdownHook.add(token::trySaveFile);

		token.start(null, host, port);
		synchronized (Thread.currentThread()) {
			Thread.currentThread().wait();
		}
	}
}
