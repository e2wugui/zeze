package Zeze.log.handle.entity;

import java.text.ParseException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

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
