package Zeze.Services;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.HashSet;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.LongAdder;
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
import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.DispatchMode;
import Zeze.Transaction.EmptyBean;
import Zeze.Transaction.Procedure;
import Zeze.Transaction.TransactionLevel;
import Zeze.Util.ConcurrentHashSet;
import Zeze.Util.FastLock;
import Zeze.Util.OutObject;
import Zeze.Util.PropertiesHelper;
import Zeze.Util.RocksDatabase;
import Zeze.Util.ShutdownHook;
import Zeze.Util.Task;
import Zeze.Util.TaskCompletionSource;
import Zeze.Util.TaskSpec;
import Zeze.Util.ThreadFactoryWithName;
import Zeze.Util.TimerFuture;
import Zeze.Util.ZezeCounter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import static Zeze.Util.Args.requireInt;
import static Zeze.Util.Args.requireValue;

// token续期服务. 跟初始设置的ttl如何兼顾? 覆盖还是选最大值? 目前暂无计划, 等有需求再说
public final class Token extends AbstractToken {
	private static final @NotNull Logger logger = LogManager.getLogger(Token.class);
	private static final int DEFAULT_PORT = 5003;
	private static final int TOKEN_CHAR_USED = 62; // 10+26+26
	private static final byte[] tokenCharTable = new byte[TOKEN_CHAR_USED];
	private static final boolean canLogNotifyTopic = AsyncSocket.ENABLE_PROTOCOL_LOG
			&& AsyncSocket.canLogProtocol(NotifyTopic.TypeId_);
	private static final ZezeCounter.LongCounter tokenSoftRefCleanCounter
			= ZezeCounter.instance.allocCounter("TokenSoftRefClean");
	private static final ReferenceQueue<Object> refQueue = new ReferenceQueue<>();
	private static final FastLock tokenRefCleanerLock = new FastLock();
	private static @Nullable Thread tokenRefCleaner;

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
	public static @NotNull String genToken(@NotNull Random random) {
		var tokenBytes = new byte[24];
		var v = System.currentTimeMillis() / 1000;
		for (int i = 0; i < 5; i++, v /= TOKEN_CHAR_USED)
			tokenBytes[4 - i] = tokenCharTable[(int)(v % TOKEN_CHAR_USED)]; // 前5字节用来存秒单位的时间戳,避免过期后生成重复token的风险,29年内不会重复
		var tmp16 = new byte[16];
		random.nextBytes(tmp16); // 一次生成16字节的安全随机数,下面分成2个64位整数使用
		v = ByteBuffer.ToLong(tmp16, 0) & Long.MAX_VALUE;
		for (int i = 0; i < 10; i++, v /= TOKEN_CHAR_USED)
			tokenBytes[5 + i] = tokenCharTable[(int)(v % TOKEN_CHAR_USED)]; // 接下来10字节存第1个64位整数
		v = ByteBuffer.ToLong(tmp16, 8) & Long.MAX_VALUE;
		for (int i = 0; i < 9; i++, v /= TOKEN_CHAR_USED)
			tokenBytes[15 + i] = tokenCharTable[(int)(v % TOKEN_CHAR_USED)]; // 最后9字节存第2个64位整数
		return new String(tokenBytes, StandardCharsets.ISO_8859_1);
	}

	private @NotNull String genToken() {
		return genToken(tokenRandom);
	}

	public static class TokenClient extends HandshakeClient {
		private Connector connector;
		private final ConcurrentHashMap<String, Consumer<NotifyTopic>> notifyTopicHandlers = new ConcurrentHashMap<>();
		private final ConcurrentHashSet<String> subTopics = new ConcurrentHashSet<>();

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

			var opt = getConfig().getHandshakeOptions();
			if (opt.getKeepCheckPeriod() == 0)
				opt.setKeepCheckPeriod(5);
			if (opt.getKeepRecvTimeout() == 0)
				opt.setKeepRecvTimeout(60);
			if (opt.getKeepSendTimeout() == 0)
				opt.setKeepSendTimeout(30);
		}

		public boolean registerNotifyTopicHandler(@NotNull String topic, @Nullable Consumer<NotifyTopic> handler) {
			return handler != null
					? notifyTopicHandlers.put(topic, handler) == null
					: notifyTopicHandlers.remove(topic) != null;
		}

