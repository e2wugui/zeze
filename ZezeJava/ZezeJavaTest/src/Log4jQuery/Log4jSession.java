package Log4jQuery;

import java.io.File;

public class Log4jSession {
	private final Log4jFile file;

	public Log4jSession(File file) {
		this.file = new Log4jFile(file);
	}

	public boolean begin(long beginTime) {
		return file.seek(beginTime);
	}

	/**
	 * 结果通过 result 获取；
	 * @return true 表示还有数据没有搜索完，false 表示结束。
	 */
	public boolean searchContains(String contains, long endTime, java.util.List<Log4jLog> result, int limit) {
		if (limit <= 0)
			return false; // end search

		while (file.hasNext()) {
			var log = file.next();
			if (log.getTime() > endTime)
				return false; // end search

			if (log.getLog().contains(contains)) {
				result.add(log);
				if (--limit <= 0)
					break; // maybe remain
			}
		}

		return file.hasNext(); // remain maybe
	}

	public boolean searchRegex(String regex, long endTime, java.util.List<Log4jLog> result, int limit) {
		throw new UnsupportedOperationException();
	}
}
