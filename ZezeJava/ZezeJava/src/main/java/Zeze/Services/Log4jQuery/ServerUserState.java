package Zeze.Services.Log4jQuery;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import Zeze.Services.LogService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * 服务端每连接状态：管理该连接上的 Log4jSession 表（新建/查询/关闭与空闲超龄会话惰性清理）。
 */
public class ServerUserState {
	private static final Logger logger = LogManager.getLogger(ServerUserState.class);
	private final LogService logService;
	private final ConcurrentHashMap<Long, Log4jSession> logSessions = new ConcurrentHashMap<>();

	public ServerUserState(LogService logService) {
		this.logService = logService;
	}

	public Log4jSession getLogSession(long sid) {
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
		var logSession = new Log4jSession(logService.getLogManager(logName));
		var exist = logSessions.putIfAbsent(sid, logSession);
		if (null != exist) {
			logSession.close();
			throw new IllegalArgumentException("duplicate sid=" + sid);
		}
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
		// 逐个关闭并收集异常：首个close失败中断循环会让其余会话的文件句柄泄漏（对齐客户端SessionAll.close）。
		IOException first = null;
		for (var logSession : logSessions.values()) {
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
		logSessions.clear();
		if (first != null)
			throw first;
	}
}