		@Override
		public void start() throws Exception {
			lock();
			try {
				if (connector != null)
					stop();
				var cfg = getConfig();
				int n = cfg.connectorCount();
				if (n != 1)
					throw new IllegalStateException("connectorCount = " + n + " != 1");
				cfg.forEachConnector(c -> this.connector = c);
				super.start();
			} finally {
				unlock();
			}
		}

		public @NotNull TokenClient start(@NotNull String host, int port) throws Exception {
			lock();
			try {
				if (connector != null)
					stop();
				connector = new Connector(host, port, true);
				connector.SetService(this);
				connector.setAutoReconnect(true);
				// 手工connector路径不复位keepalive熔断：
				// stop→start(host,port)重启后keepalive定时器永久熔断，静默死链不再被检测。
				super.start();
				connector.start();
				return this;
			} finally {
				unlock();
			}
		}

		@Override
		public void stop() throws Exception {
			lock();
			try {
				if (connector != null) {
					connector.stop();
					connector = null;
				}
				super.stop();
			} finally {
				unlock();
			}
		}

		public void waitReady() {
			connector.WaitReady();
		}

		public @Nullable AsyncSocket getSocket() {
			return connector.getSocket();
		}

		// S4-F1：不再覆写OnSocketConnected保留"全Disable连上即完成握手"的快速路径。
		// 原快速路径按本端单边配置判定握手完成，OnHandshakeDone随连接建立立即回调并重放
		// SubTopic（明文应用协议）；对端配置加密时其解码准入门禁（FND7-23，双向codec装齐
		// 才撤销）将确定性拒绝断连。握手完成判定改为双边语义：与HandshakeClient一致推迟到
		// 对端SHandshake0驱动的握手交换（processSHandshake0的全Disable推荐分支已内建），
		// WaitReady（futureSocket由OnHandshakeDone链完成）亦随之推迟到握手完成后，
		// waitReady后立即可发的RPC不再与门禁冲突。全Disable互联代价为一次额外往返。
		@Override
		public void OnHandshakeDone(@NotNull AsyncSocket so) throws Exception {
			super.OnHandshakeDone(so);
			for (var topic : subTopics)
				new SubTopic(new BTopic.Data(topic)).Send(connector.getSocket());
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
			subTopics.add(topic);
			return new SubTopic(new BTopic.Data(topic)).SendForWait(connector.getSocket());
		}

		public boolean subTopic(@NotNull String topic,
								@NotNull ProtocolHandle<Rpc<BTopic.Data, EmptyBean.Data>> handler) {
			subTopics.add(topic);
			return new SubTopic(new BTopic.Data(topic)).Send(connector.getSocket(), handler);
		}

		public @NotNull TaskCompletionSource<EmptyBean.Data> unsubTopic(@NotNull String topic) {
			subTopics.remove(topic);
			return new UnsubTopic(new BTopic.Data(topic)).SendForWait(connector.getSocket());
		}

		public boolean unsubTopic(@NotNull String topic,
								  @NotNull ProtocolHandle<Rpc<BTopic.Data, EmptyBean.Data>> handler) {
			subTopics.remove(topic);
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

		protected long ProcessNotifyTopic(@NotNull NotifyTopic p) {
			var handler = notifyTopicHandlers.get(p.Argument.getTopic());
			if (handler == null)
				return Procedure.NotImplement;
			handler.accept(p);
			return Procedure.Success;
		}
	}

	public static final class TokenServer extends HandshakeServer {
		private static final class Session {
			final @NotNull AsyncSocket so;
			final HashSet<String> subTopics = new HashSet<>(); // 该session已订阅的主题,只在Session所在的IO线程访问,不需要考虑并发

			Session(@NotNull AsyncSocket so) {
				this.so = so;
			}
		}

		private final ConcurrentHashMap<String, CopyOnWriteArrayList<Session>> topicMap = new ConcurrentHashMap<>(); // key:topic

