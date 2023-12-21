package Zeze.Services.Log4jQuery;

import java.io.IOException;
import java.util.List;
import java.util.Deque;
import java.util.regex.Pattern;
import Zeze.Builtin.LogService.BCondition;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class Log4jSession {
	private final Log4jFiles files;
	private long beginTime = -2; // 用来检测发现开始时间发生变化，此时需要重置并且seek。

	/**
	 * 构造一个搜索会话。
	 */
	public Log4jSession(@NotNull String logActive, @Nullable String logDir, @NotNull String datePattern) throws IOException {
		this.files = new Log4jFiles(logActive, logDir, datePattern);
	}

	public void reset() throws IOException {
		this.files.reset();
	}

	private void trySetBeginTime(long beginTime) throws IOException {
		if (beginTime == -2)
			throw new IllegalArgumentException("invalid beginTime -2");

		if (this.beginTime == beginTime)
			return;

		this.beginTime = beginTime;
		this.files.reset();
		if (beginTime != -1)
			this.files.seek(beginTime);
	}

	/**
	 * 按 string.find 方式搜索日志，结果通过 result 获取；
	 * @return true 表示还有数据没有搜索完，false 表示结束。
	 */
	public boolean searchContains(List<Log4jLog> result,
						  long beginTime, long endTime,
						  List<String> words, int containsType,
						  int limit) throws IOException {
		result.clear();
		trySetBeginTime(beginTime);

		if (limit <= 0)
			return false; // end search

		while (files.hasNext()) {
			var log = files.next();
			if (endTime != -1 && log.getTime() > endTime)
				return false; // end search

			if (containsCheck(log, words, containsType)) {
				result.add(log);
				if (--limit <= 0)
					break; // maybe remain
			}
		}

		return files.hasNext(); // remain maybe
	}

	private static boolean containsCheck(Log4jLog log, List<String> words, int containsType) {
		switch (containsType) {
		case BCondition.ContainsAll:
			return log.containsAll(words);
		case BCondition.ContainsAny:
			return log.containsAny(words);
		case BCondition.ContainsNone:
			return log.containsNone(words);
		default:
			throw new RuntimeException("unknown contains type=" + containsType);
		}
	}

	/**
	 * 按 Regex.match 方式搜索日志，结果通过 result 获取；
	 * @return true 表示还有数据没有搜索完，false 表示结束。
	 */
	public boolean searchRegex(List<Log4jLog> result,
							   long beginTime, long endTime,
							   String pattern,
							   int limit) throws IOException {
		result.clear();
		trySetBeginTime(beginTime);

		if (limit <= 0)
			return false; // end search

		while (files.hasNext()) {
			var log = files.next();
			if (endTime != -1 && log.getTime() > endTime)
				return false; // end search

			var regex = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
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

	public boolean browseContains(Deque<Log4jLog> result,
						  long beginTime, long endTime,
						  List<String> words, int containsType,
						  int limit, float offsetFactor) throws IOException {
		result.clear();
		trySetBeginTime(beginTime);

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
				if (containsCheck(log, words, containsType)) {
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

	public boolean browseRegex(Deque<Log4jLog> result,
							   long beginTime, long endTime,
							   String pattern,
							   int limit, float offsetFactor) throws IOException {
		result.clear();
		trySetBeginTime(beginTime);

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
				var regex = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
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
