package Zeze.Services.Log4jQuery;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import Zeze.Util.KV;
import Zeze.Util.OutLong;
import Zeze.Util.OutObject;
import Zeze.Util.Random;
import Zeze.Util.TaskSpec;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import Zeze.Util.OutInt;
import org.jetbrains.annotations.NotNull;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 日志文件集合，能搜索当前存在的所有日志。
 * 静态管理所有Log4jFile。Index。监控rotate。
 */
public class Log4jFileManager extends ReentrantLock {
	private static final @NotNull Logger logger = LogManager.getLogger(Log4jFileManager.class);

	public static class Log4jFile {
		public volatile File file;
		public final LogIndex index;

		public Log4jFile(File file, LogIndex index) {
			this.file = file;
			this.index = index;
		}

		public static Log4jFile of(File file, LogIndex index) {
			return new Log4jFile(file, index);
		}
	}

	// 持锁写（onFileCreated/buildIndex/reconcile）、无锁读（seek/size/get），用COW保证读安全。
	private final CopyOnWriteArrayList<Log4jFile> files = new CopyOnWriteArrayList<>();
	private final FileCreateDetector fileCreateDetector;
	private final String logFileBegin;
	private final String logFileEnd;
	private final LogServiceConf.LogConf logConf;
	private final Future<?> buildIndexTimer;
	// OVERFLOW触发的对账节流状态（GD-D02）：溢出语义是"可能丢失"，不拉满对账频率，窗口内重复触发不重复执行。
	private static final long RECONCILE_THROTTLE_MS = 60_000;
	private final AtomicLong lastReconcileTime = new AtomicLong();

	public Log4jFileManager(LogServiceConf.LogConf logConf) throws Exception {
		this.logConf = logConf;
		var fulls = logConf.logActive.split("\\.");
		// active名可含多个点号（如a.b.log）：begin=末段之外的全部，end=末段。
		this.logFileEnd = fulls.length > 1 ? fulls[fulls.length - 1] : "";
		this.logFileBegin = fulls.length > 1 ? String.join(".", Arrays.copyOf(fulls, fulls.length - 1)) : fulls[0];

		// OVERFLOW节流对账/监听失效最终对账的入口（GD-D02）。
		this.fileCreateDetector = new FileCreateDetector(logConf.logDir, this::onFileCreated,
				this::reconcileThrottled, this::reconcile);

		// 装载期间持有锁：onFileCreated跑在监视线程（构造即启动），不持锁装载会与其交错，
		// 产生幽灵条目或索引未随行改名；持锁后启动瞬间的create事件排队到装载完成后按序处理（FND-S3-13）。
		try {
			lock();
			try {
				loadRotates(logConf.logDir);
				var active = new File(logConf.logDir, logConf.logActive);
				if (active.exists()) {
					// 警告，如果启动的瞬间发生了log4j rotate，由于原子性没有保证，可能会创建多余的Log4jFile，
					// 搜索的时候忽略文件不存在的错误？
					// 暂时先不处理！（WatchService对rename的CREATE事件乱序时仍可能漏登新active，见挂档记录）
					files.add(Log4jFile.of(active, loadIndex(active, logConf.logActive + ".index")));
				}
			} finally {
				unlock();
			}
		} catch (Exception e) {
			// 构造失败回收detector：其线程在构造函数里已start并强引用this，不join则watch永驻半构造对象（GD-C07）。
			fileCreateDetector.stopAndJoin();
			throw e;
		}
		var period = 300_000L;
		buildIndexTimer = TaskSpec.ofAction(this::buildIndex)
				.schedulePeriodNow(Random.getInstance().nextLong(period), period);
		removeOldLinkFiles();
	}

	public Log4jFileSession seek(long time, OutInt out) throws IOException {
		return seek(time, out, null);
	}