		TokenServer(Config config) {
			super("TokenServer", config);

			var opt = getConfig().getHandshakeOptions();
			if (opt.getKeepCheckPeriod() == 0)
				opt.setKeepCheckPeriod(5);
			if (opt.getKeepRecvTimeout() == 0)
				opt.setKeepRecvTimeout(60);
			if (opt.getKeepSendTimeout() == 0)
				opt.setKeepSendTimeout(30);
		}

		boolean subTopic(@NotNull Session session, @NotNull String topic) {
			if (!session.subTopics.add(topic))
				return false;
			// compute的remapping对同一key互斥执行：退订的"remove+isEmpty删条目"与订阅的"建表+add"若各自拆成
			// computeIfAbsent/computeIfPresent两步，交错时订阅会落在已被删条的孤儿列表上，静默丢失订阅（FND-S3-15）。
			topicMap.compute(topic, (__, ss) -> {
				if (null == ss)
					ss = new CopyOnWriteArrayList<>();
				ss.add(session);
				return ss;
			});
			return true;
		}

		boolean unsubTopic(@NotNull Session session, @NotNull String topic) {
			if (!session.subTopics.remove(topic))
				return false;
			// remove+isEmpty判定放进同一remapping原子段，与subTopic的compute互斥，消除静默丢失窗口。
			topicMap.computeIfPresent(topic, (__, ss) -> {
				ss.remove(session);
				return ss.isEmpty() ? null : ss;
			});
			return true;
		}

		void unsubAllTopics(@NotNull Session session) {
			for (var topic : session.subTopics) {
				topicMap.computeIfPresent(topic, (__, ss) -> {
					ss.remove(session);
					return ss.isEmpty() ? null : ss;
				});
			}
		}

		// S4-F1：不再覆写OnSocketAccept保留"全Disable直呼OnHandshakeDone"的服务端快速路径，
		// 与HandshakeServer一致无条件发送SHandshake0（全Disable时推荐值亦为全Disable，客户端
		// processSHandshake0的既有分支以CHandshakeDone+OnHandshakeDone完成握手）。原因见
		// TokenClient.OnHandshakeDone处注释：保留该快速路径，全Disable客户端就无法改为双边
		// 语义（收不到SHandshake0则永远等不到握手完成）。Disable互联代价为一次额外往返；
		// 加密TokenClient对接全Disable本服务端的错误配置，由客户端FND7-S2②的降级拒绝
		// 显式断连（原为握手永不发生的静默挂起，可诊断性更好）。
		@Override
		public void OnHandshakeDone(@NotNull AsyncSocket so) throws Exception {
			so.setUserState(new Session(so));
		}

		@Override
		public void OnSocketDisposed(@NotNull AsyncSocket so) throws Exception {
			super.OnSocketDisposed(so);
			var session = (Session)so.getUserState();
			if (session != null) {
				so.setUserState(null);
				unsubAllTopics(session);
			}
		}

		@Override
		public void dispatchProtocol(long typeId, @NotNull ByteBuffer bb,
									 @NotNull ProtocolFactoryHandle<?> factoryHandle, @Nullable AsyncSocket so)
				throws Exception {
			try {
				decodeProtocol(typeId, bb, factoryHandle, so).handle(this, factoryHandle); // 所有协议处理几乎无阻塞,可放心直接跑在IO线程上
			} catch (Throwable e) { // logger.error
				logger.error("dispatchProtocol exception:", e);
			}
		}
	}

	// TokenState+Object+key+lock+context+CHM.Note+CHM.table: 80+16+(24+16+24)+32+(24+16+X)+32+4 = 268+X bytes
	private static final class TokenState extends SoftReference<Object> {
		final @NotNull Token token;
		final @NotNull String key;
		final FastLock lock = new FastLock();
		final @Nullable InetSocketAddress remoteAddr;
		final @NotNull Binary context; // 绑定的上下文
		final long createTime; // 创建时间戳(毫秒)
		final long endTime; // 失效时间戳(毫秒)
		long count; // 已访问次数. -1表示刚刚删除

		TokenState(@NotNull Token token, @NotNull String key, @Nullable SocketAddress remoteAddr,
				   @NotNull Binary context, long ttl) {
			super(new Object(), Token.refQueue);
			this.token = token;
			this.key = key;
			this.remoteAddr = remoteAddr instanceof InetSocketAddress ? (InetSocketAddress)remoteAddr : null;
			this.context = context;
			this.createTime = System.currentTimeMillis();
			this.endTime = createTime + ttl;
		}

