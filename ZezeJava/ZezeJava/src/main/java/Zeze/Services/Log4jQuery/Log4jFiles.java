package Zeze.Services.Log4jQuery;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * 日志文件集合，能搜索当前存在的所有日志。
 */
public class Log4jFiles {
	private final ArrayList<File> files = new ArrayList<>();
	private int currentIndex;
	private Log4jFile current;

	public Log4jFiles(@NotNull String logActive, @Nullable String logRotateDir) throws IOException {
		var active = new File(logActive);
		if (active.exists())
			files.add(active);
		loadRotateDir(logRotateDir);

		currentIndex = 0;
		if (!files.isEmpty())
			nextCurrent();
	}

	private void loadRotateDir(@Nullable String logRotateDir) {
		if (null == logRotateDir)
			return;

		var rotates = new File(logRotateDir).listFiles();
		if (null != rotates) {
			Arrays.sort(rotates); // todo 确认文件名是按时间顺序的。
			files.addAll(Arrays.asList(rotates)); // 这是idea推荐写法，asList需要new一个ArrayList对象，直接for ... add不需要。
		}
	}

	public void seek(long time) throws IOException {
		// todo index seek
		slowSeek(time);
	}

	private void slowSeek(long time) throws IOException {
		while (hasNext()) {
			var log = next();
			if (log.getTime() >= time)
				break;
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
		current = new Log4jFile(files.get(currentIndex));
	}

	public void close() throws IOException {
		if (null != current) {
			current.close();
			current = null;
		}
	}
}
