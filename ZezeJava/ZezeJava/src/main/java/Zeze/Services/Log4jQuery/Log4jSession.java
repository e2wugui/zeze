package Zeze.Services.Log4jQuery;

import java.io.IOException;
import java.io.Serial;
import java.util.List;
import java.util.Deque;
import java.util.regex.Pattern;
import Zeze.Builtin.LogService.BCondition;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

/**
 * 服务端单份日志的查询会话：持有 Log4jFileWalker 游标，执行 contains/regex 的 search/browse，
 * 并施加 limit 与扫描预算约束。
 */
public class Log4jSession {
	private static final @NotNull Logger logger = LogManager.getLogger(Log4jSession.class);

	/** 服务端单请求limit强制上限：协议字段是客户端可控的裸int，clamp后按上限执行（超出部分静默截断）。 */
	public static final int MAX_LIMIT = 10_000;
	/** 单请求扫描日志条数预算：超预算置Remain=true提前返回，客户端按翻页协议继续，对现有客户端透明。 */
	public static final int MAX_SCAN_LOGS = 100_000;
	/** 单请求扫描字节预算：防超大日志行（多行续行聚合）绕过条数预算。 */
	public static final long MAX_SCAN_BYTES = 256L * 1024 * 1024;
	/** 单请求正则预算（matcher访问的字符数）：病态模式（嵌套量词等）对长行产生指数回溯，会话锁
	 * 与处理线程被单条find()钉住且无法从外部中断——预算必须经CharSequence.charAt在matcher
	 * 内部生效（回溯重读重复计入，这正是被约束的资源）。超限中止当次search/browse返回部分结果
	 * +Remain=true并告警一次，客户端续页（每请求预算重置，单请求工作量有界即服务可用性有界）。
	 * 量级：正常模式每字符O(1)次访问，64M字符远超常规查询窗；MAX_SCAN_BYTES满额扫描的请求
	 * 可能先触本预算而多翻一页，语义不变。 */
	public static final long MAX_SCAN_REGEX_CHARS = 64L * 1024 * 1024;