		TokenState(@NotNull Token token, @NotNull String key, @NotNull ByteBuffer bb) {
			super(new Object(), Token.refQueue);
			this.token = token;
			this.key = key;
			int v = bb.ReadByte();
			if (v == 0)
				remoteAddr = null;
			else if (v == 1) {
				var ip = bb.ReadBytes();
				int port = bb.ReadInt();
				InetAddress addr;
				try {
					addr = InetAddress.getByAddress(ip);
				} catch (Exception e) {
					addr = null;
				}
				remoteAddr = addr != null ? new InetSocketAddress(addr, port) : null;
			} else
				throw new IllegalStateException("unknown TokenState version = " + v);
			context = bb.ReadBinary();
			createTime = bb.ReadLong();
			endTime = bb.ReadLong();
			count = Math.max(bb.ReadLong(), 0);
		}

		static long decodeEndTime(@NotNull ByteBuffer bb) {
			int v = bb.ReadByte();
			if (v == 1) {
				bb.ReadBytes(); // ip
				bb.ReadInt(); // port
			} else if (v != 0)
				throw new IllegalStateException("unknown TokenState version = " + v);
			bb.ReadBinary(); // context
			bb.ReadLong(); // createTime
			return bb.ReadLong(); // endTime
			// bb.ReadLong(); // count
		}

		@NotNull ByteBuffer encode(@Nullable ByteBuffer bb) {
			if (bb == null)
				bb = ByteBuffer.Allocate(32);
			bb.WriteByte(remoteAddr != null ? 1 : 0);
			if (remoteAddr != null) {
				bb.WriteBytes(remoteAddr.getAddress().getAddress()); // getHostName()不靠谱,只记IP地址吧
				bb.WriteInt(remoteAddr.getPort());
			}
			bb.WriteBinary(context);
			bb.WriteLong(createTime);
			bb.WriteLong(endTime);
			bb.WriteLong(count);
			return bb;
		}

		@NotNull String getRemoteAddr() {
			var addr = remoteAddr != null ? remoteAddr.getAddress().getHostAddress() : null;
			return addr != null ? addr : "";
		}
	}

	private static boolean moveToDB(@NotNull TokenState state, @NotNull ByteBuffer bb, boolean waitLock) {
		FastLock lock = null;
		try {
			var k = state.key.getBytes(StandardCharsets.UTF_8);
			if (waitLock)
				state.lock.lock();
			else if (!state.lock.tryLock())
				return false;
			lock = state.lock;
			if (state.count != -1) {
				if (System.currentTimeMillis() <= state.endTime) {
					bb.Reset();
					var v = state.encode(bb);
					state.token.tokenMapTable.put(k, 0, k.length, v.Bytes, 0, v.WriteIndex);
				}
				state.count = -1;
				state.token.tokenMap.remove(state.key, state);
			}
		} catch (Throwable e) { // logger.error
			logger.error("cleanTokenRef.moveToDB exception:", e);
		} finally {
			if (lock != null)
				lock.unlock();
		}
		return true;
	}

	private static void cleanTokenRef() {
		final int STATE_BUF_COUNT = 256;
		var bb = ByteBuffer.Allocate(32);
		var stateBuf = new TokenState[STATE_BUF_COUNT];
		int stateBufCount = 0;
		for (; ; ) {
			try {
				var state = (TokenState)(stateBufCount == 0 ? refQueue.remove() : refQueue.poll());
				if (state == null) {
					for (int i = 0; i < stateBufCount; i++) {
						//noinspection DataFlowIssue
						moveToDB(stateBuf[i], bb, true);
						stateBuf[i] = null;
					}
					stateBufCount = 0;
				} else {
					if (!moveToDB(state, bb, stateBufCount == STATE_BUF_COUNT))
						stateBuf[stateBufCount++] = state;
					tokenSoftRefCleanCounter.increment();
				}
			} catch (Throwable e) { // logger.error
				logger.error("cleanTokenRef exception:", e);
			}
		}
	}

