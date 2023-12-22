package Zeze.Services.Log4jQuery;

import java.io.IOException;
import Zeze.Util.OutInt;

/**
 * 日志文件集合，能搜索当前存在的所有日志。
 * 用于Log4jFileSession搜索，具有局部状态。
 */
public class Log4jFileWalker {
	private final Log4jFileManager files;
	private int currentIndex = -1;
	private Log4jFileSession current;

	public Log4jFileWalker(Log4jFileManager files) throws IOException {
		this.files = files;
	}

	public void reset() throws IOException {
		if (currentIndex == 0)
			current.reset();
		else {
			currentIndex = 0;
			if (!files.isEmpty())
				nextCurrent();
		}
	}

	public void seek(long time) throws IOException {
		var out = new OutInt();
		var log4jFileSession = files.seek(time, out);
		if (null == log4jFileSession) {
			slowSeek(time);
			return;
		}

		currentIndex = out.value;
		if (null != current)
			current.close();
		current = log4jFileSession;
	}

	private void slowSeek(long time) throws IOException {
		while (hasNext()) {
			var log = current.current();
			if (log.getTime() >= time)
				break;
			next();
		}
	}

	public boolean hasNext() throws IOException {
		// 循环写法，可以跳过空文件。
		while (currentIndex < files.size()) {
			var hasNext = current.hasNext();
			if (hasNext)
				return true;
			++ currentIndex;
			if (currentIndex < files.size())
				nextCurrent();
		}
		return false;
	}

	public Log4jLog next() throws IOException {
		return current.next();
	}

	private void nextCurrent() throws IOException {
		if (null != current)
			current.close();
		current = files.get(currentIndex);
	}

	public void close() {
		// walker 目前不需要实现关闭。
	}
}
