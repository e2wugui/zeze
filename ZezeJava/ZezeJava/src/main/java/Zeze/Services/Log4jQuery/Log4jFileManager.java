package Zeze.Services.Log4jQuery;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import Zeze.Util.KV;
import Zeze.Util.OutLong;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import Zeze.Util.OutInt;

/**
 * 日志文件集合，能搜索当前存在的所有日志。
 * 静态管理所有Log4jFile。Index。监控rotate。
 */
public class Log4jFileManager {
	public static class Log4jFile {
		public final File file;
		public volatile LogIndex index;

		public Log4jFile(File file, LogIndex index) {
			this.file = file;
			this.index = index;
		}

		public static Log4jFile of(File file, LogIndex index) {
			return new Log4jFile(file, index);
		}
	}

	private final ArrayList<Log4jFile> files = new ArrayList<>();
	private static final Logger logger = LogManager.getLogger();
	private final FileCreateDetector fileCreateDetector;
	private final String logFileBegin;
	private final String logFileEnd;
	private final String logDir;
	private final String logDatePattern;
	private final String charsetName;

	public Log4jFileManager(@NotNull String logActive,
							@NotNull String logDir,
							@NotNull String datePattern,
							@NotNull String charsetName) throws Exception {
		var fulls = logActive.split("\\.");
		this.logFileBegin = fulls[0];
		this.logFileEnd = fulls.length > 1 ? fulls[1] : "";
		this.logDir = logDir;
		this.logDatePattern = datePattern;
		this.charsetName = charsetName;

		this.fileCreateDetector = new FileCreateDetector(logDir, this::onFileCreated);

		loadRotates(logDir);
		var active = new File(logDir, logActive);
		if (active.exists()) {
			// 警告，如果启动的瞬间发生了log4j rotate，由于原子性没有保证，可能会创建多余的Log4jFile，
			// 搜索的时候忽略文件不存在的错误？
			// 暂时先不处理！
			files.add(Log4jFile.of(active, loadIndex(active,logActive + ".index")));
		}
	}

	public Log4jFileSession seek(long time, OutInt out) throws IOException {
		for (var i = files.size() - 1; i >= 0; --i) {
			var file = files.get(i);
			if (time >= file.index.getBeginTime()) {
				out.value = i;
				var logFileSession = new Log4jFileSession(file.file, file.index, charsetName);
				logFileSession.seek(time);
				return logFileSession;
			}
		}
		return null;
	}

	public String getCurrentLogFileName() {
		return logFileBegin + "." + logFileEnd;
	}

	public String getCurrentIndexFileName() {
		return getCurrentLogFileName() + ".index";
	}

	public String getLogDir() {
		return logDir;
	}

	public int testFileName(String fileName, OutLong out) {
		if (fileName.equals(getCurrentLogFileName()))
			return 0; // 当前日志文件

		if (fileName.startsWith(logFileBegin) && fileName.endsWith(logFileEnd)) {
			// rotate log file name = logFileBegin + logDatePattern + '.' + logFileEnd;
			// logDatePattern默认是 .yyyy-MM-dd
			var datePatternPart = fileName.substring(logFileBegin.length(), fileName.length() - logFileEnd.length() - 1);
			var formatter = new SimpleDateFormat(logDatePattern);
			try {
				var date = formatter.parse(datePatternPart);
				if (null != out)
					out.value = date.getTime();
				return 1; // 是rotate出来的日志文件。
			} catch (ParseException e) {
				// skip and continue
			}
		}
		return -1; // 其他。
	}

	private void onFileCreated(Path path) {
		var fileName = path.toFile().getName();
		var type = testFileName(fileName, null);
		switch (type) {
		case 0: // current log file created
			break;

		case 1: // rotate target
			var indexFile = Path.of(logDir, getCurrentIndexFileName()).toFile();
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

	public boolean isEmpty() {
		return files.isEmpty();
	}

	public int size() {
		return files.size();
	}

	public Log4jFileSession get(int index) throws IOException {
		var file = files.get(index);
		return new Log4jFileSession(file.file, file.index, charsetName);
	}

	private void loadRotates(String logRotateDir) throws Exception {
		var listFiles = new File(logRotateDir).listFiles();
		var rotates = new ArrayList<KV<Long, String>>();
		if (null != listFiles) {
			for (var file : listFiles) {
				if (file.isFile()) {
					var date = new OutLong();
					if (1 == testFileName(file.getName(), date))
						rotates.add(KV.create(date.value, file.getName()));
				}
			}
			rotates.sort(Comparator.comparingLong(KV::getKey));
			for (var kv : rotates) {
				var logFile = new File(logDir, kv.getValue());
				this.files.add(Log4jFile.of(logFile, loadIndex(logFile, kv.getValue() + ".index")));
			}
		}
	}

	private LogIndex loadIndex(File logFile, String logIndexFileName) throws Exception {
		var indexFile = new File(logDir, logIndexFileName);
		var index = new LogIndex(indexFile);
		// 索引没有建立完成的需要继续
		try (var log = new Log4jFileSession(logFile, null, charsetName)) {
			var indexes = new ArrayList<LogIndex.Record>();
			var lastIndexTime = index.getEndTime();
			var offset = index.lowerBound(lastIndexTime);
			log.seek(offset, lastIndexTime);
			while (log.hasNext()) {
				var next = log.next();
				if (next.getTime() - lastIndexTime >= 10_000) {
					// 每10s建立一条索引。
					indexes.add(LogIndex.Record.of(next.getTime(), next.getOffset()));
					if (indexes.size() >= 100) {
						index.addIndex(indexes);
						lastIndexTime = indexes.get(indexes.size() - 1).time;
						indexes.clear();
					}
				}
			}
			if (!indexes.isEmpty())
				index.addIndex(indexes);
		}
		return index;
	}
}