	private final Random tokenRandom = new SecureRandom();
	private final ConcurrentHashMap<String, TokenState> tokenMap = new ConcurrentHashMap<>();
	private final LongAdder newCounter = new LongAdder(); // 分配计数
	// volatile：start/closeDb在锁内写，协议任务与cleanTokenMapTable线程无锁读。
	private volatile RocksDatabase rocksdb;
	private volatile RocksDatabase.Table tokenMapTable;
	private TokenServer service;
	private TimerFuture<?> cleanTokenMapFuture;
	private volatile ScheduledFuture<?> cleanTokenMapTableFuture;
	// S4-F3：cleanTokenMapTable的运行标志——stop()在cancel(true)后、saveDB/closeDb前于Token
	// 锁外带超时等其归零（cancel只发中断不等待任务退出，任务观察中断后仍会commit/收尾；
	// 不等即关库违反RocksDatabase关闭契约，在飞get/put/commit对已关闭句柄操作可崩JVM）。
	// 必须锁外等待：任务的finally要拿同一把Token锁做重调度检查，持锁等待即死锁。
	private volatile boolean cleanTokenMapTableRunning;
	private static final long CLEAN_TOKEN_MAP_TABLE_STOP_TIMEOUT_MS = 10_000;

	public TokenServer getService() {
		return service;
	}

	// 参数host,port优先; 如果传null/<=0则以conf为准; 如果conf也没配置则用默认值null/DEFAULT_PORT
	public Token start(@Nullable Config conf, @Nullable String host, int port) throws Exception {
		lock();
		try {
			if (service != null)
				return this;

			rocksdb = new RocksDatabase(PropertiesHelper.getString("token.rocksdb", "token_db"));
			// getOrAddTable在下方回滚try块内（FND8-63）：原与建库一起在try之外，getOrAddTable
			// 失败时rocksdb已赋值无人关闭，后续每次start重复new RocksDatabase对同目录二次
			// open——Windows下LOCK互斥每次必抛（重试环还先空转10秒），restart永久失败。

			tokenRefCleanerLock.lock();
			try {
				if (tokenRefCleaner == null) {
					tokenRefCleaner = new Thread(Token::cleanTokenRef, "TokenRefCleaner");
					tokenRefCleaner.setDaemon(true);
					tokenRefCleaner.setPriority(Thread.MAX_PRIORITY);
					tokenRefCleaner.start();
				}
			} finally {
				tokenRefCleanerLock.unlock();
			}

			ZezeCounter.tryInit();

			try {
				tokenMapTable = rocksdb.getOrAddTable("tokenMap");
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

				cleanTokenMapFuture = TaskSpec.ofAction(this::cleanTokenMap).schedulePeriodNow(1000, 1000);
				cleanTokenMapTableFuture = TaskSpec.ofAction(this::cleanTokenMapTable).scheduleAtNow(3, 14);
				return this;
			} catch (Throwable ex) {
				// 启动全有或全无（FND4-69）：
				// 半途失败清空已建状态，否则重入检查把"未运行的服务"当已启动直接
				// 返回this——二次start假成功，服务永不监听且清理任务未注册，
				// getService()非null掩盖故障。
				if (cleanTokenMapFuture != null) {
					cleanTokenMapFuture.cancel(false);
					cleanTokenMapFuture = null;
				}
			if (cleanTokenMapTableFuture != null) {
				cleanTokenMapTableFuture.cancel(false);
				cleanTokenMapTableFuture = null;
			}
			if (service != null) {
				// 先stop再置null（FND7-22）：config.start()逐个启动acceptor，多acceptor配置下
				// 前一个bind成功、后一个失败时，已bind的监听socket与start()启动的keepAlive
				// 定时器仍在运行；只置null的话无人能再停它们——同端口重试start永远bind冲突，
				// 直到进程退出。stop失败仅记日志，不掩盖原始启动异常。
				try {
					service.stop();
				} catch (Throwable t) { // logger.error
					logger.error("Token.start rollback service.stop exception:", t);
				}
				service = null;
			}
			if (rocksdb != null) {
					try {
						rocksdb.close();
					} catch (Throwable ignored) {
					}
					rocksdb = null;
				}
				tokenMapTable = null;
				throw ex;
			}
		} finally {
			unlock();
		}
	}

