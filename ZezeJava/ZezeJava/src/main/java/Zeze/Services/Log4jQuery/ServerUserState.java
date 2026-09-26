package Zeze.Services.Log4jQuery;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import Zeze.Services.LogService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ServerUserState {
	private static final Logger logger = LogManager.getLogger(ServerUserState.class);
	private final LogService logService;
	private final ConcurrentHashMap<Long, Log4jSession> logSessions = new ConcurrentHashMap<>();

	public ServerUserState(LogService logService) {
		this.logService = logService;
	}

	public Log4jSession getLogSession(long sid) {
		return logSessions.get(sid);
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
			synchronized (logSession) { // 与Browse/Search按同一会话锁互斥，close不再打断并发查询（FND-S3-9）
				logSession.close();
			}
		}
	}

	/**
	 * 惰性清理空闲超龄会话（GD-D03）：NewSession/查询路径顺带调用，不做定期任务、不做数量上限。
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
