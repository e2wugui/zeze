package Zeze.Services.Log4jQuery;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import Zeze.Util.BufferedRandomFile;

public class Log4jFileSession implements Closeable {
	private final File file;
	private final BufferedRandomFile randomAccessFile;
	private Log4jLog nextLog; // 下一条完整的日志。
	private Log4jLog nextNextMaybePartLog; // 下下一条日志，可能不完整。
	private final LogIndex index;

	@Override
	public String toString() {
		return file.toString();
	}

	public Log4jFileSession(File file, LogIndex index, String charsetName) throws IOException {
		this.file = file;
		this.index = index;
		this.randomAccessFile = new BufferedRandomFile(file, charsetName);
		this.nextLog = tryNext();
	}

	public File getFile() {
		return file;
	}

	public void reset() throws IOException {
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
			System.out.println(" ===================== " + offset);
			if (offset != -1)
				return offset;
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
			var log = Log4jLog.tryParse(offset, line);
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
			var log = Log4jLog.tryParse(offset, line);
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