	public void stop() throws Exception {
		lock();
		try {
			if (cleanTokenMapTableFuture != null) {
				cleanTokenMapTableFuture.cancel(true);
				cleanTokenMapTableFuture = null;
			}
			if (cleanTokenMapFuture != null) {
				cleanTokenMapFuture.cancel(true);
				cleanTokenMapFuture = null;
			}
			if (service != null) {
				service.stop();
				service = null;
			}
		} finally {
			unlock();
		}
		// S4-F3：锁外带超时等待cleanTokenMapTable真正结束（任务的finally要拿Token锁重调度，
		// 持锁等待必死锁，见cleanTokenMapTableRunning注释）。等待窗口内并发的start()会因
		// 旧库未关而在目录LOCK互斥上失败（FND8-63既有行为），不产生静默双开。
		waitCleanTokenMapTableStopped();
		lock();
		try {
			if (rocksdb != null) {
				// stop关库（FND8-63）：stop只saveDB不关库时rocksdb/tokenMapTable悬挂，restart的
				// start对同目录二次open——Windows下LOCK互斥每次必抛（重试环还先空转10秒），
				// Linux下双实例双WAL/memtable写同目录。对齐全仓stop关库惯例（RedoQueue等先例）；
				// 重启后ProcessGetTokenRequest对miss的token按需懒加载，无数据语义损失。
				// saveDB失败记录（不改变stop异常契约）：此时关库，未落库的内存态token会丢。
				if (!saveDB())
					logger.error("Token.stop saveDB failed, in-memory token states may be lost.");
				closeDb(); // 先saveDB后关库（closeDb置null两字段；可重入锁安全）
			}
			tokenMap.clear();
		} finally {
			unlock();
		}
	}

	/** S4-F3：等待cleanTokenMapTable运行标志归零（带超时兜底，超时记录后放弃——极端挂死
	 * 任务不应让stop永久阻塞，残余竞态按RocksDB侧崩溃风险接受并在日志中可见）。 */
	private void waitCleanTokenMapTableStopped() {
		var deadline = System.currentTimeMillis() + CLEAN_TOKEN_MAP_TABLE_STOP_TIMEOUT_MS;
		while (cleanTokenMapTableRunning) {
			if (System.currentTimeMillis() >= deadline) {
				logger.error("cleanTokenMapTable not exit in {}ms after stop, give up waiting.",
						CLEAN_TOKEN_MAP_TABLE_STOP_TIMEOUT_MS);
				return;
			}
			try {
				//noinspection BusyWait
				Thread.sleep(1);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt(); // 保留中断标记继续限时等待（deadline兜底）
			}
		}
	}

	public void closeDb() {
		lock();
		try {
			if (rocksdb != null) {
				rocksdb.close();
				rocksdb = null;
			}
			tokenMapTable = null;
		} finally {
			unlock();
		}
	}

	private void cleanTokenMap() {
		var now = System.currentTimeMillis();
		for (var state : tokenMap.values()) {
			if (state.endTime < now && state.lock.tryLock()) {
				try {
					if (state.count != -1) {
						state.count = -1;
						tokenMap.remove(state.key, state);
					}
				} finally {
					state.lock.unlock();
				}
			}
		}
	}

	// 包装层（S4-F3）：入口/出口维护volatile运行标志，供stop()锁外等待任务真正结束——
	// 归零即本任务连同其finally的重调度检查全部完成、不再触碰rocksdb。
	private void cleanTokenMapTable() {
		cleanTokenMapTableRunning = true;
		try {
			cleanTokenMapTableOnce();
		} finally {
			cleanTokenMapTableRunning = false;
		}
	}

