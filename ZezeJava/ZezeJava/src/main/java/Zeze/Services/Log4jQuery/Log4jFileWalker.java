package Zeze.Services.Log4jQuery;

import java.io.IOException;
import java.util.Objects;
import Zeze.Util.OutInt;
import org.jetbrains.annotations.NotNull;

/**
 * 日志文件集合，能搜索当前存在的所有日志。
 * 用于Log4jFileSession搜索，具有局部状态。
 */
public class Log4jFileWalker {
	private final @NotNull Log4jFileManager files;
	private int currentIndex = -1;
	private Log4jFileSession current;

	public Log4jFileWalker(@NotNull Log4jFileManager files) throws IOException {
		Objects.requireNonNull(files);
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
		if (log4jFileSession == null) {
			slowSeek(time);
			return;
		}

		currentIndex = out.value;
		if (current != null)
			current.close();
		current = log4jFileSession;
	}

	private void slowSeek(long time) throws IOException {
		while (hasNext() && current.current().getTime() < time)
			next();
	}

	public boolean hasNext() throws IOException {
		// 循环写法，可以跳过空文件。
		while (currentIndex < files.size()) {
			if (current.hasNext())
				return true;
			if (++currentIndex < files.size())
				nextCurrent();
		}
		return false;
	}

	public Log4jLog next() throws IOException {
		return current.next();
	}

	private void nextCurrent() throws IOException {
		if (current != null)
			current.close();
		current = files.get(currentIndex);
	}

	public void close() {
		// walker 目前不需要实现关闭。
	}
}