	/**
	 * outEntry回传实际打开的条目（GD-C01）：与out.value同源捕获，walker以条目引用为可收缩列表的重定位锚点，
	 * 出参风格与get(int, OutObject)同构。
	 */
	public Log4jFileSession seek(long time, OutInt out, OutObject<Log4jFile> outEntry) throws IOException {
		for (var i = files.size() - 1; i >= 0; --i) {
			var file = files.get(i);
			if (time >= file.index.getBeginTime()) {
				var target = file.file;
				Log4jFileSession logFileSession;
				try {
					logFileSession = new Log4jFileSession(target, file.index, logConf.charsetName, logConf.logTimeFormat);
				} catch (FileNotFoundException e) {
					// 文件被外部清理（logrotate压缩/保留期删除）：跳过该条目继续更旧的，持锁摘除+warn（GD-D01）。
					removeMissingFile(file, target, e);
					continue;
				}
				out.value = i;
				if (null != outEntry)
					outEntry.value = file;
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
		return logConf.logDir;
	}

	public int testFileName(String fileName, OutLong out) {
		if (fileName.equals(getCurrentLogFileName()))
			return 0; // 当前日志文件

		// 长度门槛防御重叠名（active "zeze.log" 下 "zezelog"）substring越界：rotate名至少=begin+分隔点+end。
		if (fileName.startsWith(logFileBegin) && fileName.endsWith(logFileEnd)
				&& fileName.length() >= logFileBegin.length() + logFileEnd.length() + 1) {
			// rotate log file name = logFileBegin + logDatePattern + '.' + logFileEnd;
			// logDatePattern默认是 .yyyy-MM-dd
			var datePatternPart = fileName.substring(logFileBegin.length(), fileName.length() - logFileEnd.length() - 1);
			var formatter = new SimpleDateFormat(logConf.logDatePattern);
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
		lock();
		try {
			var fileName = path.toFile().getName();
			var type = testFileName(fileName, null);
			switch (type) {
			case 0: // current log file created
				var currentLogFileName = getCurrentLogFileName();
				if (fileName.equals(currentLogFileName)
						&& (files.isEmpty() || !files.getLast().file.getName().equals(currentLogFileName))) {
					var logFile = new File(logConf.logDir, fileName);
					files.add(Log4jFile.of(logFile, loadIndex(logFile, getCurrentIndexFileName())));
					// 登记即建硬链接，同步清理旧链接：removeOldLinkFiles只在构造期执行，不在此调用则链接随轮转累积（GD-C08）。
					removeOldLinkFiles();
				}
				break;

			case 1: // rotate target
				if (files.isEmpty())
					return;

				var last = files.getLast();
				if (last.file.getName().equals(getCurrentLogFileName())) {
					// rename index file
					var indexFile = Path.of(logConf.logDir, getCurrentIndexFileName()).toFile();
					if (indexFile.exists()) {
						if (!indexFile.renameTo(new File(logConf.logDir, fileName + ".index")))
							logger.error("rename error: {}", indexFile);
					}
					// 修改file指向新的logFile。index保持不变。
					last.file = new File(logConf.logDir, fileName);
					// 顺序无关补登（FND2-S3-5）：部分平台WatchService对rotate双CREATE事件的递交顺序
					// 不保证，新active事件先到时被case 0同名守卫跳过漏登。这里在改指后主动补登：
					// 乱序时由本分支兜底；正序时新active尚未创建或已由case 0登记，守卫去重。
					var activeName = getCurrentLogFileName();
					var activeFile = new File(logConf.logDir, activeName);
					if (activeFile.exists() && !files.getLast().file.getName().equals(activeName)) {
						files.add(Log4jFile.of(activeFile, loadIndex(activeFile, getCurrentIndexFileName())));
						removeOldLinkFiles(); // 同case 0：补登建的硬链接之后同步清理。
					}
				}
				break;
			}
		} catch (Exception ex) {
			logger.error("", ex);
		} finally {
			unlock();
		}
	}

	public void stop() {
		buildIndexTimer.cancel(false);
		fileCreateDetector.stopAndJoin();
	}

	public boolean isEmpty() {
		return files.isEmpty();
	}

	public int size() {
		return files.size();
	}

	/**
	 * 打开files[index]的文件会话。
	 * 文件被外部清理（FileNotFoundException）时跳过该条目继续（GD-D01）：持锁摘除+warn使后续条目前移，
	 * 用同一index重试即得原来的下一个文件；残余条目全部打不开时返回null（此时index已不小于files.size()，
	 * walker按遍历耗尽处理）。
	 */
	public Log4jFileSession get(int index) throws IOException {
		return get(index, null);
	}

	/**
	 * outEntry回传实际打开的条目（GD-C01）：同index重试摘除后，回传的是重试最终打开的条目——
	 * walker以此引用锚定可收缩的files列表（整型下标摘除左移后失真），出参风格与seek(time, OutInt)同构。
	 */
	public Log4jFileSession get(int index, OutObject<Log4jFile> outEntry) throws IOException {
		while (index < files.size()) {
			var file = files.get(index);
			var target = file.file;
			try {
				var session = new Log4jFileSession(target, file.index, logConf.charsetName, logConf.logTimeFormat);
				if (null != outEntry)
					outEntry.value = file;
				return session;
			} catch (FileNotFoundException e) {
				removeMissingFile(file, target, e);
			}
		}
		return null;
	}

	/**
	 * COW无锁身份查找（Log4jFile未覆写equals即引用同一性，GD-C01）：walker在hasNext入口/耗尽推进时
	 * 按条目引用重同步currentIndex。列表快照与调用方读到的一致（COW不变式）。
	 */
	public int indexOf(Log4jFile file) {
		return files.indexOf(file);
	}

	/**
	 * 条目指向的文件已被外部清理（FileNotFoundException）：持锁摘除条目并warn（GD-D01）。
	 * 持锁复核failedTarget的identity：并发轮转（onFileCreated改指新文件）后条目已指向有效文件时不摘。
	 * 摘除后文件又回来的恢复不做（罕见，记档），由对账（GD-D02）低频重扫补登。
	 */
	private void removeMissingFile(Log4jFile file, File failedTarget, FileNotFoundException cause) {
		lock();
		try {
			if (file.file == failedTarget && files.remove(file))
				logger.warn("log file missing, remove entry: {}", failedTarget, cause);
		} finally {
			unlock();
		}
	}

	/**
	 * OVERFLOW/监听失效触发的对账入口（GD-D02）：节流——窗口内重复触发不重复对账（周期任务兜底收敛）。
	 * 竞态下多执行一轮对账无害（reconcile幂等）。
	 */
	private void reconcileThrottled() {
		var now = System.currentTimeMillis();
		var last = lastReconcileTime.get();
		if (now - last < RECONCILE_THROTTLE_MS)
			return;
		if (lastReconcileTime.compareAndSet(last, now))
			reconcile();
	}

	/**
	 * 目录对账（GD-D02）：磁盘为真相源，把files收敛到与logDir一致。
	 * 消失条目摘除（与GD-D01查询路径同一形态：持锁remove+warn）；未登记的合法文件名补登
	 * （loadIndex幂等，testFileName是现成判定器）。目录不存在/不可访问时跳过并保留告警，不视为错误。
	 * 低频挂在buildIndexTimer（5分钟）上，不做独立定时器。
	 */
	private void reconcile() {		lock();
		try {
			var listFiles = new File(logConf.logDir).listFiles();
			if (null == listFiles) {
				// 目录被删除/网络盘失联（key.reset()==false的常见根因）：对账空转不视为错误，watch的error告警已在。
				logger.warn("reconcile skipped: logDir not accessible: {}", logConf.logDir);
				return;
			}

			var registered = new HashSet<String>();
			for (var file : files)
				registered.add(file.file.getName());

			var rotates = new ArrayList<KV<Long, String>>(); // 未登记的rotate文件（补登用）
			var activeOnDisk = false;
			for (var f : listFiles) {
				if (!f.isFile() || !f.getName().endsWith(".log"))
					continue;
				var date = new OutLong();
				var type = testFileName(f.getName(), date);
				if (type == 0)
					activeOnDisk = true;
				else if (type == 1 && !registered.contains(f.getName()))
					rotates.add(KV.create(date.value, f.getName()));
			}

			rotates.sort(Comparator.comparingLong(KV::getKey));
			// 漏轮转改指（GD-C03）排在摘除循环前：active条目若先被摘除（active文件已消失的变体）即失去
			// 携旧索引改指rotate的机会；改指后条目指向存在的rotate文件，摘除循环自然放行。
			repointMissedRotation(rotates);

			// 摘除消失条目：磁盘上已不存在的登记条目（.gz压缩/保留期删除无事件，只能靠重扫发现）。
			for (var file : files) {
				if (!file.file.exists()) {
					files.remove(file);
					logger.warn("log file missing (reconcile), remove entry: {}", file.file);
				}
			}

			// 补登：rotate按时间序插入到既有active条目之前（锁内active推进要求active==last；直接追加会把
			// active挤到中间，触发下方守卫把active重复登记——同一文件双条目，搜索结果重复）。
			// 补登只做头部采样（GD-D01）：GB级轮转文件的全量扫描让锁内补登分钟级、watch线程（恢复场景
			// 对账内联在其本尊上）被钉住、新CREATE事件堆积再触发OVERFLOW——"恢复动作自己制造下一轮丢失"。
			// 采样后锁内只剩列表收敛+首条记录入索引（毫秒级，与单文件体量解耦）；余量由buildIndex锁外
			// 续建通道增量补齐。两半缺一不可：只采样不续建=永久残索引、该条目查询永久线性定位。
			if (!rotates.isEmpty()) {
				var insertPos = files.size();
				for (var i = files.size() - 1; i >= 0; --i) {
					if (files.get(i).file.getName().equals(getCurrentLogFileName())) {
						insertPos = i; // active条目已登记：rotate插到它前面，保持active为last。
						break;
					}
				}
				for (var kv : rotates) {
					var logFile = new File(logConf.logDir, kv.getValue());
					files.add(insertPos++, Log4jFile.of(logFile,
							sampleIndexHead(logFile, openIndex(kv.getValue() + ".index"))));
				}
			}
			if (activeOnDisk && (files.isEmpty() || !files.getLast().file.getName().equals(getCurrentLogFileName()))) {
				var activeFile = new File(logConf.logDir, getCurrentLogFileName());
				files.add(Log4jFile.of(activeFile,
						sampleIndexHead(activeFile, openIndex(getCurrentIndexFileName()))));
			}
		} catch (Exception ex) {
			// 单轮对账失败不打断周期任务，下轮重试。
			logger.error("reconcile error", ex);
		} finally {
			unlock();
		}
	}

	/**
	 * 补登漏轮转的case-1"索引改名+条目改指"语义（GD-C03）：轮转双CREATE事件被OVERFLOW吞掉/watch失效时，
	 * 磁盘形态是"旧名消失+rotate名出现+active重建"，而既有active条目仍持旧内容的LogIndex（offset全是旧
	 * 内容的文件内位置）——旧时间窗查询命中错文件、buildIndex给旧索引续写制造新旧混合索引且错位跨重启固化。
	 * 检测：存在未登记rotate && active条目索引的末记录offset超出active文件当前长度——自洽索引的offset必落
	 * 在文件长度内，超出即索引描述的是别的内容（即最早漏登rotate承载的旧内容；空索引lowerBound返回-1恒不触发）。
	 * 处置（与onFileCreated case-1同构三步）：
	 * 1. current索引改名跟随rotate（失败即中止改指——案卷变体防御的回滚语义：继续改指会让rotate与
	 *    新active双条目共享同一索引文件交叉读写）；
	 * 2. active条目改指rotate（LogIndex对象随行，mmap按inode有效）；
	 * 3. active名留给reconcile既有守卫按新索引补登（loadIndex发现current索引已改名即全新建）。
	 * 其余漏登rotate（更晚的轮转）走常规全量补登。
	 * 限制：buildIndex已给旧索引混入新内容记录后（offset不再超长）检测不到，维持既有行为（案卷GD-C03限制条件）。
	 */
	private void repointMissedRotation(ArrayList<KV<Long, String>> rotates) {
		if (rotates.isEmpty())
			return;
		var activeName = getCurrentLogFileName();
		Log4jFile activeEntry = null;
		for (var file : files) {
			if (file.file.getName().equals(activeName)) {
				activeEntry = file;
				break;
			}
		}
		if (null == activeEntry)
			return;

		var lastOffset = activeEntry.index.lowerBound(activeEntry.index.getEndTime());
		var activeFile = new File(logConf.logDir, activeName);
		if (lastOffset <= activeFile.length()) // 文件不存在时length()==0：索引有记录即判失配，改指同样正确
			return;

		var rotateName = rotates.getFirst().getValue(); // 时间序最早的漏登rotate：active索引内容所在
		var indexFile = Path.of(logConf.logDir, getCurrentIndexFileName()).toFile();
		if (indexFile.exists() && !indexFile.renameTo(new File(logConf.logDir, rotateName + ".index"))) {
			logger.error("reconcile missed rotation: rename index fail, keep repoint aborted: {} -> {}",
					indexFile, rotateName + ".index");
			return;
		}
		logger.warn("reconcile missed rotation: repoint active entry {} -> {} with renamed index",
				activeName, rotateName);
		activeEntry.file = new File(logConf.logDir, rotateName);
		rotates.removeFirst(); // 已由改指登记，不再常规补登
	}

	private void loadRotates(String logRotateDir) throws Exception {
		var listFiles = new File(logRotateDir).listFiles();
		var rotates = new ArrayList<KV<Long, String>>();
		if (null != listFiles) {
			for (var file : listFiles) {
				if (file.isFile() && file.getName().endsWith(".log")) {
					var date = new OutLong();
					if (1 == testFileName(file.getName(), date))
						rotates.add(KV.create(date.value, file.getName()));
				}
			}
			rotates.sort(Comparator.comparingLong(KV::getKey));
			for (var kv : rotates) {
				var logFile = new File(logConf.logDir, kv.getValue());
				this.files.add(Log4jFile.of(logFile, loadIndex(logFile, kv.getValue() + ".index")));
			}
		}
	}

	private void removeOldLinkFiles() {
		var linkDir = new File(logConf.logDir, "indexLinks");
		var links = linkDir.listFiles();
		var max = 0L;
		File maxFile = null;
		if (null != links) {
			for (var link : links) {
				if (link.isDirectory())
					continue;
				var linkName = link.getName();
				final long linkValue;
				try {
					linkValue = Long.parseLong(linkName);
				} catch (NumberFormatException e) {
					continue; // 自管目录被外部污染（desktop.ini等），跳过；下方清理循环按非max删除。
				}
				if (linkValue > max) {
					max = linkValue;
					maxFile = link;
				}
			}

			for (var link : links) {
				if (link != maxFile) {
					if (!link.delete())
						logger.warn("delete link error: {}", link);
				}
			}
		}
	}

	private File nextLinkFile() throws IOException {
		var linkDir = new File(logConf.logDir, "indexLinks");
		Files.createDirectories(linkDir.toPath());
		var links = linkDir.listFiles();
		var max = 0L;
		if (null != links) {
			for (var link : links) {
				if (link.isDirectory())
					continue;
				var linkName = link.getName();
				final long linkValue;
				try {
					linkValue = Long.parseLong(linkName);
				} catch (NumberFormatException e) {
					continue; // 非数字名跳过，不参与max；抛出会让该次轮转登记失败且不再重试。
				}
				if (linkValue > max)
					max = linkValue;
			}
		}
		return new File(linkDir, String.valueOf(max + 1));
	}

	/**
	 * 打开（必要时创建）索引文件并装载LogIndex，不扫描日志文件：current索引经硬链接打开——写走
	 * 链接仍落原文件，且manager持链接引用使轮转rename后旧LogIndex的mmap按inode仍有效（改指条目随行）；
	 * 非current名直接打开（文件不存在时LogIndex构造内创建）。
	 */
	private LogIndex openIndex(String logIndexFileName) throws Exception {
		var indexFile = new File(logConf.logDir, logIndexFileName);
		if (logIndexFileName.equals(getCurrentIndexFileName())) {
			if (!indexFile.exists()) {
				Files.createFile(indexFile.toPath());
			}
			var linkFile = nextLinkFile();
			var linkPath = Files.createLink(linkFile.toPath(), indexFile.toPath());
			return new LogIndex(linkPath.toFile());
		}
		return new LogIndex(indexFile);
	}

	private LogIndex loadIndex(File logFile, String logIndexFileName) throws Exception {
		return loadIndex(logFile, openIndex(logIndexFileName));
	}

	/**
	 * 头部采样（GD-D01）：扫描到首条记录入索引即停——beginTime可用的最小充分集，不是妥协：
	 * 空索引beginTime=Long.MAX_VALUE使seek选中条件恒假（空表不能入列），首条之后任意time的
	 * 正确定位由getIndexOffset回退offset 0 + detailSeek线性推进兜底（既有行为，非新机制）。
	 * 扫描向前使首条即最早、此后不变，beginTime在COW发布前写入（安全发布）。
	 * 非空索引（既有索引的补登场景，如条目摘除后文件回归）退化为一次常规增量步进：从endTime续、
	 * 最多读一个10s窗口即得首条合格记录，追加有序不破坏二分。读取量与文件体量无关（毫秒级）。
	 */
	private LogIndex sampleIndexHead(File logFile, LogIndex index) throws Exception {
		var lastIndexTime = index.getEndTime();
		try (var log = new Log4jFileSession(logFile, null, logConf.charsetName, logConf.logTimeFormat)) {
			var offset = index.lowerBound(lastIndexTime); // 空索引=-1：seek不动作，会话停在文件头
			log.seek(offset, lastIndexTime);
			while (log.hasNext()) {
				var next = log.next();
				if (next.getTime() - lastIndexTime >= 10_000) {
					// 每10s建立一条索引；首条即最早：直接入索引即返回。
					index.addIndex(next.getTime(), next.getOffset());
					break;
				}
			}
		}
		return index;
	}

	private LogIndex loadIndex(File logFile, LogIndex index) throws Exception {
		// 索引没有建立完成的需要继续
		try (var log = new Log4jFileSession(logFile, null, logConf.charsetName, logConf.logTimeFormat)) {
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
						lastIndexTime = indexes.getLast().time;
						indexes.clear();
					}
				}
			}
			if (!indexes.isEmpty())
				index.addIndex(indexes);
		}
		return index;
	}

