package Log4jQuery;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * 日志文件集合，能搜索当前存在的所有日志。
 */
public class Log4jFiles {
	private final ArrayList<File> files = new ArrayList<>();
	private int currentIndex;
	private Log4jFile current;

	public Log4jFiles(String logActive, String logRotateDir) {
		var active = new File(logActive);
		if (active.exists())
			files.add(active);
		loadRotateDir(logRotateDir);

		currentIndex = 0;
		if (!files.isEmpty())
			nextCurrent();
	}

	private void loadRotateDir(String logRotateDir) {
		var rotates = new File(logRotateDir).listFiles();
		if (null != rotates) {
			Arrays.sort(rotates); // todo 确认文件名是按时间顺序的。
			files.addAll(Arrays.asList(rotates));
		}
	}

	public void seek(long time) {
		// todo index seek
		slowSeek(time);
	}

	private void slowSeek(long time) {
		while (hasNext()) {
			var log = next();
			if (log.getTime() >= time)
				break;
		}
	}

	public boolean hasNext() {
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

	public Log4jLog next() {
		return current.next();
	}

	private void nextCurrent() {
		if (null != current)
			current.close();
		current = new Log4jFile(files.get(currentIndex));
	}

	public void close() {
		if (null != current) {
			current.close();
			current = null;
		}
	}
}
