package Zeze.Services.Log4jQuery;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

public class ServerUserState {
	private final LogServiceConf conf;
	private final ConcurrentHashMap<Long, Log4jSession> logSessions = new ConcurrentHashMap<>();

	public ServerUserState(LogServiceConf conf) {
		this.conf = conf;
	}

	public Log4jSession getLogSession(long sid) {
		return logSessions.get(sid);
	}

	public void newLogSession(long sid) throws IOException {
		var logSession = new Log4jSession(conf.logActive, conf.logDir, conf.logDatePattern);
		var exist = logSessions.putIfAbsent(sid, logSession);
		if (null != exist) {
			logSession.close();
			throw new IllegalArgumentException("duplicate sid=" + sid);
		}
	}

	public void closeLogSession(long sid) throws IOException {
		var logSession = logSessions.remove(sid);
		if (null != logSession)
			logSession.close();
	}

	public void close() throws IOException {
		for (var logSession : logSessions.values())
			logSession.close();
	}
}
