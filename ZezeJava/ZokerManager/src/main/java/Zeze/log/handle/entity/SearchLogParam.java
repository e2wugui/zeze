package Zeze.log.handle.entity;

import java.text.ParseException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import Zeze.Builtin.LogService.BCondition;
import Zeze.Services.Log4jQuery.LogServiceConf;

/**
 * /api/search 与 /api/browse 的公共请求参数：数据源、时间范围、关键词、分页等及其解析。
 */
public class SearchLogParam {
	// HTTP处理器并发parse，formatter必须不可变线程安全（SimpleDateFormat共享实例会竞争错乱）。
	// 语义对齐SimpleDateFormat默认时区。
	private static final DateTimeFormatter dateFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

	private String serverName;
	private String logName;
	private boolean reset;
	private float offsetFactor;
	private int limit;
	private String beginTime;
	private String endTime;
	private String words;
	private int containsType;
	private String pattern;
	private boolean changeSession;

	/**
	 * 入口参数校验（对齐 {@code limit<=0} 的 invalid limit 形态）：containsType 枚举、
	 * words/pattern 归一后双空、（browse）offsetFactor∈[0,1)、beginTime/endTime 时间串
	 * 格式。这些参数须入口拒绝：透传时服务端回非零结果码与死会话同族（operateRecovering
	 * 误判会话级死亡，反复重建同参数重试恒失败）、静默退化为无上下文的过滤搜索、或坍缩
	 * system error。返回 null=通过，否则为 errorResult 的 desc（browse 传 true 校验
	 * offsetFactor，search 不使用该参数）。
	 */
	public String validateError(boolean browse) {
		if (containsType != BCondition.ContainsAll && containsType != BCondition.ContainsAny
				&& containsType != BCondition.ContainsNone)
			return "invalid containsType";
		if (wordsToList().isEmpty() && (pattern == null || pattern.isBlank()))
			return "empty condition";
		if (browse) {
			var factor = offsetFactor;
			if (!(factor >= 0f && factor < 1f)) // NaN 落 false 同拒
				return "invalid offsetFactor";
		}
		var timeError = timeFormatError();
		if (timeError != null)
			return timeError;
		return null;
	}

	/** beginTime/endTime 非空白时的格式预检：返回 desc（null=通过）。空串=null 语义（不限时间）。 */
	private String timeFormatError() {
		if (beginTime != null && !beginTime.isBlank() && !isParsableTime(beginTime))
			return "invalid beginTime, expect yyyy-MM-dd HH:mm:ss";
		if (endTime != null && !endTime.isBlank() && !isParsableTime(endTime))
			return "invalid endTime, expect yyyy-MM-dd HH:mm:ss";
		return null;
	}

	private static boolean isParsableTime(String time) {
		try {
			parseTime(time);
			return true;
		} catch (RuntimeException e) { // LocalDateTime.parse 的 DateTimeParseException
			return false;
		}
	}

	public String getServerName() {
		return serverName;
	}

	public void setServerName(String serverName) {
		this.serverName = serverName;
	}

	public String getLogName() {
		return logName;
	}

	public void setLogName(String logName) {
		this.logName = logName;
	}

	/**
	 * 解析查询目标日志名：显式非空白名 trim 后原样使用；缺省/空白时取部署配置
	 * （{@link LogServiceConf}，与同进程 LogService 同源 server.xml）的主 LogConf
	 * ——配置顺序的首个（单日志部署即唯一名），配置顺序即多日志形态下缺省目标的
	 * 权威定义（此前多份即拒绝，随发双 LogConf 形态的开箱查询恒拒，故改由配置顺序
	 * 定主）。零 LogConf（或程序化填 map 未记主名）无法确定默认，返回 null——调用方
	 * 以 {@link #missingLogNameDesc} 回显式错误引导显式传参（缺省 null 透传到
	 * Session 构造抛 IAE 坍缩 system error）。显式名的<b>存在性</b>不在此判
	 * （返回值语义=解析，不=校验通过），由 {@link #unknownLogNameDesc} 承担。
	 */
	public String resolveLogName(LogServiceConf deployConf) {
		if (logName != null && !logName.isBlank())
			return logName.trim();
		var names = deployConf.getLogConfs().keySet();
		if (names.isEmpty())
			return null;
		var primary = deployConf.getPrimaryLogName();
		if (primary != null && names.contains(primary))
			return primary;
		// 防御回退：程序化填 map（未走 parse）不记主名——单份无歧义取之，多份维持旧拒绝。
		return names.size() == 1 ? names.iterator().next() : null;
	}