	private void cleanTokenMapTableOnce() {
		logger.info("cleanTokenMapTable: begin ...");
		var now = System.currentTimeMillis();
		var bb = ByteBuffer.Wrap(ByteBuffer.Empty);
		long n = 0, d = 0;
		// S4-F2：newBatch用try-with-resources包住（对齐saveDB）——原batch从不close，
		// WriteBatch持有native内存，每14秒调度一次的任务逐次泄漏累积。
		try (var batch = rocksdb.newBatch()) {
			try (var it = tokenMapTable.iterator()) {
				for (it.seekToFirst(); it.isValid(); it.next()) {
					try {
						bb.wraps(it.value());
						if (TokenState.decodeEndTime(bb) < now) {
							tokenMapTable.delete(batch, it.key());
							d++;
						}
						n++;
					} catch (Exception e) {
						logger.warn("cleanTokenMapTable exception:", e);
					}
					if (Thread.interrupted())
						break;
				}
				batch.commit();
			}
		} catch (Exception e) {
			logger.error("cleanTokenMapTable exception:", e);
		} finally {
			logger.info("cleanTokenMapTable: {} => {} ({} ms)", n, n - d, System.currentTimeMillis() - now);
			lock();
			try {
				// stop已取消并置空任务（或closeDb已置空rocksdb）时不再重调度，避免任务"复活"后访问已关闭的资源。
				if (rocksdb != null && cleanTokenMapTableFuture != null)
					cleanTokenMapTableFuture = TaskSpec.ofAction(this::cleanTokenMapTable).scheduleAtNow(3, 14);
			} finally {
				unlock();
			}
		}
	}

	private boolean saveDB() {
		logger.info("saveDB begin ...");
		try {
			var timeBegin = System.nanoTime();
			long n = 0;
			var bb = ByteBuffer.Allocate(32);
			try (var batch = rocksdb.newBatch()) {
				var now = System.currentTimeMillis();
				for (var state : tokenMap.values()) {
					var k = state.key.getBytes(StandardCharsets.UTF_8);
					state.lock.lock();
					try {
						if (state.count != -1) {
							if (now <= state.endTime) {
								bb.Reset();
								var v = state.encode(bb);
								tokenMapTable.put(batch, k, 0, k.length, v.Bytes, 0, v.WriteIndex);
							}
							state.count = -1;
							tokenMap.remove(state.key, state);
							n++;
						}
					} finally {
						state.lock.unlock();
					}
				}
				batch.commit();
			}
			logger.info("saveDB end ({}, {} ms)", n, (System.nanoTime() - timeBegin) / 1_000_000);
			return true;
		} catch (Exception e) {
			logger.error("saveDB exception:", e);
			return false;
		}
	}

	@Override
	protected long ProcessNewTokenRequest(@NotNull Zeze.Builtin.Token.NewToken r) {
		var arg = r.Argument;
		var ttl = arg.getTtl();
		if (ttl <= 0) {
			r.SendResultCode(-1);
			return Procedure.Success;
		}
		var remoteAddr = r.getSender().getRemoteAddress();
		String token;
		var created = new OutObject<>(false);
		do {
			token = genToken();
			// 这里严格来说应该再检查一下数据库里有没有这个token,但考虑到token含有秒时间戳,在tokenMap的软引用存活期不应该小于1秒
			tokenMap.computeIfAbsent(token, t -> {
				created.value = true;
				return new TokenState(this, t, remoteAddr, arg.getContext(), ttl);
			});
		} while (!created.value);
		newCounter.increment();
		r.Result.setToken(token);
		r.SendResultCode(0);
		return Procedure.Success;
	}

