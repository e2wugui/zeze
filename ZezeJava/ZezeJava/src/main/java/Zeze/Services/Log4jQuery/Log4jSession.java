package Zeze.Services.Log4jQuery;

import java.io.IOException;
import java.util.List;
import java.util.Deque;
import java.util.regex.Pattern;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class Log4jSession {
	private final Log4jFiles files;
	private final long beginTime;
	private final long endTime;

	// 关键字匹配
	private final List<String> words;
	private final boolean containsAll;

	// 正则表达式匹配
	private final String pattern;
	private final Pattern regex;

	public long getBeginTime() {
		return beginTime;
	}

	public long getEndTime() {
		return endTime;
	}

	/**
	 * 构造一个搜索会话。
	 * @param beginTime beginTime -1 means begin of log.
	 * @param endTime endTime -1 means end of log.
	 */
	public Log4jSession(@NotNull String logActive, @Nullable String logRotateDir,
						long beginTime, long endTime,
						String pattern) throws IOException {
		this.beginTime = beginTime;
		this.endTime = endTime;
		this.pattern = pattern;
		this.regex = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
		this.words = null;
		this.containsAll = false;

		this.files = new Log4jFiles(logActive, logRotateDir);
		if (beginTime != -1)
			this.files.seek(beginTime);
	}

	public Log4jSession(@NotNull String logActive, @Nullable String logRotateDir,
						long beginTime, long endTime,
						List<String> words, boolean containsAll) throws IOException {
		this.beginTime = beginTime;
		this.endTime = endTime;
		this.pattern = null;
		this.regex = null;
		this.words = words;
		this.containsAll = containsAll;

		this.files = new Log4jFiles(logActive, logRotateDir);
		if (beginTime != -1)
			this.files.seek(beginTime);
	}

	public boolean search(List<Log4jLog> result, int limit) throws IOException {
		result.clear();
		if (null == regex)
			return searchContains(result, limit);
		return searchRegex(result, limit);
	}

	private boolean containsCheck(Log4jLog log) {
		if (containsAll)
			return log.containsAll(words);
		return log.containsNone(words);
	}

	/**
	 * 按 string.find 方式搜索日志，结果通过 result 获取；
	 * @return true 表示还有数据没有搜索完，false 表示结束。
	 */
	private boolean searchContains(List<Log4jLog> result, int limit) throws IOException {
		if (limit <= 0)
			return false; // end search

		while (files.hasNext()) {
			var log = files.next();
			if (endTime != -1 && log.getTime() > endTime)
				return false; // end search

			if (containsCheck(log)) {
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
	private boolean searchRegex(List<Log4jLog> result, int limit) throws IOException {
		if (limit <= 0)
			return false; // end search

		while (files.hasNext()) {
			var log = files.next();
			if (endTime != -1 && log.getTime() > endTime)
				return false; // end search

			var matcher = regex.matcher(log.getLog());
			if (matcher.find()) {
				result.add(log);
				if (--limit <= 0)
					break; // maybe remain
			}
		}

		return files.hasNext(); // remain maybe
	}

	public void close() throws IOException {
		files.close();
	}

	public boolean browse(Deque<Log4jLog> result, int limit, float offsetFactor) throws IOException {
		result.clear();
		if (null == regex)
			return browseContains(result, limit, offsetFactor);
		return browseRegex(result, limit, offsetFactor);
	}

	private boolean browseContains(Deque<Log4jLog> result, int limit, float offsetFactor) throws IOException {
		if (limit <= 0)
			return false; // end search

		var offset = (int)(limit * offsetFactor);
		if (offset >= limit)
			throw new IllegalArgumentException("offset factor too big.");

		var locate = false;
		while (files.hasNext()) {
			var log = files.next();
			if (endTime != -1 && log.getTime() > endTime)
				return false; // end search

			result.add(log);
			if (locate) {
				--limit;
				if (limit <= 0)
					break;
			} else {
				if (containsCheck(log)) {
					locate = true;
					limit -= result.size();
					if (limit <= 0)
						break;
				} else if (result.size() > offset)
					result.pollFirst(); // 只在开头保留offset数量不匹配行。
			}
		}

		return files.hasNext(); // remain maybe
	}

	private boolean browseRegex(Deque<Log4jLog> result, int limit, float offsetFactor) throws IOException {
		if (limit <= 0)
			return false; // end search

		var offset = (int)(limit * offsetFactor);
		if (offset >= limit)
			throw new IllegalArgumentException("offset factor too big.");

		var locate = false;
		while (files.hasNext()) {
			var log = files.next();
			if (endTime != -1 && log.getTime() > endTime)
				return false; // end search

			result.add(log);
			if (locate) {
				--limit;
				if (limit <= 0)
					break;
			} else {
				var matcher = regex.matcher(log.getLog());
				if (matcher.find()) {
					locate = true;
					limit -= result.size();
					if (limit <= 0)
						break;
				} else if (result.size() > offset)
					result.pollFirst(); // 只在开头保留offset数量不匹配行。
			}
		}

		return files.hasNext(); // remain maybe
	}
}
