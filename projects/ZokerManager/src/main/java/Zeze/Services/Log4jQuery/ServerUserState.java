package Zeze.Services.Log4jQuery;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import Zeze.Services.LogService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * 服务端每连接状态：管理该连接上的 Log4jSession 表（新建/查询/关闭与空闲超龄会话惰性清理）。
 */
public class ServerUserState {
	private static final Logger logger = LogManager.getLogger(ServerUserState.class);

	// 连接关闭的会话回收执行面：进程级守护单线程。OnSocketClose 可运行在 selector 线程
	// （TcpSocket.doClose 在发起关闭线程同步回调），而逐会话 close 须与 Browse/Search 持同一
	// 会话锁互斥——慢盘/大文件下单请求扫描可达分钟级，回调线程同步等锁会把该 selector 上全部
	// 连接的 IO 一并钉停（Service.stop 锁内路径同理，框架锁序契约本就要求回调内业务锁单向不等待）。
	// 关闭只做句柄回收、无时序与结果可见性要求，投递专职线程排队执行（排队期间 fd 迟回收，无正确性影响；
	// 单线程即够——不等锁的 close 毫秒级，等锁则该会话的查询正在等价地占用工作线程，串行化只推迟
	// 回收不放大拥塞）。守护线程不参与服务生命周期：stop 不等待还卡在等锁上的回收，JVM 退出不受阻。
	private static final ExecutorService LOG_SESSION_CLOSER = Executors.newSingleThreadExecutor(r -> {
		var thread = new Thread(r, "Zeze.LogService.LogSessionCloser");
		thread.setDaemon(true);
		return thread;
	});

	private final LogService logService;
	private final ConcurrentHashMap<Long, Log4jSession> logSessions = new ConcurrentHashMap<>();
	private volatile boolean closed;

	public ServerUserState(LogService logService) {
		this.logService = logService;
	}

	public Log4jSession getLogSession(long sid) {
		if (closed)
			return null;
		var logSession = logSessions.get(sid);
		if (null != logSession)
			// 命中即锁外前置刷新（volatile写）：把"查询受理"提前到拿引用时刻。原窗口=拿引用到
			// 进会话锁（touchActive在锁内首行），含等锁——可被并发长查询钉住秒级，恰到期会话在
			// 窗口内被惰性清理回收，查询随后对已关walker抛IllegalStateException。前置刷新后
			// 清理的锁内复核读到的必是新值（volatile写先于该读发生），该形态消除。剩余窗口=map.get
			// 到本行之间被抢占且清理方完整走过锁内复核并close——纳秒级无锁窗口内的完整竞争，
			// 实际不可达；即便命中，touchActive是幂等volatile写，对已移除会话无副作用，
			// 查询失败形态与修复前相同（可见错误码、客户端可重建会话恢复）。
			logSession.touchActive();
		return logSession;
	}

	public void newLogSession(String logName, long sid) throws IOException {
		if (closed)
			throw new IllegalStateException("log connection closed");
		var logSession = new Log4jSession(logService.getLogManager(logName));
		RuntimeException rejected;
		// 只串行登记与关闭快照：构造及逐会话close都在锁外，selector不等查询锁。
		synchronized (this) {
			if (closed)
				rejected = new IllegalStateException("log connection closed");
			else if (logSessions.putIfAbsent(sid, logSession) != null)
				rejected = new IllegalArgumentException("duplicate sid=" + sid);
			else
				return;
		}
		try {
			logSession.close();
		} catch (IOException e) {
			rejected.addSuppressed(e);
		}
		throw rejected;
	}

	public void closeLogSession(long sid) throws IOException {
		var logSession = logSessions.remove(sid);
		if (null != logSession) {
			synchronized (logSession) { // 与Browse/Search按同一会话锁互斥，close不打断并发查询
				logSession.close();
			}
		}
	}

	/**
	 * 惰性清理空闲超龄会话：NewSession/查询路径顺带调用，不做定期任务、不做数量上限。
	 * 复用closeLogSession的按会话锁互斥范式：锁前先查lastActiveTime（正在查询的会话进锁首行已刷新，
	 * 不阻塞不清理）；锁内复核——"查询中的会话不会过期"。
	 * 单个会话close失败只warn不中断（会话已从map移除，失败只影响该会话的句柄释放）。
	 */
	public void cleanIdleLogSessions(long idleTimeoutMillis) {
		if (idleTimeoutMillis <= 0)
			return; // 禁用

		for (var e : logSessions.entrySet()) {
			var logSession = e.getValue();
			if (System.currentTimeMillis() - logSession.getLastActiveTime() < idleTimeoutMillis)
				continue; // 活跃会话不动
			//noinspection SynchronizationOnLocalVariableOrMethodParameter
			synchronized (logSession) {
				if (System.currentTimeMillis() - logSession.getLastActiveTime() < idleTimeoutMillis)
					continue; // 锁内复核：等锁期间刚被并发查询刷新
				if (logSessions.remove(e.getKey(), logSession)) {
					try {
						logSession.close();
					} catch (IOException ex) {
						logger.warn("close idle log session fail, sid={}", e.getKey(), ex);
					}
				}
			}
		}
	}

	public void close() throws IOException {
		// 摘除后关闭全部快照内会话并收集异常（首个close失败不中断循环，否则其余会话句柄泄漏，
		// 对齐客户端SessionAll.close），全部处理完统一抛首个异常。
		var first = closeSessions(detachSessions());
		if (first != null)
			throw first;
	}

	/**
	 * OnSocketClose 专用关闭入口：会话表摘除在调用线程同步完成（连接已死，逻辑上立即不可再查询，
	 * 惰性清理/迟到 getLogSession 随即看到空表），逐会话按会话锁互斥的物理 close 投递专职守护线程
	 * ——回调线程（可能是 selector）不得同步等会话锁（等锁语义见 LOG_SESSION_CLOSER 注释）。
	 * close 失败无处上抛（回调上游只记日志不补调），在 closer 线程 warn（对齐 cleanIdleLogSessions）。
	 */
	public void closeAsync() {
		var pending = detachSessions();
		if (pending.isEmpty())
			return; // 无会话不投递：closer线程零任务，也不为空连接排队
		LOG_SESSION_CLOSER.execute(() -> {
			var first = closeSessions(pending);
			if (first != null)
				logger.warn("close log sessions after socket close fail", first);
		});
	}

	/** 摘除全部会话（快照+清空）：返回的快照由调用方负责逐个关闭。 */
	private synchronized List<Log4jSession> detachSessions() {
		closed = true; // 即使空表也保留终态：迟到Normal派发请求不得重新登记。
		var pending = List.copyOf(logSessions.values());
		logSessions.clear();
		return pending;
	}

	/**
	 * 逐会话按会话锁互斥关闭（与 Browse/Search 同锁序，close 不打断并发查询），异常收集不中断：
	 * @return 首个 IOException（其余 addSuppressed），null=全部成功。
	 */
	private static IOException closeSessions(List<Log4jSession> sessions) {
		IOException first = null;
		for (var logSession : sessions) {
			try {
				//noinspection SynchronizationOnLocalVariableOrMethodParameter
				synchronized (logSession) {
					logSession.close();
				}
			} catch (IOException e) {
				if (first == null)
					first = e;
				else
					first.addSuppressed(e);
			}
		}
		return first;
	}
}
