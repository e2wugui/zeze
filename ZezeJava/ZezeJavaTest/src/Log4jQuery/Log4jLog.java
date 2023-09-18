package Log4jQuery;

public class Log4jLog {
	private final long time;
	private final String log;

	public Log4jLog(long time, String log) {
		this.time = time;
		this.log = log;
	}

	public long getTime() {
		return time;
	}

	public String getLog() {
		return log;
	}
}
