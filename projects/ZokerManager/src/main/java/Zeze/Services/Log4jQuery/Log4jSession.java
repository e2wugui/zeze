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

	/**
	 * 单页应答编码体量预算（FND28-L1）：对齐Net默认传输上限（SocketOptions双向2MB）的保守页
	 * 预算——取其半，为发送侧堆积留余量（TcpSocket.checkOverflow按"堆积+本包&gt;上限"整包
	 * 静默丢弃）。页内结果UTF-8字节总量超预算即以Remain=true截断返回（客户端续页，不丢不重），
	 * 单页编码体量有界、不再触顶丢页；Net层"溢出整包静默丢弃+游标已推进"的根因留越界。
	 */
	public static final int PAGE_RESULT_BYTES_BUDGET = 1024 * 1024;
	/** clampLimit推导用的保守行均值（UTF-8字节）：常规行数百字节、多行聚合（堆栈）数KB，取1KB；
	 * 真实分布由各查询循环的页字节预算兜底（条数上限只是第一道，不单独依赖均值假设）。 */
	private static final int CONSERVATIVE_AVG_LINE_BYTES = 1024;
	/** 服务端单请求limit强制上限：协议字段是客户端可控的裸int，clamp后按上限执行（超出部分静默截断）。
	 * 取值=页字节预算/保守行均值（FND28-L1）：旧值10_000在默认配置下行均~250B×数千条即编码
	 * 超2MB传输上限，应答整包被静默丢弃而服务端游标已推进——页级结果永久跳过。 */
	public static final int MAX_LIMIT = PAGE_RESULT_BYTES_BUDGET / CONSERVATIVE_AVG_LINE_BYTES;
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
	/** 单条日志参与正则判定的字符上限（log4j-03，FND26）：超长行（误打印的超大base64/JSON单行blob）
	 * 截断参与匹配——前缀参与，与预算中止的部分匹配口径一致，截断warn留痕。上限必须严格小于
	 * {@link #MAX_SCAN_REGEX_CHARS}：通读型pattern对截断视图的完整扫描也不触预算中止，保证该行
	 * 必被判定并推进（不截断时>64M字符的行每页都耗尽整页预算：中止→暂存→重判同一行→再中止，
	 * 游标永久卡死，每页固定烧满预算）。contains路径无正则预算不受影响，不截断（返回内容保持原样）。 */
	public static final int MAX_REGEX_LOG_CHARS = 8 * 1024 * 1024;

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
	// lastLogFromPending（log4j-03，FND26）：本次取出的条是否来自暂存重判——首判预算中止
	// 暂存是翻页协议的既有语义（客户端续页可换pattern/words重判，见FND24暂存交接契约）；
	// 但暂存条以整页新预算重判仍然中止=该行对当前pattern不可判定，继续暂存只会无限重演
	// 同一结局（游标永久卡死，每页固定烧满预算）——此时弃置前进。
	private boolean lastLogFromPending;
	private Log4jLog nextLog() throws IOException {
		if (null != pendingNext) {
			var pending = pendingNext;
			pendingNext = null;
			lastLogFromPending = true;
			return pending;
		}
		lastLogFromPending = false;
		return files.hasNext() ? files.next() : null;
	}

	/** UTF-8编码字节数（无分配精确计数；代理对按2×3计，≥真实的4，保守方向）。 */
	private static long utf8Length(CharSequence s) {
		long n = 0;
		for (var i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			n += c < 0x80 ? 1 : (c < 0x800 ? 2 : 3);
		}
		return n;
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
		var resultBytes = 0L; // 本页结果UTF-8字节（FND28-L1页字节预算）
		// 同searchRegex：取条统一走nextLog()——regex页预算中止的暂存条（pendingNext）必须被
		// 同会话后续的contains查询消费（Search/Browse按请求内容在words/pattern间路由，同sid
		// 交错可达），直走walker.next()会越过已取出的暂存条，该条静默漏出结果。
		try {
			while (true) {
				var log = nextLog();
				if (null == log)
					break;
				// 窗口边界逐条过滤：扫描流时间不单调（列表不变式是轮转序，非内容时间序），
				// 全局早停会谎报查完并漏读其后仍落窗口内的日志——终止只由walker耗尽/扫描
				// 预算承担。上界沿用边界条丢弃语义（不进结果不暂存）；下界兜定位之后的
				// 时间回落行（定位只保证起点>=beginTime）。窗外条计入扫描预算。
				var outOfWindow = (endTime != -1 && log.getTime() > endTime)
						|| (beginTime != -1 && log.getTime() < beginTime);
				if (outOfWindow) {
					if (++scanned >= MAX_SCAN_LOGS || (scannedBytes += log.getLog().length()) >= MAX_SCAN_BYTES)
						return true; // remain：防单请求无界扫描
					continue;
				}

				if (containsCheck(log, words, containsType)) {
					var lineBytes = utf8Length(log.getLog());
					if (lineBytes > PAGE_RESULT_BYTES_BUDGET && result.isEmpty()) {
						// 单条超整页预算（FND28-L1）：任何条数limit下都无法经传输上限送达，跳过并warn
						// （不暂存——暂存会使下一页空结果+Remain死循环）。
						logger.warn("skip single log exceeding page byte budget: {} bytes", lineBytes);
					} else if (resultBytes + lineBytes > PAGE_RESULT_BYTES_BUDGET) {
						// 页字节预算满：本条暂存下一页重取（不丢不重），Remain让客户端续页。
						pendingNext = log;
						return true; // remain
					} else {
						result.add(log);
						resultBytes += lineBytes;
						if (--limit <= 0)
							break; // maybe remain
					}
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
		var resultBytes = 0L; // 本页结果UTF-8字节（FND28-L1页字节预算）
		var regexChars = MAX_SCAN_REGEX_CHARS; // 正则预算跨行共享，在matcher内部生效（见常量注释）
		try {
			while (true) {
				var log = nextLog();
				if (null == log)
					break;
				// 同searchContains：窗口边界逐条过滤（扫描流时间不单调），终止只由walker
				// 耗尽/扫描预算承担。
				var outOfWindow = (endTime != -1 && log.getTime() > endTime)
						|| (beginTime != -1 && log.getTime() < beginTime);
				if (outOfWindow) {
					if (++scanned >= MAX_SCAN_LOGS || (scannedBytes += log.getLog().length()) >= MAX_SCAN_BYTES)
						return true; // remain：窗外条同样计入扫描预算，防单请求无界扫描
					continue;
				}

				var budget = new RegexBudget(regexInput(log), regexChars);
				var matcher = regex.matcher(budget);
				boolean matched;
				try {
					matched = matcher.find();
					regexChars = budget.remaining(); // 仅成功判定才回收剩余预算（中止路径remaining已毒化为负）
				} catch (RegexBudgetExceeded e) {
					if (!lastLogFromPending) {
						// 首判中止的本条：暂存重判（不丢不重），返回部分结果+Remain，客户端续页后
						// 预算重置（可换pattern/words重判，FND24暂存交接契约）。
						pendingNext = log;
						logger.warn("searchRegex budget exceeded: {}, return partial with remain", MAX_SCAN_REGEX_CHARS);
						return true; // remain
					}
					// 暂存重判（整页新预算）仍中止（log4j-03死循环面）：该行对当前pattern不可判定，
					// 继续暂存每页都得到同一结局——游标永久卡死。弃置该条+warn前进
					//（匹配语义=不可判定即不命中，与截断的部分参与口径一致；客户端换pattern
					// 的新查询经reset/新会话不受影响）。弃置行已烧满本页正则预算：清零使后续行
					// 本页不再判定（暂存交下页新预算），单页正则工作量恒有界。
					logger.warn("searchRegex skip unjudgeable log (re-judge with fresh budget still exceeded), logTime={}",
							log.getTime());
					matched = false;
					regexChars = 0;
				}
				if (matched) {
					var lineBytes = utf8Length(log.getLog());
					if (lineBytes > PAGE_RESULT_BYTES_BUDGET && result.isEmpty()) {
						// 同searchContains：单条超整页预算，跳过并warn（不暂存防死循环）。
						logger.warn("skip single log exceeding page byte budget: {} bytes", lineBytes);
					} else if (resultBytes + lineBytes > PAGE_RESULT_BYTES_BUDGET) {
						// 同searchContains：页字节预算满，暂存下一页重取，Remain续页。
						pendingNext = log;
						return true; // remain
					} else {
						result.add(log);
						resultBytes += lineBytes;
						if (--limit <= 0)
							break; // maybe remain
					}
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
		var resultBytes = 0L; // 当前deque内容UTF-8字节（pollFirst时扣减；FND28-L1页字节预算）
		// 同searchContains：取条统一走nextLog()消费可能的暂存条（见searchContains循环处注释）。
		try {
			while (true) {
				var log = nextLog();
				if (null == log)
					break;
				// 同searchContains：窗口边界逐条过滤（扫描流时间不单调），终止只由walker
				// 耗尽/扫描预算承担。
				var outOfWindow = (endTime != -1 && log.getTime() > endTime)
						|| (beginTime != -1 && log.getTime() < beginTime);
				if (outOfWindow) {
					if (++scanned >= MAX_SCAN_LOGS || (scannedBytes += log.getLog().length()) >= MAX_SCAN_BYTES)
						return true; // remain：窗外条同样计入扫描预算，防单请求无界扫描
					continue;
				}

				var lineBytes = utf8Length(log.getLog());
				if (lineBytes > PAGE_RESULT_BYTES_BUDGET) {
					// 单条超整页预算（FND28-L1）：任何形态都无法经传输上限送达，跳过并warn
					// （browse逐行入列，暂存会重演同样超限，不暂存防死循环）。
					logger.warn("skip single log exceeding page byte budget: {} bytes", lineBytes);
				} else if (!result.isEmpty() && resultBytes + lineBytes > PAGE_RESULT_BYTES_BUDGET) {
					// 页字节预算满：本条暂存下一页重取（不丢不重），Remain让客户端续页。
					pendingNext = log;
					return true; // remain
				} else {
					result.add(log);
					resultBytes += lineBytes;
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
						} else if (result.size() > offset) {
							resultBytes -= utf8Length(result.pollFirst().getLog()); // 只在开头保留offset数量不匹配行。
						}
					}
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
		var resultBytes = 0L; // 当前deque内容UTF-8字节（poll时扣减；FND28-L1页字节预算）
		var regexChars = MAX_SCAN_REGEX_CHARS; // 正则预算跨行共享，在matcher内部生效（见常量注释）
		try {
			while (true) {
				var log = nextLog();
				if (null == log)
					break;
				// 同searchContains：窗口边界逐条过滤（扫描流时间不单调），终止只由walker
				// 耗尽/扫描预算承担。
				var outOfWindow = (endTime != -1 && log.getTime() > endTime)
						|| (beginTime != -1 && log.getTime() < beginTime);
				if (outOfWindow) {
					if (++scanned >= MAX_SCAN_LOGS || (scannedBytes += log.getLog().length()) >= MAX_SCAN_BYTES)
						return true; // remain：窗外条同样计入扫描预算，防单请求无界扫描
					continue;
				}

				var lineBytes = utf8Length(log.getLog());
				if (lineBytes > PAGE_RESULT_BYTES_BUDGET) {
					// 同browseContains：单条超整页预算，跳过并warn（不暂存防死循环）。
					logger.warn("skip single log exceeding page byte budget: {} bytes", lineBytes);
				} else if (!result.isEmpty() && resultBytes + lineBytes > PAGE_RESULT_BYTES_BUDGET) {
					// 同browseContains：页字节预算满，暂存下一页重取，Remain续页。
					pendingNext = log;
					return true; // remain
				} else {
					result.add(log);
					resultBytes += lineBytes;
					if (locate) {
						--limit;
						if (limit <= 0)
							break;
					} else {
						var budget = new RegexBudget(regexInput(log), regexChars);
						var matcher = regex.matcher(budget);
						boolean matched;
						try {
							matched = matcher.find();
							regexChars = budget.remaining(); // 仅成功判定才回收剩余预算（中止路径remaining已毒化为负）
						} catch (RegexBudgetExceeded e) {
							result.pollLast();
							resultBytes -= lineBytes;
							if (!lastLogFromPending) {
								// 本条已add进result且未判定（locate分支不触matcher，此处必为locate==false）：
								// 移除后暂存重判（不丢不重），返回部分结果+Remain，客户端续页后预算重置
								//（可换pattern/words，FND24暂存交接契约）。
								pendingNext = log;
								logger.warn("browseRegex budget exceeded: {}, return partial with remain", MAX_SCAN_REGEX_CHARS);
								return true; // remain
							}
							// 暂存重判（整页新预算）仍中止（log4j-03死循环面，同searchRegex）：
							// 弃置前进，走matched=false的常规不匹配分支（offset窗口滑动照常）；
							// 弃置行已烧满本页正则预算，清零使后续行本页不再判定。
							logger.warn("browseRegex skip unjudgeable log (re-judge with fresh budget still exceeded), logTime={}",
									log.getTime());
							matched = false;
							regexChars = 0;
						}
						if (matched) {
							locate = true;
							limit -= result.size();
							if (limit <= 0)
								break;
						} else if (result.size() > offset) {
							resultBytes -= utf8Length(result.pollFirst().getLog()); // 只在开头保留offset数量不匹配行。
						}
					}
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

	/** 行级正则输入（log4j-03）：超长行截断为前缀参与匹配（语义见MAX_REGEX_LOG_CHARS注释）。 */
	private static CharSequence regexInput(Log4jLog log) {
		var text = log.getLog();
		if (text.length() <= MAX_REGEX_LOG_CHARS)
			return text;
		logger.warn("regex input truncated: log chars={} limit={}", text.length(), MAX_REGEX_LOG_CHARS);
		return text.subSequence(0, MAX_REGEX_LOG_CHARS);
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
