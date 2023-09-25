package Log4jQuery;

import java.util.regex.Pattern;

public class Log4jSession {
	private final Log4jFiles files;
	private final long beginTime;
	private final long endTime;
	private final String pattern;
	private final Pattern regex;

	public long getBeginTime() {
		return beginTime;
	}

	public long getEndTime() {
		return endTime;
	}

	public String getPattern() {
		return pattern;
	}

	public Pattern getRegex() {
		return regex;
	}

	/**
	 * 构造一个搜索会话。
	 * @param beginTime beginTime -1 means begin of log.
	 * @param endTime endTime -1 means end of log.
	 */
	public Log4jSession(String logActive, String logRotateDir, long beginTime, long endTime, String pattern, boolean isRegex) {
		this.beginTime = beginTime;
		this.endTime = endTime;
		this.pattern = pattern;
		this.regex = isRegex ? Pattern.compile(pattern, Pattern.CASE_INSENSITIVE) : null;

		this.files = new Log4jFiles(logActive, logRotateDir);
		if (beginTime != -1)
			this.files.seek(beginTime);
	}

	public boolean search(java.util.List<Log4jLog> result, int limit) {
		if (null == regex)
			return searchContains(result, limit);
		return searchRegex(result, limit);
	}

	/**
	 * 按 string.find 方式搜索日志，结果通过 result 获取；
	 * @return true 表示还有数据没有搜索完，false 表示结束。
	 */
	private boolean searchContains(java.util.List<Log4jLog> result, int limit) {
		if (limit <= 0)
			return false; // end search

		while (files.hasNext()) {
			var log = files.next();
			if (endTime != -1 && log.getTime() > endTime)
				return false; // end search

			if (log.getLog().contains(pattern)) {
				result.add(log);
				if (--limit <= 0)
					break; // maybe remain
			}
		}

		return files.hasNext(); // remain maybe
	}

	/**
	 * 按 Regex.match 方式搜索日志，结果通过 result 获取；
	 * @return true 表示还有数据没有搜索完，false 表示结束。
	 */
	private boolean searchRegex(java.util.List<Log4jLog> result, int limit) {
		if (limit <= 0)
			return false; // end search

		while (files.hasNext()) {
			var log = files.next();
			if (endTime != -1 && log.getTime() > endTime)
				return false; // end search

			var matcher = regex.matcher(log.getLog());
			var matchFound = matcher.find();
			if (matchFound) {
				result.add(log);
				if (--limit <= 0)
					break; // maybe remain
			}
		}

		return files.hasNext(); // remain maybe
	}

	public void close() {
		files.close();
	}
}
