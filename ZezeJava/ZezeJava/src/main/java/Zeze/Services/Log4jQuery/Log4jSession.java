package Zeze.Services.Log4jQuery;

import java.io.IOException;
import java.util.List;
import java.util.Deque;
import java.util.regex.Pattern;
import Zeze.Builtin.LogService.BCondition;

/**
 * 服务端单份日志的查询会话：持有 Log4jFileWalker 游标，执行 contains/regex 的 search/browse，
 * 并施加 limit 与扫描预算约束。
 */
public class Log4jSession {
	/** 服务端单请求limit强制上限：协议字段是客户端可控的裸int，clamp后按上限执行（超出部分静默截断）。 */
	public static final int MAX_LIMIT = 10_000;
	/** 单请求扫描日志条数预算：超预算置Remain=true提前返回，客户端按翻页协议继续，对现有客户端透明。 */
	public static final int MAX_SCAN_LOGS = 100_000;
	/** 单请求扫描字节预算：防超大日志行（多行续行聚合）绕过条数预算。 */
	public static final long MAX_SCAN_BYTES = 256L * 1024 * 1024;

	private final Log4jFileWalker files;
	private long beginTime = -2; // 用来检测发现开始时间发生变化，此时需要重置并且seek。
	// 最后活动时间：Browse/Search进入会话锁后刷新，服务端据此惰性清理空闲会话。
	private volatile long lastActiveTime = System.currentTimeMillis();

	public static int clampLimit(int limit) {
		return Math.min(limit, MAX_LIMIT);
	}

	public long getLastActiveTime() {
		return lastActiveTime;
	}

	/** 在持有会话锁的查询路径入口刷新：与清理的锁内复核共同保证"查询中的会话不会过期"。 */
	public void touchActive() {
		lastActiveTime = System.currentTimeMillis();
	}

	/**
	 * 构造一个搜索会话。
	 */
	public Log4jSession(Log4jFileManager files) {
		this.files = new Log4jFileWalker(files);
	}

	public void reset() throws IOException {
		// beginTime去重状态必须随游标一起失效：reset的语义是"下一查询从头重新定位"，
		// 但定位（seek到首条time≥beginTime）只发生在trySetBeginTime里且以beginTime未变去重短路——
		// reset只归零游标不失效beginTime时，同beginTime的reset刷新请求会从最旧文件头迭代，早于
		// beginTime的日志混入结果（查询契约违反）且全历史线性重扫。失效为-2后下一查询必走
		// reset+seek重定位；beginTime=-1流程不变（-2→-1变化，reset后不seek）。
		this.beginTime = -2;
		this.files.reset();
	}

	private void trySetBeginTime(long beginTime) throws IOException {
		if (beginTime == -2)
			throw new IllegalArgumentException("invalid beginTime -2");

		if (this.beginTime == beginTime)
			return;

		// 先定位后提交去重哨兵：seek链路抛IOException时哨兵未提交，客户端携带同一beginTime
		// 重试不会命中短路，必重新定位；先提交则重试从被reset归零的最旧文件头全量返回，
		// 早于beginTime的旧日志混入结果（下界过滤只靠定位保证，扫描循环无下界检查）。
		this.files.reset();
		if (beginTime != -1) {
			try {
				this.files.seek(beginTime);
			} catch (IOException e) {
				// walker已reset而this.beginTime仍指向旧定位：下次同beginTime查询命中去重短路，
				// 从被归零的位置返回早于beginTime的旧日志。置-2（失效标记）强制下次必重定位。
				this.beginTime = -2;
				throw e;
			}
		}
		this.beginTime = beginTime;
	}

	/**
	 * 按 string.find 方式搜索日志，结果通过 result 获取；
	 *
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

		var scanned = 0;
		var scannedBytes = 0L;
		while (files.hasNext()) {
			var log = files.next();
			if (endTime != -1 && log.getTime() > endTime)
				return false; // end search

			if (containsCheck(log, words, containsType)) {
				result.add(log);
				if (--limit <= 0)
					break; // maybe remain
			}
			// 扫描预算：当前条已处理完毕才判预算，超预算置Remain提前返回，下一页从下一条继续（不丢不重）。
			if (++scanned >= MAX_SCAN_LOGS || (scannedBytes += log.getLog().length()) >= MAX_SCAN_BYTES)
				return true; // remain
		}

		return files.hasNext(); // remain maybe
	}

	private static boolean containsCheck(Log4jLog log, List<String> words, int containsType) {
		return switch (containsType) {
			case BCondition.ContainsAll -> log.containsAll(words);
			case BCondition.ContainsAny -> log.containsAny(words);
			case BCondition.ContainsNone -> log.containsNone(words);
			default -> throw new RuntimeException("unknown contains type=" + containsType);
		};
	}

	/**
	 * 按 Regex.match 方式搜索日志，结果通过 result 获取；
	 *
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

		var regex = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE); // 循环外编译一次复用
		var scanned = 0;
		var scannedBytes = 0L;
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
			// 扫描预算：当前条已处理完毕才判预算，超预算置Remain提前返回，下一页从下一条继续（不丢不重）。
			if (++scanned >= MAX_SCAN_LOGS || (scannedBytes += log.getLog().length()) >= MAX_SCAN_BYTES)
				return true; // remain
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
		var scanned = 0;
		var scannedBytes = 0L;
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
			// 扫描预算：当前条已处理完毕才判预算，超预算置Remain提前返回，下一页从下一条继续（不丢不重）。
			if (++scanned >= MAX_SCAN_LOGS || (scannedBytes += log.getLog().length()) >= MAX_SCAN_BYTES)
				return true; // remain
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
		var regex = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE); // 循环外编译一次复用
		var scanned = 0;
		var scannedBytes = 0L;
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
			// 扫描预算：当前条已处理完毕才判预算，超预算置Remain提前返回，下一页从下一条继续（不丢不重）。
			if (++scanned >= MAX_SCAN_LOGS || (scannedBytes += log.getLog().length()) >= MAX_SCAN_BYTES)
				return true; // remain
		}

		return files.hasNext(); // remain maybe
	}
}