	@Override
	protected long ProcessGetTokenRequest(@NotNull Zeze.Builtin.Token.GetToken r) {
		var arg = r.Argument;
		var res = r.Result;
		var token = arg.getToken();
		var maxCount = arg.getMaxCount();
		for (; ; ) {
			var state = tokenMap.get(token);
			if (state == null) {
				try {
					// S4-F3：tokenMapTable为volatile，closeDb/stop置null后miss路径不得裸解引用。
					// 尽力收窄：判空后关库仍可能与本get竞态（TOCTOU残余接受，裁决既定）。
					var table = tokenMapTable;
					if (table != null) {
						var v = table.get(token.getBytes(StandardCharsets.UTF_8));
						if (v != null)
							state = tokenMap.computeIfAbsent(token, t -> new TokenState(this, t, ByteBuffer.Wrap(v)));
					}
				} catch (Exception e) {
					logger.warn("tokenMapTable.get exception:", e);
				}
				if (state == null) {
					res.setTime(-1);
					r.SendResultCode(0);
					return Procedure.Success;
				}
			} else
				state.get(); // touch SoftReference
			state.lock.lock();
			try {
				var count = state.count;
				if (count == -1)
					continue; // removed, retry
				var time = System.currentTimeMillis();
				if (time >= state.endTime) {
					state.count = -1;
					tokenMap.remove(token, state);
					res.setTime(-2);
				} else {
					count++;
					if (maxCount > 0 && count >= maxCount && !tokenMap.remove(token, state))
						res.setTime(-3);
					else {
						if (maxCount > 0 && count >= maxCount) {
							// 达到maxCount被成功移除后置-1（与"移除即置-1"约定一致），防止软引用清理时moveToDB写回复活耗尽token。
							state.count = -1;
							// 核销还必须删除RocksDB中已落库的副本：核销前saveDB/moveToDB写入的旧count
							// 会在重启/软引用回收后经DB回载重新起算，同一token可无限次重放
							// （707a0eff1只封住了核销后被moveToDB写回，未清核销前已落库的副本）。
							// moveToDB/saveDB都持state.lock且跳过count==-1的state，删后不会被写回。
							try {
								var table = tokenMapTable; // S4-F3：同上，停机窗口尽力收窄
								if (table != null)
									table.delete(token.getBytes(StandardCharsets.UTF_8));
							} catch (Exception e) {
								logger.warn("tokenMapTable.delete on consume exception:", e);
							}
						} else
							state.count = count;
						res.setContext(state.context);
						res.setCount(count);
						res.setTime(time - state.createTime);
					}
				}
			} finally {
				state.lock.unlock();
			}
			res.setAddr(state.getRemoteAddr());
			r.SendResultCode(0);
			return Procedure.Success;
		}
	}

	@Override
	protected long ProcessTokenStatusRequest(@NotNull TokenStatus r) {
		var res = r.Result;
		res.setNewCount(newCounter.sum());
		res.setCurCount(tokenMap.size());
		var s = service;
		res.setConnectCount(s != null ? s.getSocketCount() : -1);
		res.setPerfLog(ZezeCounter.instance.getLast().formattedLog());
		r.SendResultCode(0);
		return Procedure.Success;
	}

	@Override
	protected long ProcessSubTopicRequest(@NotNull SubTopic r) {
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
	protected long ProcessUnsubTopicRequest(@NotNull UnsubTopic r) {
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

	@Override
	protected long ProcessPubTopicRequest(@NotNull PubTopic r) {
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
				if (canLogNotifyTopic && !sb.isEmpty()) {
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
					if (count == 0)
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

	public static void main(@NotNull String @NotNull [] args) throws Exception {
		Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
			//noinspection CallToPrintStackTrace
			e.printStackTrace();
			logger.error("uncaught exception in {}:", t, e);
		});

		String host = null;
		int port = 0;
		int netThreadCount = 0;
		int workerThreadCount = 0;

		for (int i = 0; i < args.length; ++i) {
			switch (args[i]) {
			case "-host":
				host = requireValue(args, ++i, "-host");
				if (host.isBlank())
					host = null;
				break;
			case "-port":
				port = requireInt(args, ++i, "-port");
				break;
			case "-net-threads":
				netThreadCount = requireInt(args, ++i, "-net-threads");
				break;
			case "-worker-threads":
				workerThreadCount = requireInt(args, ++i, "-worker-threads");
				break;
			default:
				throw new IllegalArgumentException("unknown argument: " + args[i]);
			}
		}

		if (netThreadCount < 1)
			netThreadCount = Runtime.getRuntime().availableProcessors();
		if (workerThreadCount < 1)
			workerThreadCount = Runtime.getRuntime().availableProcessors() * 2;
		Task.initThreadPool(Task.newFixedThreadPool(workerThreadCount, "ZezeTaskPool"),
				Executors.newSingleThreadScheduledExecutor(
						new ThreadFactoryWithName("ZezeScheduledPool", Thread.NORM_PRIORITY + 2)));
		if (Selectors.getInstance().getCount() < netThreadCount)
			Selectors.getInstance().add(netThreadCount - Selectors.getInstance().getCount());

		var token = new Token();
		ShutdownHook.add(token::stop);

		token.start(null, host, port);
		synchronized (Thread.currentThread()) {
			Thread.currentThread().wait();
		}
	}
}