	/**
	 * 显式 logName 的存在性校验（对称 serverName 的注册表预校验与缺省形态的
	 * {@link #missingLogNameDesc}，补两条既有防线之间的缺口）：显式非空白名不在部署配置
	 * 的 LogConf 名集（拼写错误/跨部署拷贝的请求模板）时返回 "unknown logName: X"——
	 * 透传则单服视图 Session 构造抛裸 RuntimeException 坍缩 system error、全服视图逐台
	 * 跳过成 0 成员被误报 "no reachable log server"，错误归因失真。缺省形态返回 null
	 * （默认解析的产出必在配置集内）；已知名返回 null=通过。
	 */
	public String unknownLogNameDesc(LogServiceConf deployConf) {
		if (logName == null || logName.isBlank())
			return null;
		var trimmed = logName.trim();
		return deployConf.getLogConfs().containsKey(trimmed) ? null : "unknown logName: " + trimmed;
	}

	/** 缺省 logName 无法确定默认时的 errorResult desc：零 LogConf 指向配置漏配
	 * （唯一现役形态）；多份无主名仅程序化填 map 的防御形态可达，列出日志名引导显式传参。 */
	public static String missingLogNameDesc(LogServiceConf deployConf) {
		var names = new TreeSet<>(deployConf.getLogConfs().keySet());
		if (names.isEmpty())
			return "missing logName: LogServiceConf defines no LogConf";
		return "missing logName: multiple logs " + names + " without primary, send logName explicitly";
	}

	public boolean isReset() {
		return reset;
	}

	public void setReset(boolean reset) {
		this.reset = reset;
	}

	public String getBeginTime() {
		return beginTime;
	}

	public void setBeginTime(String beginTime) {
		this.beginTime = beginTime;
	}

	public String getEndTime() {
		return endTime;
	}

	public void setEndTime(String endTime) {
		this.endTime = endTime;
	}

	public String getWords() {
		return words;
	}

	public void setWords(String words) {
		this.words = words;
	}

	public int getContainsType() {
		return containsType;
	}

	public void setContainsType(int containsType) {
		this.containsType = containsType;
	}

	public String getPattern() {
		return pattern;
	}

	public void setPattern(String pattern) {
		this.pattern = pattern;
	}

	public int getLimit() {
		return limit;
	}

	public void setLimit(int limit) {
		this.limit = limit;
	}

	public float getOffsetFactor() {
		return offsetFactor;
	}

	public void setOffsetFactor(float offsetFactor) {
		this.offsetFactor = offsetFactor;
	}

	public boolean isChangeSession() {
		return changeSession;
	}

	public void setChangeSession(boolean changeSession) {
		this.changeSession = changeSession;
	}

	/**
	 * 拆分 words 为关键词列表：每段 trim 后过滤空白段（FND30 zokermanager-03）。
	 * 前导/连续逗号产生的空段若进入 BCondition.Words，服务器字面量子串匹配
	 * {@code log.contains("")} 恒真——ContainsAny 过滤器整体旁路返回全量、
	 * ContainsNone 恒返回空。整体无有效关键词时返回空列表（服务器侧 words 空
	 * 即不过滤，既有语义）。
	 */
	public List<String> wordsToList() {
		List<String> wordList = new ArrayList<>();
		if (words == null || words.isBlank()) {
			return wordList;
		}
		for (String segment : words.split(",")) {
			String word = segment.trim();
			if (!word.isEmpty())
				wordList.add(word);
		}
		return wordList;
	}

	/**
	 * 会话身份的查询条件指纹：words（归一列表，与服务端过滤同源）、pattern、
	 * containsType、beginTime/endTime（解析值——空串与缺省归一为 -1，等值不同写法
	 * 不误判漂移）拼接的确定性串；browse 另含 offsetFactor 并以模式前缀区分
	 * （search/browse 对游标的消费形态不同，不共享会话身份）。同条件翻页指纹稳定；
	 * 须在入口校验之后调用（时间串已预检可解析）。
	 * <p>会话回执比对以它扩维（{@code LogSessionBinding.matches}）：服务端仅 beginTime
	 * 有去重哨兵（Log4jSession.trySetBeginTime），其余条件变更复用旧会话只从当前
	 * 游标向前求值——新条件在游标之前的匹配静默缺失且 200 成功；条件变即视同
	 * changeSession 关旧建新，从查询窗口头完整求值。</p>
	 */
	public String conditionFingerprint(boolean browse) throws ParseException {
		return (browse ? "browse|" : "search|")
				+ parseBeginTime() + "|" + parseEndTime() + "|" + getContainsType()
				+ "|" + wordsToList() + "|" + (getPattern() == null ? "" : getPattern())
				+ (browse ? "|" + getOffsetFactor() : "");
	}

	public long parseBeginTime() throws ParseException {
		if (beginTime == null || beginTime.isBlank()) {
			return -1;
		}
		return parseTime(beginTime);
	}

	public long parseEndTime() throws ParseException {
		if (endTime == null || endTime.isBlank()) {
			return -1;
		}
		return parseTime(endTime);
	}

	private static long parseTime(String time) {
		return LocalDateTime.parse(time, dateFormat).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
	}
}
