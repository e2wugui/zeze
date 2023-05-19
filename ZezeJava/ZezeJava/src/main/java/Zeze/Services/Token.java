package Zeze.Services;

import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import Zeze.Config;
import Zeze.Net.Acceptor;
import Zeze.Net.AsyncSocket;
import Zeze.Net.Binary;
import Zeze.Net.Protocol;
import Zeze.Net.ProtocolHandle;
import Zeze.Net.Selectors;
import Zeze.Net.Service;
import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.Procedure;
import Zeze.Util.PerfCounter;
import Zeze.Util.Task;
import Zeze.Util.ThreadFactoryWithName;
import Zeze.Util.TimerFuture;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.jetbrains.annotations.Nullable;

/*
TODO:
1. 避免token过多导致内存占用过高. 数据库化? 淘汰机制?
2. token续期服务. 跟初始设置的ttl如何兼顾? 覆盖还是选最大值?
3. 给客户端提供Agent封装方便使用.
*/
public final class Token extends AbstractToken {
	static {
		System.getProperties().putIfAbsent("log4j.configurationFile", "log4j2.xml");
		var level = Level.toLevel(System.getProperty("logLevel"), Level.INFO);
		((LoggerContext)LogManager.getContext(false)).getConfiguration().getRootLogger().setLevel(level);
	}

	private static final Logger logger = LogManager.getLogger(Token.class);
	private static final int DEFAULT_PORT = 5003;
	private static final int TOKEN_CHAR_USED = 62; // 10+26+26
	private static final byte[] tokenCharTable = new byte[TOKEN_CHAR_USED];
	private static final Token instance = new Token();

	static {
		int i = 0;
		for (int b = '0'; b <= '9'; b++)
			tokenCharTable[i++] = (byte)b;
		for (int b = 'A'; b <= 'Z'; b++)
			tokenCharTable[i++] = (byte)b;
		for (int b = 'a'; b <= 'z'; b++)
			tokenCharTable[i++] = (byte)b;
	}

	// 生成24个字符的Token字符串. 每个字符只会出现半角的数字和字母共62种. 24个半角字符的字符串正好占满3个64位,内存利用率高.
	private String genToken() {
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

	public static Token getInstance() {
		return instance;
	}

	private static final class TokenService extends Service {
		private AsyncSocket socket;

		TokenService(Config config) {
			super("TokenService", config);
		}

		@Override
		public void start() throws Exception {
			start(null, DEFAULT_PORT);
		}

		public synchronized void start(@Nullable InetAddress addr, int port) {
			if (socket != null)
				stop();
			socket = newServerSocket(addr, port, new Acceptor(port, addr != null ? addr.getHostAddress() : null));
		}

		@Override
		public synchronized void stop() {
			if (socket != null) {
				socket.close();
				socket = null;
				for (var so : socketMap)
					so.close();
				socketMap.clear();
			}
		}

		@Override
		public void OnSocketAccept(AsyncSocket so) throws Exception {
			logger.info("OnSocketAccept: {}", so);
			super.OnSocketAccept(so);
		}

		@Override
		public void OnSocketClose(AsyncSocket so, Throwable e) throws Exception {
			logger.info("OnSocketClose: {}", so);
			super.OnSocketClose(so, e);
		}

		@Override
		public void dispatchProtocol(long typeId, ByteBuffer bb, ProtocolFactoryHandle<?> factoryHandle, AsyncSocket so)
				throws Exception {
			try {
				var p = decodeProtocol(typeId, bb, factoryHandle, so);
				p.handle(this, factoryHandle); // 所有协议处理几乎无阻塞,可放心直接跑在IO线程上
			} catch (Throwable e) { // logger.error
				logger.error("dispatchProtocol exception:", e);
			}
		}

		@Override
		public <P extends Protocol<?>> void dispatchRpcResponse(P rpc, ProtocolHandle<P> responseHandle,
																ProtocolFactoryHandle<?> factoryHandle) {
			try {
				responseHandle.handle(rpc); // 所有协议处理几乎无阻塞,可放心直接跑在IO线程上
			} catch (Throwable e) { // logger.error
				logger.error("dispatchRpcResponse exception:", e);
			}
		}
	}

	private static final class TokenState {
		final Binary context; // 绑定的上下文
		final long createTime; // 创建时间戳(毫秒)
		final long endTime; // 失效时间戳(毫秒)
		long count; // 已访问次数

		TokenState(Binary context, long ttl) {
			this.context = context;
			this.createTime = System.currentTimeMillis();
			this.endTime = createTime + ttl;
		}
	}

	private final Random tokenRandom = new SecureRandom();
	private final ConcurrentHashMap<String, TokenState> tokenMap = new ConcurrentHashMap<>();
	private TokenService service;
	private TimerFuture<?> cleanTokenMapFuture;

	public synchronized void start(@Nullable InetAddress addr, int port) {
		if (service != null)
			return;

		PerfCounter.instance.tryStartScheduledLog();

		service = new TokenService(new Config().loadAndParse());
		RegisterProtocols(service);
		service.start(addr, port);

		cleanTokenMapFuture = Task.scheduleUnsafe(1000, 1000, this::cleanTokenMap);
	}

	public synchronized void stop() {
		if (service != null) {
			cleanTokenMapFuture.cancel(true);
			tokenMap.clear();
			service.stop();
			service = null;
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
		String token;
		do {
			token = genToken();
		} while (tokenMap.putIfAbsent(token, new TokenState(arg.getContext(), ttl)) != null);
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
		var maxCount = arg.getMaxCount();
		//noinspection SynchronizationOnLocalVariableOrMethodParameter
		synchronized (state) {
			var time = System.currentTimeMillis();
			if (time >= state.endTime) {
				tokenMap.remove(token, state);
				res.setTime(-1);
			} else {
				var count = state.count + 1;
				if (maxCount > 0 && count >= maxCount && !tokenMap.remove(token, state))
					res.setTime(-1);
				else {
					state.count = count;
					res.setContext(state.context);
					res.setCount(count);
					res.setTime(time - state.createTime);
				}
			}
		}
		r.SendResultCode(0);
		return Procedure.Success;
	}

	private void cleanTokenMap() {
		var time = System.currentTimeMillis();
		for (var e : tokenMap.entrySet()) {
			var state = e.getValue();
			if (time >= state.endTime)
				tokenMap.remove(e.getKey(), state);
		}
	}

	public static void main(String[] args) throws Exception {
		Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
			e.printStackTrace();
			logger.error("uncaught exception in {}:", t, e);
		});

		String ip = null;
		int port = DEFAULT_PORT;
		int threadCount = 0;

		for (int i = 0; i < args.length; ++i) {
			switch (args[i]) {
			case "-ip":
				ip = args[++i];
				if (ip.isBlank())
					ip = null;
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

		logger.info("Token service start at {}:{}", ip != null ? ip : "", port);
		instance.start(ip != null ? InetAddress.getByName(ip) : null, port);
		synchronized (Thread.currentThread()) {
			Thread.currentThread().wait();
		}
	}
}