	private void buildIndex() {
		reconcile(); // 低频对账（GD-D02）：挂在buildIndexTimer上，先把files收敛到磁盘真相，再推进索引。
		// 锁外增量续建全部非active条目（GD-D01）：补登头部采样只保证条目可入列，余量在此收敛——
		// 该通道对补登条目此前并不存在（旧代码只推进last==当前名，而补登rotate恰插在active之前，永远轮不到），
		// 两半缺一不可。锁外正当性：loadIndex(File,LogIndex)只触碰(logFile,index)二元组、不读写files，
		// LogIndex自带rwLock（查询路径本就与其无锁并发），manager锁真正要保的只有files变更与轮转
		// "索引改名+条目改指"的串行——锁内全量扫描是历史形状，不是正确性需求；sealed rotate内容不可变、
		// 不参与改名/改指，锁外安全。新→旧序使近期时间窗最先获得精确跳转；逐条目隔离异常：单文件损坏/
		// 消失只损失该条目本轮续建，不再中止整轮（旧代码单catch包全局）。
		// COW toArray是快照语义：锁外遍历期间watch线程的并发摘除/补登不移花接木；被摘除条目的续建
		// 读已失效文件，异常由逐条目隔离吞掉，无害。
		var snapshot = files.toArray(new Log4jFile[0]);
		for (var i = snapshot.length - 1; i >= 0; --i) {
			var entry = snapshot[i];
			if (entry.file.getName().equals(getCurrentLogFileName()))
				continue; // active条目不走锁外（file为volatile，reconcile改指后此处即时可见）：见下方锁内推进。
			try {
				loadIndex(entry.file, entry.index);
			} catch (Exception ex) {
				logger.error("buildIndex entry fail: {}", entry.file, ex);
			}
		}
		// active条目维持锁内推进现状（GD-D01）：GD-C03错位检测（repointMissedRotation读endTime对照
		// 文件长度）与case-1改名/改指依赖addIndex与轮转处理同锁串行——移出锁会重建R1刚关闭的竞态窗口。
		lock();
		try {
			if (files.isEmpty())
				return;

			var last = files.getLast();
			if (!last.file.getName().equals(getCurrentLogFileName()))
				return;

			loadIndex(last.file, last.index);
		} catch (Exception ex) {
			logger.error("", ex);
		} finally {
			unlock();
		}
	}
}
