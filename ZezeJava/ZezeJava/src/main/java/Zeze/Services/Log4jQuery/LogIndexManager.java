package Zeze.Services.Log4jQuery;

import java.io.IOException;
import java.nio.file.Path;
import org.jetbrains.annotations.NotNull;

/**
 * 管理LogIndex：打开，创建，重命名
 */
public class LogIndexManager {
	private final FileCreateDetector fileCreateDetector;
	private final String logFileBegin;
	private final String logFileEnd;
	private final String logDatePattern;

	public LogIndexManager(@NotNull String logFileName, @NotNull String logDir, @NotNull String logDatePattern) throws IOException {
		var fulls = logFileName.split("\\.");
		this.logFileBegin = fulls[0];
		this.logFileEnd = fulls.length > 1 ? fulls[1] : "";
		this.logDatePattern = logDatePattern;
		this.fileCreateDetector = new FileCreateDetector(logDir, this::onFileCreated);
	}

	public int testFileName(String fileName) {
		if (fileName.equals(logFileBegin + "." + logFileEnd)) // 当前日志文件
			return 0;

		if (fileName.startsWith(logFileBegin) && fileName.endsWith(logFileEnd)) {
			// rotate log file name = logFileBegin + logDatePattern + '.' + logFileEnd;
			// 其中logDatePattern默认是 .yyyy-MM-dd
			// todo 怎么检测当前文件名符合上面的格式？模式匹配？
			var datePatternPart = fileName.substring(logFileBegin.length(),
					fileName.length() - logFileBegin.length() - logFileEnd.length());

			return 1; // 是rotate出来的日志文件。
		}
		return -1; // 其他。
	}

	private void onFileCreated(Path path) {
		var type = testFileName(path.toFile().getName());
		switch (type) {
		case 0: // current log file created
			break;

		case 1: // rotate target
			break;
		}
	}

	public void stop() {
		fileCreateDetector.stopAndJoin();
	}
}
