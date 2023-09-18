package Log4jQuery;

import java.io.File;

public class Log4jFile {
	private final File file;

	public Log4jFile(File file) {
		this.file = file;
	}

	public File getFile() {
		return file;
	}

	public boolean seek(long time) {
		return true;
	}

	public boolean hasNext() {
		return false;
	}

	public Log4jLog next() {
		return new Log4jLog(0, "");
	}
}