	private final Log4jFileWalker files;
	// 正则预算中止的本条日志暂存（walker.next()取出即前进、判定未完成）：下一请求以重置后的
	// 预算重新判定，不丢不重。游标重定位（reset/seek）时丢弃——重新定位后的扫描按新窗口重读。
	private Log4jLog pendingNext;
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
		resetWalker();
	}

	// 游标重定位即丢弃暂存条：它属于旧位置，重新定位后的扫描按新窗口重新读取（与等价的全新查询一致）。
	private void resetWalker() throws IOException {
		pendingNext = null;
		files.reset();
	}

	private void trySetBeginTime(long beginTime) throws IOException {
		if (beginTime == -2)
			throw new IllegalArgumentException("invalid beginTime -2");

		if (this.beginTime == beginTime)
			return;

		// 先定位后提交去重哨兵：seek链路抛IOException时哨兵未提交，客户端携带同一beginTime
		// 重试不会命中短路，必重新定位；先提交则重试从被reset归零的最旧文件头全量返回，
		// 早于beginTime的旧日志混入结果（下界过滤只靠定位保证，扫描循环无下界检查）。
		resetWalker();
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

	// 取下一条（含正则预算中止的暂存重判）：walker.next()取出即前进，预算中止的本条经暂存槽
	// 由下一请求重新判定，不丢不重。
	private Log4jLog nextLog() throws IOException {
		if (null != pendingNext) {
			var pending = pendingNext;
			pendingNext = null;
			return pending;
		}
		return files.hasNext() ? files.next() : null;
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
		// 同searchRegex：取条统一走nextLog()——regex页预算中止的暂存条（pendingNext）必须被
		// 同会话后续的contains查询消费（Search/Browse按请求内容在words/pattern间路由，同sid
		// 交错可达），直走walker.next()会越过已取出的暂存条，该条静默漏出结果。
		try {
			while (true) {
				var log = nextLog();
				if (null == log)
					break;
				if (endTime != -1 && log.getTime() > endTime) {
					// 终止判定的这条被消费但不进结果也不暂存：固定窗口翻页（查完remain=false即止）
					// 不受影响；同会话同beginTime渐进扩大endTime续窗时，该边界日志不属于任何一页。
					logger.debug("query terminated by endTime, boundary log dropped: logTime={}, endTime={}",
							log.getTime(), endTime);
					return false; // end search
				}

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
		} catch (IOException e) {
			// 页扫描中途IO失败：已消费日志与已填result的部分结果均未发出（handler上抛不发应答），
			// 客户端同beginTime重试命中去重短路会从游标当前位置续读——定位点与失败点之间的日志
			// 不属于任何一页。失效beginTime哨兵（与trySetBeginTime的seek链路失败处置同构）：
			// 重试必重定位重读，以已发页重读的重复换取不丢窗。
			this.beginTime = -2;
			throw e;
		}
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
		var regexChars = MAX_SCAN_REGEX_CHARS; // 正则预算跨行共享，在matcher内部生效（见常量注释）
		try {
			while (true) {
				var log = nextLog();
				if (null == log)
					break;
				if (endTime != -1 && log.getTime() > endTime) {
					logger.debug("query terminated by endTime, boundary log dropped: logTime={}, endTime={}",
							log.getTime(), endTime); // 同searchContains：边界条既不进结果也不暂存
					return false; // end search
				}

				var budget = new RegexBudget(log.getLog(), regexChars);
				var matcher = regex.matcher(budget);
				boolean matched;
				try {
					matched = matcher.find();
				} catch (RegexBudgetExceeded e) {
					// 判定中止的本条：暂存重判（不丢不重），返回部分结果+Remain，客户端续页后预算重置。
					pendingNext = log;
					logger.warn("searchRegex budget exceeded: {}, return partial with remain", MAX_SCAN_REGEX_CHARS);
					return true; // remain
				}
				regexChars = budget.remaining();
				if (matched) {
					result.add(log);
					if (--limit <= 0)
						break; // maybe remain
				}
				// 扫描预算：当前条已处理完毕才判预算，超预算置Remain提前返回，下一页从下一条继续（不丢不重）。
				if (++scanned >= MAX_SCAN_LOGS || (scannedBytes += log.getLog().length()) >= MAX_SCAN_BYTES)
					return true; // remain
			}

			return files.hasNext(); // remain maybe
		} catch (IOException e) {
			// 同searchContains：扫描中途IO失败失效beginTime哨兵，重试重定位重读不丢窗。
			this.beginTime = -2;
			throw e;
		}
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
		// 同searchContains：取条统一走nextLog()消费可能的暂存条（见searchContains循环处注释）。
		try {
			while (true) {
				var log = nextLog();
				if (null == log)
					break;
				if (endTime != -1 && log.getTime() > endTime) {
					logger.debug("query terminated by endTime, boundary log dropped: logTime={}, endTime={}",
							log.getTime(), endTime); // 同searchContains：边界条既不进结果也不暂存
					return false; // end search
				}

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
		} catch (IOException e) {
			// 同searchContains：扫描中途IO失败失效beginTime哨兵，重试重定位重读不丢窗。
			this.beginTime = -2;
			throw e;
		}
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
		var regexChars = MAX_SCAN_REGEX_CHARS; // 正则预算跨行共享，在matcher内部生效（见常量注释）
		try {
			while (true) {
				var log = nextLog();
				if (null == log)
					break;
				if (endTime != -1 && log.getTime() > endTime) {
					logger.debug("query terminated by endTime, boundary log dropped: logTime={}, endTime={}",
							log.getTime(), endTime); // 同searchContains：边界条既不进结果也不暂存
					return false; // end search
				}

				result.add(log);
				if (locate) {
					--limit;
					if (limit <= 0)
						break;
				} else {
					var budget = new RegexBudget(log.getLog(), regexChars);
					var matcher = regex.matcher(budget);
					boolean matched;
					try {
						matched = matcher.find();
					} catch (RegexBudgetExceeded e) {
						// 本条已add进result且未判定（locate分支不触matcher，此处必为locate==false）：
						// 移除后暂存重判（不丢不重），返回部分结果+Remain，客户端续页后预算重置。
						result.pollLast();
						pendingNext = log;
						logger.warn("browseRegex budget exceeded: {}, return partial with remain", MAX_SCAN_REGEX_CHARS);
						return true; // remain
					}
					regexChars = budget.remaining();
					if (matched) {
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
		} catch (IOException e) {
			// 同searchContains：扫描中途IO失败失效beginTime哨兵，重试重定位重读不丢窗。
			this.beginTime = -2;
			throw e;
		}
	}

	/** 预算耗尽：经charAt从matcher.find()内部抛出中止匹配——行间预算检查拦不住单条find的回溯钉住。 */
	private static final class RegexBudgetExceeded extends RuntimeException {
		@Serial
		private static final long serialVersionUID = 1L;
	}

	/** 正则预算CharSequence：regex引擎读输入只经charAt，在此计数、超限抛出中止（回溯重读重复计入）。 */
	private static final class RegexBudget implements CharSequence {
		private final CharSequence delegate;
		private long remaining;

		RegexBudget(CharSequence delegate, long budget) {
			this.delegate = delegate;
			this.remaining = budget;
		}

		long remaining() {
			return remaining;
		}

		@Override
		public char charAt(int index) {
			if (--remaining < 0)
				throw new RegexBudgetExceeded();
			return delegate.charAt(index);
		}

		@Override
		public int length() {
			return delegate.length();
		}

		@Override
		public CharSequence subSequence(int start, int end) {
			return new RegexBudget(delegate.subSequence(start, end), remaining);
		}
	}
}
