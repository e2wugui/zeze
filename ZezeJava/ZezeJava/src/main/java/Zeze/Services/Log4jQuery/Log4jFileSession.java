package Zeze.Services.Log4jQuery;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import Zeze.Util.BufferedRandomFile;

/**
 * 单个日志文件的顺序读取会话：按行解析日志（多行续行聚合），支持按索引与时间定位。
 */
public class Log4jFileSession implements Closeable {
	private final File file;
	private final BufferedRandomFile randomAccessFile;
	private Log4jLog nextLog; // 下一条完整的日志。
	private Log4jLog nextNextMaybePartLog; // 下下一条日志，可能不完整。
	private final LogIndex index;
	private final String logTimeFormat;

	@Override
	public String toString() {
		return file.toString();
	}

	public Log4jFileSession(File file, LogIndex index, String charsetName, String logTimeFormat) throws IOException {
		this.file = file;
		this.index = index;
		this.randomAccessFile = new BufferedRandomFile(file, charsetName);
		this.logTimeFormat = logTimeFormat;
		this.nextLog = tryNext();
	}

	public File getFile() {
		return file;
	}

	public void reset() throws IOException {
		this.nextNextMaybePartLog = null; // 残留stash属于旧游标位置，不清会被tryNext当作第一条返回（乱序/重复/丢续行）。
		this.randomAccessFile.seek(0);
		this.nextLog = tryNext();
	}

	/**
	 * 定位到 log.time ≥ time 的日志的位置。
	 * 这里只查找一个文件。
	 *
	 * @param time seek time
	 * @return true if seek success
	 */
	public boolean seek(long time) throws IOException {
		return seek(getIndexOffset(time), time);
	}

	public boolean seek(long offset, long time) throws IOException {
		if (offset >= 0) {
			randomAccessFile.seek(offset);
			this.nextLog = tryNext();
			return detailSeek(time);
		}
		return false;
	}

	private long getIndexOffset(long time) {
		// 没有索引时，从头开始搜索。
		if (null != index) {
			var offset = index.lowerBound(time);
			if (offset != -1)
				return offset;
			// lowerBound的-1混装了"空索引"与"time超出索引末端"两种情形：
			// 后者是常态——buildIndex按5分钟周期推进，查询时间落在索引末端之后的滞后带内
			//（监控端"最近N分钟"尾窗查询恰是最高频形状），此时末记录offset是现成最佳起点，
			// 回落0等于把整个已索引区间重读一遍（GB级文件整读只为定位尾部几行，预算
			// MAX_SCAN_LOGS/BYTES只约束其后的结果循环，对seek内部扫描零约束）。
			// detailSeek从末记录向前推进到time，与乱序日志的既有容忍度一致（covered路径
			// 本来就从lowerBound记录起向前定位）；空索引（endTime=0且无记录）维持回落0。
			if (time > index.getEndTime()) {
				var last = index.lowerBound(index.getEndTime()); // 末记录（endTime即末记录时间，必命中）
				if (last != -1)
					return last;
			}
		}
		return 0;
	}

	private boolean detailSeek(long time) throws IOException {
		while (nextLog != null) {
			// detailSeek没有处理当前数据，是定位，所以需要先判断当前数据，之后才next。
			if (nextLog.getTime() >= time)
				return true;
			nextLog = tryNext();
		}
		return false;
	}

	public boolean hasNext() throws IOException {
		return nextLog != null;
	}

	public Log4jLog current() {
		return nextLog;
	}

	public Log4jLog next() throws IOException {
		var next = nextLog;
		nextLog = tryNext();
		return next;
	}

	private Log4jLog tryNextPart() throws IOException {
		String line;
		while (true) {
			var offset = randomAccessFile.getPosition();
			line = randomAccessFile.readLine();
			if (line == null)
				break;
			var log = Log4jLog.tryParse(offset, line, logTimeFormat);
			if (null == log) {
				// 不是日志起始，那么这个肯定是多行日志的孤儿，忽略。
				continue;
			}
			return log;
		}
		return null;
	}

	private Log4jLog tryNext() throws IOException {
		if (null == nextNextMaybePartLog) {
			// 第一次执行或者到达文件结尾。
			nextNextMaybePartLog = tryNextPart();
		}
		// 到达文件结尾。
		if (nextNextMaybePartLog == null)
			return null;

		var next = nextNextMaybePartLog;
		nextNextMaybePartLog = null; // 先清空，下面的循环如果发现有剩下的日志，会重新设置上。
		String line;
		while (true) {
			var offset = randomAccessFile.getPosition();
			line = randomAccessFile.readLine();
			if (line == null)
				break;
			var log = Log4jLog.tryParse(offset, line, logTimeFormat);
			if (null == log) {
				next.addLine(line);
				continue;
			}
			// 下一次将要解析日志，可能不完整。
			nextNextMaybePartLog = log;
			break;
		}
		return next;
	}

	@Override
	public void close() throws IOException {
		randomAccessFile.close();
	}
}
