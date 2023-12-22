package Zeze.Services.Log4jQuery;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

/**
 * 管理LogIndex：打开，创建，重命名
 */
public class LogIndexManager {
	private static final Logger logger = LogManager.getLogger();
	private final FileCreateDetector fileCreateDetector;
	private final String logFileBegin;
	private final String logFileEnd;
	private final String logDir;
	private final String logDatePattern;

	public LogIndexManager(@NotNull String logFileName, @NotNull String logDir, @NotNull String logDatePattern) throws IOException {
		var fulls = logFileName.split("\\.");
		this.logFileBegin = fulls[0];
		this.logFileEnd = fulls.length > 1 ? fulls[1] : "";
		this.logDir = logDir;
		this.logDatePattern = logDatePattern;
		this.fileCreateDetector = new FileCreateDetector(logDir, this::onFileCreated);
	}

	public String getLogFileName() {
		return logFileBegin + "." + logFileEnd;
	}

	public String getIndexFileName() {
		return getLogFileName() + ".index";
	}

	public String getLogDir() {
		return logDir;
	}

	public int testFileName(String fileName) {
		if (fileName.equals(getLogFileName()))
			return 0; // 当前日志文件

		if (fileName.startsWith(logFileBegin) && fileName.endsWith(logFileEnd)) {
			// rotate log file name = logFileBegin + logDatePattern + '.' + logFileEnd;
			// logDatePattern默认是 .yyyy-MM-dd
			var datePatternPart = fileName.substring(logFileBegin.length(), fileName.length() - logFileEnd.length() - 1);
			var formatter = new SimpleDateFormat(logDatePattern);
			try {
				formatter.parse(datePatternPart);
				return 1; // 是rotate出来的日志文件。
			} catch (ParseException e) {
				// skip and continue
			}
		}
		return -1; // 其他。
	}

	private void onFileCreated(Path path) {
		var fileName = path.toFile().getName();
		var type = testFileName(fileName);
		switch (type) {
		case 0: // current log file created
			break;

		case 1: // rotate target
			var indexFile = Path.of(logDir, getIndexFileName()).toFile();
			if (indexFile.exists()) {
				if (!indexFile.renameTo(new File(logDir, fileName + ".index")))
					logger.error("rename error. " + indexFile);
			}
			break;
		}
	}

	public void stop() {
		fileCreateDetector.stopAndJoin();
	}
}
