package Zeze.Services.Log4jQuery;

import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 单条日志：首行解析出的时间与文件内偏移，多行续行聚合拼接（完整文本惰性生成）。
 */
public class Log4jLog extends ReentrantLock {
	private final long time;
	private final long offset;
	private volatile String log;
	private final StringBuilder lines = new StringBuilder();

	public Log4jLog(long time, long offset, String line) {
		this.time = time;
		this.offset = offset;
		this.lines.append(line);
	}

	public void addLine(String line) {
		this.lines.append("\n").append(line);
	}

	public long getTime() {
		return time;
	}
	public long getOffset() {
		return offset;
	}

	public String getLog() {
		var tmp = log;
		if (null != tmp)
			return tmp;

		lock();
		try {
			log = lines.toString();
			return log;
		} finally {
			unlock();
		}
	}

	public boolean containsAll(List<String> words) {
		var log = getLog();
		for (var word : words) {
			if (!log.contains(word))
				return false;
		}
		return true;
	}

	public boolean containsAny(List<String> words) {
		var log = getLog();
		for (var word : words) {
			if (log.contains(word))
				return true;
		}
		return false;
	}

	public boolean containsNone(List<String> words) {
		var log = getLog();
		for (var word : words) {
			if (log.contains(word))
				return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return getLog();
	}

	// 时间格式随LogConf按份持有（Log4jFileSession.logTimeFormat），多份日志配置可各自不同。

	public static Log4jLog tryParse(long offset, String line, String logTimeFormat) {
		var dayOffset = line.indexOf(' ');
		if (dayOffset > 0) {
			var timeOffset = line.indexOf(' ', dayOffset + 1);
			if (timeOffset > 0) {
				var strTime = line.substring(0, timeOffset);
				var parsePosition = new ParsePosition(0);
				var simpleDateFormat = new SimpleDateFormat(logTimeFormat);
				var date = simpleDateFormat.parse(strTime, parsePosition);
				if (null != date && parsePosition.getErrorIndex() == -1) {
					return new Log4jLog(date.getTime(), offset, line);
				}
			}
		}
		return null;
	}
}
