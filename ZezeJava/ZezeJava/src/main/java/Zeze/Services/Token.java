package Zeze.Services;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.LongAdder;
import java.util.concurrent.locks.ReentrantReadWriteLock;
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
import Zeze.Util.DaemonTimer;
import Zeze.Util.FastLock;
import Zeze.Util.OutObject;
import Zeze.Util.PropertiesHelper;
import Zeze.Util.RocksDatabase;
import Zeze.Util.ShutdownHook;
import Zeze.Util.Task;
import Zeze.Util.TaskCompletionSource;
import Zeze.Util.ThreadFactoryWithName;
import Zeze.Util.ZezeCounter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jspecify.annotations.Nullable;
import static Zeze.Util.Args.requireInt;
import static Zeze.Util.Args.requireValue;
import Zeze.Util.RocksDatabase.Table;

/**
 * Token服务：生成/核销一次性令牌（内存SoftReference缓存+RocksDB落库），附带主题订阅广播。
 */
public final class Token extends AbstractToken {
	// 表级读写锁：运行期读写、迁移落库与分批清理持读锁并发；stop/saveDB/closeDb 持写锁排空原生句柄访问
	private final ReentrantReadWriteLock tokenStoreLock = new ReentrantReadWriteLock();
	// Key 级分段加载锁：仲裁冷缓存回载，防止并发 GetToken 同时回载同一 DB 副本重放核销（永不与 state.lock 嵌套）
	private static final int TOKEN_LOAD_LOCK_COUNT = 256;
	private static final int TOKEN_LOAD_LOCK_MASK = TOKEN_LOAD_LOCK_COUNT - 1;
	// 分批清理 RocksDB 过期行的批上限，批间释放读锁避免长时间阻塞停机写锁
	static final int CLEAN_TABLE_BATCH_SIZE = 1000;
	private final FastLock[] tokenLoadLocks = createTokenLoadLocks();
	private static final Logger logger = LogManager.getLogger(Token.class);
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
	public static String genToken(Random random) {
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

	private String genToken() {
		return genToken(tokenRandom);
	}

	public static class TokenClient extends HandshakeClient {
		private @Nullable Connector connector;
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

		public boolean registerNotifyTopicHandler(String topic, @Nullable Consumer<NotifyTopic> handler) {
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

		public TokenClient start(String host, int port) throws Exception {
			lock();
			try {
				if (connector != null)
					stop();
				connector = new Connector(host, port, true);
				connector.SetService(this);
				connector.setAutoReconnect(true);
				// 先调用 super.start() 复位 stop 后的服务状态与保活定时器
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
			//noinspection DataFlowIssue
			connector.WaitReady();
		}

		public @Nullable AsyncSocket getSocket() {
			//noinspection DataFlowIssue
			return connector.getSocket();
		}

		// 沿用 HandshakeClient 双边握手语义：推迟到对端 SHandshake0 完成握手后再重放 SubTopic，避免明文撞服务端门禁
		@Override
		public void OnHandshakeDone(AsyncSocket so) throws Exception {
			super.OnHandshakeDone(so);
			for (var topic : subTopics)
				//noinspection DataFlowIssue
				new SubTopic(new BTopic.Data(topic)).Send(connector.getSocket());
		}

		public TaskCompletionSource<BNewTokenRes.Data> newToken(@Nullable Binary context, long ttl) {
			//noinspection DataFlowIssue
			return new NewToken(new BNewTokenArg.Data(context, ttl)).SendForWait(connector.getSocket());
		}

		public boolean newToken(@Nullable Binary context, long ttl,
								ProtocolHandle<Rpc<BNewTokenArg.Data, BNewTokenRes.Data>> handler) {
			//noinspection DataFlowIssue
			return new NewToken(new BNewTokenArg.Data(context, ttl)).Send(connector.getSocket(), handler);
		}

		public TaskCompletionSource<BGetTokenRes.Data> getToken(String token, long maxCount) {
			//noinspection DataFlowIssue
			return new GetToken(new BGetTokenArg.Data(token, maxCount)).SendForWait(connector.getSocket());
		}

		public boolean getToken(String token, long maxCount,
								ProtocolHandle<Rpc<BGetTokenArg.Data, BGetTokenRes.Data>> handler) {
			//noinspection DataFlowIssue
			return new GetToken(new BGetTokenArg.Data(token, maxCount)).Send(connector.getSocket(), handler);
		}

		public TaskCompletionSource<EmptyBean.Data> subTopic(String topic) {
			subTopics.add(topic);
			//noinspection DataFlowIssue
			return new SubTopic(new BTopic.Data(topic)).SendForWait(connector.getSocket());
		}

		public boolean subTopic(String topic,
								ProtocolHandle<Rpc<BTopic.Data, EmptyBean.Data>> handler) {
			subTopics.add(topic);
			//noinspection DataFlowIssue
			return new SubTopic(new BTopic.Data(topic)).Send(connector.getSocket(), handler);
		}

		public TaskCompletionSource<EmptyBean.Data> unsubTopic(String topic) {
			subTopics.remove(topic);
			//noinspection DataFlowIssue
			return new UnsubTopic(new BTopic.Data(topic)).SendForWait(connector.getSocket());
		}

		public boolean unsubTopic(String topic,
								  ProtocolHandle<Rpc<BTopic.Data, EmptyBean.Data>> handler) {
			subTopics.remove(topic);
			//noinspection DataFlowIssue
			return new UnsubTopic(new BTopic.Data(topic)).Send(connector.getSocket(), handler);
		}

		public TaskCompletionSource<EmptyBean.Data> pubTopic(String topic, @Nullable Binary content,
																	  boolean broadcast) {
			//noinspection DataFlowIssue
			return new PubTopic(new BPubTopic.Data(topic, content, broadcast)).SendForWait(connector.getSocket());
		}

		public boolean pubTopic(String topic, @Nullable Binary content, boolean broadcast,
								ProtocolHandle<Rpc<BPubTopic.Data, EmptyBean.Data>> handler) {
			//noinspection DataFlowIssue
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

	public static final class TokenServer extends HandshakeServer {
		private static final class Session {
			final AsyncSocket so;
			final HashSet<String> subTopics = new HashSet<>(); // 该session已订阅的主题,只在Session所在的IO线程访问,不需要考虑并发

			Session(AsyncSocket so) {
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

		boolean subTopic(Session session, String topic) {
			if (!session.subTopics.add(topic))
				return false;
			// 用 compute 保证与退订删空条目互斥，防止订阅落到已移除的孤儿列表
			topicMap.compute(topic, (__, ss) -> {
				if (null == ss)
					ss = new CopyOnWriteArrayList<>();
				ss.add(session);
				return ss;
			});
			return true;
		}

		boolean unsubTopic(Session session, String topic) {
			if (!session.subTopics.remove(topic))
				return false;
			// 与 subTopic 的 compute 互斥，在同一步完成移除与删空
			topicMap.computeIfPresent(topic, (__, ss) -> {
				ss.remove(session);
				return ss.isEmpty() ? null : ss;
			});
			return true;
		}

		void unsubAllTopics(Session session) {
			for (var topic : session.subTopics) {
				topicMap.computeIfPresent(topic, (__, ss) -> {
					ss.remove(session);
					return ss.isEmpty() ? null : ss;
				});
			}
		}

		// 沿用 HandshakeServer 统一发送 SHandshake0，确保双边握手完成语义一致
		@Override
		public void OnHandshakeDone(AsyncSocket so) throws Exception {
			// CHandshakeDone 可重复到达；已订阅的 Session 必须保持到连接销毁时统一退订。
			if (so.isHandshakeDone())
				return;
			so.setUserState(new Session(so));
			super.OnHandshakeDone(so);
		}

		@Override
		public void OnSocketDisposed(AsyncSocket so) throws Exception {
			super.OnSocketDisposed(so);
			var session = (Session)so.getUserState();
			if (session != null) {
				so.setUserState(null);
				unsubAllTopics(session);
			}
		}

		@Override
		public void dispatchProtocol(long typeId, ByteBuffer bb,
									 ProtocolFactoryHandle<?> factoryHandle, @Nullable AsyncSocket so)
				throws Exception {
			try {
				decodeProtocol(typeId, bb, factoryHandle, so).handle(this, factoryHandle); // 所有协议处理几乎无阻塞,可放心直接跑在IO线程上
			} catch (Throwable e) {
				logger.error("dispatchProtocol exception:", e);
			}
		}
	}

	// TokenState+Object+key+lock+context+CHM.Note+CHM.table: 80+16+(24+16+24)+32+(24+16+X)+32+4 = 268+X bytes
	private static final class TokenState extends SoftReference<Object> {
		final Token token;
		final String key;
		final FastLock lock = new FastLock();
		final @Nullable InetSocketAddress remoteAddr;
		final Binary context; // 绑定的上下文
		final long createTime; // 创建时间戳(毫秒)
		final long endTime; // 失效时间戳(毫秒)
		long count; // 已访问次数. -1表示刚刚删除

		TokenState(Token token, String key, @Nullable SocketAddress remoteAddr,
				   Binary context, long createTime, long endTime) {
			super(new Object(), Token.refQueue);
			this.token = token;
			this.key = key;
			this.remoteAddr = remoteAddr instanceof InetSocketAddress ? (InetSocketAddress)remoteAddr : null;
			this.context = context;
			this.createTime = createTime;
			this.endTime = endTime;
		}

		TokenState(Token token, String key, ByteBuffer bb) {
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

		static long decodeEndTime(ByteBuffer bb) {
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

		ByteBuffer encode(@Nullable ByteBuffer bb) {
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

		String getRemoteAddr() {
			var addr = remoteAddr != null ? remoteAddr.getAddress().getHostAddress() : null;
			return addr != null ? addr : "";
		}
	}

	private static FastLock[] createTokenLoadLocks() {
		var locks = new FastLock[TOKEN_LOAD_LOCK_COUNT];
		for (int i = 0; i < locks.length; i++)
			locks[i] = new FastLock();
		return locks;
	}

	private FastLock tokenLoadLock(String token) {
		int h = token.hashCode();
		return tokenLoadLocks[(h ^ (h >>> 16)) & TOKEN_LOAD_LOCK_MASK];
	}

	private static boolean moveToDB(TokenState state, ByteBuffer bb, boolean waitLock) {
		var readLock = state.token.tokenStoreLock.readLock();
		if (waitLock)
			readLock.lock();
		else if (!readLock.tryLock())
			return false;
		try {
			return moveToDBLocked(state, bb, waitLock);
		} finally {
			readLock.unlock();
		}
	}

	private static boolean moveToDBLocked(TokenState state, ByteBuffer bb, boolean waitLock) {
		var table = state.token.tokenMapTable;
		if (table == null)
			return true; // DB已关闭，放弃迁移且不再放回stateBuf重试
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
					table.put(k, 0, k.length, v.Bytes, 0, v.WriteIndex);
				}
				state.count = -1;
				state.token.tokenMap.remove(state.key, state);
			}
		} catch (Throwable e) {
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
			} catch (Throwable e) {
				logger.error("cleanTokenRef exception:", e);
			}
		}
	}

	private final Random tokenRandom = new SecureRandom();
	private final ConcurrentHashMap<String, TokenState> tokenMap = new ConcurrentHashMap<>();
	private final LongAdder newCounter = new LongAdder(); // 分配计数
	// start/closeDb 锁内写，协议任务与清理线程无锁读
	private volatile @Nullable RocksDatabase rocksdb;
	private volatile @Nullable Table tokenMapTable;
	// stop 置位、start 清零，让分批 RocksDB 清理在批边界尽快退出
	private volatile boolean stopping;
	private @Nullable TokenServer service;
	// 每秒扫描过期内存令牌；stop 在锁外有界等待在飞轮次
	private final DaemonTimer cleanTokenMapDaemon = new DaemonTimer("Token.cleanTokenMap", 1000, this::cleanTokenMap);
	// 每日 03:14 分批清理 RocksDB 过期令牌
	private final DaemonTimer cleanTokenMapTableDaemon = new DaemonTimer("Token.cleanTokenMapTable",
			() -> Task.delayUntilNextDaily(3, 14), 0, this::cleanTokenMapTableOnce);

	public @Nullable TokenServer getService() {
		return service;
	}

	// 参数host,port优先; 如果传null/<=0则以conf为准; 如果conf也没配置则用默认值null/DEFAULT_PORT
	public Token start(@Nullable Config conf, @Nullable String host, int port) throws Exception {
		lock();
		try {
			if (service != null)
				return this;

			stopping = false;
			rocksdb = new RocksDatabase(PropertiesHelper.getString("token.rocksdb", "token_db"));

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
				//noinspection DataFlowIssue
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

				cleanTokenMapDaemon.start();
				cleanTokenMapTableDaemon.start();
				return this;
			} catch (Throwable ex) {
				// 启动中途失败时回滚已建守护、监听端口与 RocksDB，确保重试 start 可行
				stopping = true;
				cleanTokenMapDaemon.stop();
				cleanTokenMapTableDaemon.stop();
				if (service != null) {
					try {
						service.stop();
					} catch (Throwable t) {
						logger.error("Token.start rollback service.stop exception:", t);
					}
					service = null;
				}
				if (rocksdb != null) {
					try {
						closeDb();
					} catch (Throwable ignored) {
					}
				}
				throw ex;
			}
		} finally {
			unlock();
		}
	}

	public void stop() throws Exception {
		lock();
		try {
			stopping = true;
			if (service != null) {
				service.stop();
				service = null;
			}
		} finally {
			unlock();
		}
		// 锁外有界等待两个清理守护退出，确保关库前不再有并发 RocksDB 访问
		cleanTokenMapDaemon.stop();
		cleanTokenMapTableDaemon.stop();
		lock();
		var writeLock = tokenStoreLock.writeLock();
		writeLock.lock();
		try {
			boolean saved = rocksdb != null || tokenMap.isEmpty();
			if (rocksdb != null) {
				// 先落库再关库，重启后冷令牌按需从 DB 懒加载；保存失败则保留内存态以便重试
				saved = saveDB();
				if (!saved)
					logger.error("Token.stop saveDB failed, in-memory token states may be lost.");
				closeDb();
			}
			if (saved)
				tokenMap.clear();
		} finally {
			writeLock.unlock();
			unlock();
		}
	}

	public void closeDb() {
		lock();
		var writeLock = tokenStoreLock.writeLock();
		writeLock.lock();
		try {
			var db = rocksdb;
			if (db != null) {
				db.close();
				rocksdb = null;
			}
			tokenMapTable = null;
		} finally {
			writeLock.unlock();
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

	private record CleanBatchResult(int scanned, int deleted, byte @Nullable [] lastKey, boolean hasMore) {
	}

	private void cleanTokenMapTableOnce() {
		cleanTokenMapTableOnce(CLEAN_TABLE_BATCH_SIZE);
	}

	void cleanTokenMapTableOnce(int batchSize) {
		if (batchSize <= 0)
			throw new IllegalArgumentException("batchSize <= 0: " + batchSize);
		if (stopping || rocksdb == null || tokenMapTable == null)
			return;
		logger.info("cleanTokenMapTable: begin ...");
		var startTime = System.currentTimeMillis();
		var bb = ByteBuffer.Wrap(ByteBuffer.Empty);
		long scannedTotal = 0, deletedTotal = 0;
		byte @Nullable [] resumeKey = null;
		var readLock = tokenStoreLock.readLock();
		try {
			while (!stopping && !Thread.currentThread().isInterrupted()) {
				readLock.lock();
				try {
					var db = rocksdb;
					var table = tokenMapTable;
					if (stopping || db == null || table == null || db.isClosing() || table.isDestroyPending())
						break;
					var result = cleanTokenMapTableBatch(db, table, bb, resumeKey, batchSize, System.currentTimeMillis());
					scannedTotal += result.scanned;
					deletedTotal += result.deleted;
					if (!result.hasMore)
						break;
					resumeKey = result.lastKey;
				} finally {
					readLock.unlock();
				}
			}
		} catch (Exception e) {
			logger.error("cleanTokenMapTable exception:", e);
		} finally {
			logger.info("cleanTokenMapTable: {} => {} ({} ms)",
					scannedTotal, scannedTotal - deletedTotal, System.currentTimeMillis() - startTime);
		}
	}

	private CleanBatchResult cleanTokenMapTableBatch(RocksDatabase db, Table table, ByteBuffer bb,
													 byte @Nullable [] resumeKey, int batchSize, long now)
			throws Exception {
		int scanned = 0;
		int deleted = 0;
		byte @Nullable [] lastKey = null;
		boolean hasMore = false;
		RocksDatabase.Batch batch = null;
		try (var it = table.iterator()) {
			if (resumeKey == null)
				it.seekToFirst();
			else {
				it.seek(resumeKey);
				if (RocksDatabase.isValidOrThrow(it) && Arrays.equals(it.key(), resumeKey))
					it.next();
			}
			while (scanned < batchSize && RocksDatabase.isValidOrThrow(it)) {
				var key = it.key();
				lastKey = key;
				try {
					bb.wraps(it.value());
					if (TokenState.decodeEndTime(bb) < now) {
						if (batch == null)
							batch = db.newBatch();
						table.delete(batch, key);
						deleted++;
					}
				} catch (Exception e) {
					logger.warn("cleanTokenMapTable exception:", e);
				}
				scanned++;
				it.next();
				if (stopping || Thread.currentThread().isInterrupted())
					break;
			}
			if (batch != null)
				batch.commit();
			if (scanned == batchSize && !stopping && !Thread.currentThread().isInterrupted()
					&& RocksDatabase.isValidOrThrow(it))
				hasMore = true;
		} finally {
			if (batch != null)
				batch.close();
		}
		return new CleanBatchResult(scanned, deleted, lastKey, hasMore);
	}

	private boolean saveDB() {
		var writeLock = tokenStoreLock.writeLock();
		writeLock.lock();
		try {
			return saveDBLocked();
		} finally {
			writeLock.unlock();
		}
	}

	private boolean saveDBLocked() {
		var db = rocksdb;
		var table = tokenMapTable;
		if (db == null || table == null)
			return false;
		logger.info("saveDB begin ...");
		try {
			var timeBegin = System.nanoTime();
			long n = 0;
			var saved = new java.util.ArrayList<TokenState>();
			var bb = ByteBuffer.Allocate(32);
			try (var batch = db.newBatch()) {
				var now = System.currentTimeMillis();
				for (var state : tokenMap.values()) {
					var k = state.key.getBytes(StandardCharsets.UTF_8);
					state.lock.lock();
					try {
						if (state.count != -1) {
							if (now <= state.endTime) {
								bb.Reset();
								var v = state.encode(bb);
								table.put(batch, k, 0, k.length, v.Bytes, 0, v.WriteIndex);
							}
							saved.add(state);
							n++;
						}
					} finally {
						state.lock.unlock();
					}
				}
				batch.commit();
				// 与单条moveToDB相同：落库成功以后才丢弃内存态，提交失败仍可重试。
				for (var state : saved) {
					state.lock.lock();
					try {
						state.count = -1;
						tokenMap.remove(state.key, state);
					} finally {
						state.lock.unlock();
					}
				}
			}
			logger.info("saveDB end ({}, {} ms)", n, (System.nanoTime() - timeBegin) / 1_000_000);
			return true;
		} catch (Exception e) {
			logger.error("saveDB exception:", e);
			return false;
		}
	}

	@Override
	protected long ProcessNewTokenRequest(Zeze.Builtin.Token.NewToken r) {
		var readLock = tokenStoreLock.readLock();
		readLock.lock();
		try {
			if (tokenMapTable == null) {
				r.SendResultCode(-1);
				return Procedure.Success;
			}
			return newTokenLocked(r);
		} finally {
			readLock.unlock();
		}
	}

	private long newTokenLocked(Zeze.Builtin.Token.NewToken r) {
		var arg = r.Argument;
		var ttl = arg.getTtl();
		if (ttl <= 0) {
			r.SendResultCode(-1);
			return Procedure.Success;
		}
		//noinspection DataFlowIssue
		var remoteAddr = r.getSender().getRemoteAddress();
		String token;
		var created = new OutObject<>(false);
		//noinspection DataFlowIssue
		do {
			token = genToken();
			// 这里严格来说应该再检查一下数据库里有没有这个token,但考虑到token含有秒时间戳,在tokenMap的软引用存活期不应该小于1秒
			var state = tokenMap.computeIfAbsent(token, t -> {
				var now = System.currentTimeMillis();
				if (now > Long.MAX_VALUE - ttl)
					return null;
				created.value = true;
				return new TokenState(this, t, remoteAddr, arg.getContext(), now, now + ttl);
			});
			if (state == null) {
				r.SendResultCode(-1);
				return Procedure.Success;
			}
		} while (!created.value);
		newCounter.increment();
		r.Result.setToken(token);
		r.SendResultCode(0);
		return Procedure.Success;
	}

	@Override
	protected long ProcessGetTokenRequest(Zeze.Builtin.Token.GetToken r) {
		var readLock = tokenStoreLock.readLock();
		readLock.lock();
		try {
			return getTokenLocked(r);
		} finally {
			readLock.unlock();
		}
	}

	private long getTokenLocked(Zeze.Builtin.Token.GetToken r) {
		var arg = r.Argument;
		var res = r.Result;
		var table = tokenMapTable;
		if (table == null) {
			res.setTime(-3);
			r.SendResultCode(0);
			return Procedure.Success;
		}
		var token = arg.getToken();
		var maxCount = arg.getMaxCount();
		for (; ; ) {
			var state = tokenMap.get(token);
			if (state == null) {
				state = loadTokenFromDbLocked(table, token);
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
					if (maxCount > 0 && count >= maxCount) {
						// 删除成功后才发布核销结果；失败保留原计数，客户端可以重试，不能返回一次成功消费。
						try {
							table.delete(token.getBytes(StandardCharsets.UTF_8));
						} catch (Exception e) {
							logger.warn("tokenMapTable.delete on consume exception:", e);
							res.setTime(-3);
							r.SendResultCode(0);
							return Procedure.Success;
						}
						state.count = -1;
						tokenMap.remove(token, state);
					} else
						state.count = count;
					res.setContext(state.context);
					res.setCount(count);
					res.setTime(time - state.createTime);
				}
			} finally {
				state.lock.unlock();
			}
			res.setAddr(state.getRemoteAddr());
			r.SendResultCode(0);
			return Procedure.Success;
		}
	}

	private @Nullable TokenState loadTokenFromDbLocked(Table table, String token) {
		var loadLock = tokenLoadLock(token);
		loadLock.lock();
		try {
			var state = tokenMap.get(token);
			if (state != null) {
				state.get(); // touch SoftReference
				return state;
			}
			var v = table.get(token.getBytes(StandardCharsets.UTF_8));
			if (v == null)
				return null;
			return tokenMap.computeIfAbsent(token, t -> new TokenState(this, t, ByteBuffer.Wrap(v)));
		} catch (Exception e) {
			logger.warn("tokenMapTable.get exception:", e);
			return null;
		} finally {
			loadLock.unlock();
		}
	}

	@Override
	protected long ProcessTokenStatusRequest(TokenStatus r) {
		var res = r.Result;
		res.setNewCount(newCounter.sum());
		res.setCurCount(tokenMap.size());
		var s = service;
		res.setConnectCount(s != null ? s.getSocketCount() : -1);
		res.setPerfLog(ZezeCounter.instance.getLastSnapshot().formattedLog());
		r.SendResultCode(0);
		return Procedure.Success;
	}

	@Override
	protected long ProcessSubTopicRequest(SubTopic r) {
		//noinspection DataFlowIssue
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
		//noinspection DataFlowIssue
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

	public static void main(String [] args) throws Exception {
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
				Executors.newScheduledThreadPool(1,
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
