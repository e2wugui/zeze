package Zeze.Services.Log4jQuery;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.RandomAccessFile;

public class Log4jFile {
	private final File file;
	private final RandomAccessFile randomAccessFile;
	private BufferedReader bufferedReader;
	private Log4jLog nextLog; // 下一条完整的日志。
	private Log4jLog nextNextMaybePartLog; // 下下一条日志，可能不完整。

	public Log4jFile(File file) throws IOException {
		this.file = file;
		this.randomAccessFile = new RandomAccessFile(file, "r");
		this.bufferedReader = new BufferedReader(new FileReader(randomAccessFile.getFD()));
		this.nextLog = tryNext();
	}

	public File getFile() {
		return file;
	}

	/**
	 * 定位到 log.time >= time 的日志的位置。
	 * 这里只查找一个文件。
	 * @param time seek time
	 * @return true if seek success
	 */
	public boolean seek(long time) throws IOException {
		var offset = getIndexOffset(time);
		if (offset >= 0) {
			randomAccessFile.seek(offset);
			this.bufferedReader = new BufferedReader(new FileReader(randomAccessFile.getFD()));
			this.nextLog = tryNext();
			return detailSeek(time);
		}
		return false;
	}

	private long getIndexOffset(long time) {
		// todo index seek
		return 0;
	}

	private boolean detailSeek(long time) throws IOException {
		while (hasNext()) {
			var log = next();
			if (log.getTime() >= time)
				return true;
		}
		return false;
	}

	public boolean hasNext() throws IOException {
		return nextLog != null;
	}

	public Log4jLog next() throws IOException {
		var next = nextLog;
		nextLog = tryNext();
		return next;
	}

	private Log4jLog tryNextPart() throws IOException {
		String line;
		while ((line = bufferedReader.readLine()) != null) {
			var log = Log4jLog.tryParse(line);
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
		while ((line = bufferedReader.readLine()) != null) {
			var log = Log4jLog.tryParse(line);
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

	public void close() throws IOException {
		bufferedReader.close();
		randomAccessFile.close();
	}
}
