package Zeze.Services.Log4jQuery;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.ParsePosition;
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
		// 轮转移交的复制接管路径（transferIndexToRotate链接失败回退）与file成对更换实例：
		// volatile使无锁查询路径（seek/get/buildIndex快照）即时读到新实例，旧实例随引用释放
		// （映射由GC回收）。链接接管路径条目保持原实例，赋值no-op。
		public volatile LogIndex index;

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
	// OVERFLOW触发的对账节流状态：溢出语义是"可能丢失"，不拉满对账频率，窗口内重复触发不重复执行。
	private static final long RECONCILE_THROTTLE_MS = 60_000;
	private final AtomicLong lastReconcileTime = new AtomicLong();

	public Log4jFileManager(LogServiceConf.LogConf logConf) throws Exception {
		this.logConf = logConf;
		var fulls = logConf.logActive.split("\\.");
		// active名可含多个点号（如a.b.log）：begin=末段之外的全部，end=末段；
		// 单段名（无点号，如zeze）end=""，rotate名=begin+日期模式（无尾部分隔点），
		// 名字生成与匹配（getCurrentLogFileName/testFileName）都按此形态工作。
		this.logFileEnd = fulls.length > 1 ? fulls[fulls.length - 1] : "";
		this.logFileBegin = fulls.length > 1 ? String.join(".", Arrays.copyOf(fulls, fulls.length - 1)) : fulls[0];

		// OVERFLOW节流对账/监听失效最终对账的入口。
		// 构造只注册监视：watch必须晚于装载启动（见下方start调用处注释），
		// 抢先消费会在files装载前走早退分支跳过索引移交。
		this.fileCreateDetector = new FileCreateDetector(logConf.logDir, this::onFileCreated,
				this::reconcileThrottled, this::reconcile);

		// 装载持锁：loadRotates/addByContentTime按持锁契约调用；装载完成start后，
		// onFileCreated（监视线程）与reconcile同以此锁为串行点，交错会产生幽灵条目或索引未随行移交。
		try {
			lock();
			try {
				loadRotates(logConf.logDir);
				var active = new File(logConf.logDir, logConf.logActive);
				if (active.exists()) {
					// 警告，如果启动的瞬间发生了log4j rotate，由于原子性没有保证，可能会创建多余的Log4jFile。
					// WatchService对rename的CREATE事件递交乱序时仍可能漏登新active。
					// 索引解析含配对校验（openActiveIndexAtLoad）：停机期/装载前轮转留下的旧内容
					// 索引不会被配给新active（否则错配终态无修复路径——rotate已登记，repoint永不触发）。
					files.add(Log4jFile.of(active, loadIndex(active, openActiveIndexAtLoad(active))));
				}
			} finally {
				unlock();
			}
		} catch (Exception e) {
			// 构造失败回收detector：关闭watchService（close幂等）；此时未start、无线程可join，stopAndJoin立即返回。
			fileCreateDetector.stopAndJoin();
			throw e;
		}
		// watch必须晚于装载启动：装载前消费CREATE(rotate)会走files.isEmpty()早退分支，跳过
		// 索引移交（transferIndexToRotate），装载随即把旧内容索引配给新active且无修复路径（rotate
		// 已登记，repointMissedRotation空表早退）。装载完成（锁内loadRotates/loadIndex全部结束、
		// 锁已释放）后启动：排队事件按序补处理，files已非空走完整case-1；装载完成到start之间发生
		// 的轮转由5分钟reconcile兜底（未登记rotate走repointMissedRotation补移交+改指；
		// 装载前已完成的轮转由openActiveIndexAtLoad配对校验兜住）。
		fileCreateDetector.start();
		var period = 300_000L;
		buildIndexTimer = TaskSpec.ofAction(this::buildIndex)
				.schedulePeriodNow(Random.getInstance().nextLong(period), period);
		// 持锁调用：与onFileCreated同一串行点，构造尾锁外调用与
		// 并发轮转的链接清理/登记交错时存活句柄计算可读到中间态。
		lock();
		try {
			removeOldLinkFiles();
		} finally {
			unlock();
		}
	}

	public Log4jFileSession seek(long time, OutInt out) throws IOException {
		return seek(time, out, null);
	}

	/**
	 * outEntry回传实际打开的条目：与out.value同源捕获，walker以条目引用为可收缩列表的重定位锚点，
	 * 出参风格与get(int, OutObject)同构。
	 */
	public Log4jFileSession seek(long time, OutInt out, OutObject<Log4jFile> outEntry) throws IOException {
		// 无锁读：迭代期间列表可被并发摘除收缩（reconcile整批/removeMissingFile），i只减不增但
		// 上界须每轮复查——i落在收缩后size之外时COW的get越界抛未检查异常。越界退出=遍历耗尽，
		// 与walker的while(currentIndex<size)/get的上界检查同构，返回null由walker走slowSeek线性兜底。
		for (var i = files.size() - 1; i >= 0 && i < files.size(); --i) {
			var file = files.get(i);
			if (time >= file.index.getBeginTime()) {
				var target = file.file;
				Log4jFileSession logFileSession;
				try {
					logFileSession = new Log4jFileSession(target, file.index, logConf.charsetName, logConf.logTimeFormat);
				} catch (FileNotFoundException e) {
					// 文件被外部清理（logrotate压缩/保留期删除）：跳过该条目继续更旧的，持锁摘除+warn；
					// 轮转宽限未摘除时同样continue降级——active条目留给case-1/repointMissedRotation改指。
					removeMissingFile(file, target, e);
					continue;
				}
				out.value = i;
				if (null != outEntry)
					outEntry.value = file;
				try {
					logFileSession.seek(time);
				} catch (IOException e) {
					// 已构造的会话（RAF已打开）在定位失败时必须关闭，否则fd只能等GC兜底回收
					try {
						logFileSession.close();
					} catch (IOException closeEx) {
						e.addSuppressed(closeEx);
					}
					throw e;
				}
				return logFileSession;
			}
		}
		return null;
	}

	public String getCurrentLogFileName() {
		// logFileEnd为空（单段名）时不拼分隔点："zeze."与磁盘名"zeze"永不相等，
		// active条目的登记/改指/补登匹配全部失配。
		return logFileEnd.isEmpty() ? logFileBegin : logFileBegin + "." + logFileEnd;
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

		// rotate名后缀=分隔点+end（单段名end为空则无后缀，日期模式自带前导点）。
		var suffixLen = logFileEnd.isEmpty() ? 0 : logFileEnd.length() + 1;
		// 长度门槛防御重叠名（active "zeze.log" 下 "zezelog"）substring越界：rotate名至少=begin+1字符日期段。
		if (fileName.startsWith(logFileBegin) && fileName.endsWith(logFileEnd)
				&& fileName.length() >= logFileBegin.length() + suffixLen + 1) {
			// rotate log file name = logFileBegin + logDatePattern + '.' + logFileEnd;
			// logDatePattern默认是 .yyyy-MM-dd
			var datePatternPart = fileName.substring(logFileBegin.length(), fileName.length() - suffixLen);
			var formatter = new SimpleDateFormat(logConf.logDatePattern);
			formatter.setLenient(false); // lenient归一化越界字段，非日期数字串也可被静默接受
			var parsePosition = new ParsePosition(0);
			var date = formatter.parse(datePatternPart, parsePosition);
			// 日期段必须被完整消费：前缀可解析即通过会接受带垃圾尾部的文件名
			//（名字日期与内容时序不一致的乱序名可达面扩大）。
			if (null != date && parsePosition.getErrorIndex() == -1
					&& parsePosition.getIndex() == datePatternPart.length()) {
				if (null != out)
					out.value = date.getTime();
				return 1; // 是rotate出来的日志文件。
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
					// 运行期active索引一律新建（openFreshActiveIndex不复用current.index名字），
					// 头部采样给beginTime使seek可选中条目，余量由buildIndex增量补齐。
					files.add(Log4jFile.of(logFile, sampleIndexHead(logFile, openFreshActiveIndex())));
					// 登记即建索引文件，同步清理旧链接：removeOldLinkFiles只在构造期执行，不在此调用则链接随轮转累积。
					removeOldLinkFiles();
				}
				break;

			case 1: // rotate target
				if (files.isEmpty())
					return;

				var last = files.getLast();
				if (last.file.getName().equals(getCurrentLogFileName())) {
					// 索引移交而非改名：active条目的索引被存活mmap持有（经indexLinks链接映射），
					// Windows对该inode的rename/delete必败（旧方案renameCurrentIndexTo在Windows上
					// 自首次轮转起即断裂）。改为rotate名下链接接管承载inode（条目实例/增长通道不变，
					// 链接不可行时退回复制+换新实例），任何路径不再对存活mmap的inode做rename/delete。
					// 移交失败即中止改指与补登（回滚语义，与repointMissedRotation共用helper）：
					// 失败后继续会让rotate条目与补登的active条目错配内容。中止后条目仍指current名，
					// 由下一轮reconcile摘除+常规补登收敛（配对重新正确）。
					var rotateIndex = transferIndexToRotate(last.index, fileName);
					if (null == rotateIndex)
						return;
					// 修改file指向新的logFile；index随移交设定（链接接管=原实例，复制接管=新实例）。
					last.index = rotateIndex;
					last.file = new File(logConf.logDir, fileName);
					// 顺序无关补登：部分平台WatchService对rotate双CREATE事件的递交顺序
					// 不保证，新active事件先到时被case 0同名守卫跳过漏登。这里在改指后主动补登：
					// 乱序时由本分支兜底；正序时新active尚未创建或已由case 0登记，守卫去重。
					var activeName = getCurrentLogFileName();
					var activeFile = new File(logConf.logDir, activeName);
					if (activeFile.exists() && !files.getLast().file.getName().equals(activeName)) {
						files.add(Log4jFile.of(activeFile, sampleIndexHead(activeFile, openFreshActiveIndex())));
						removeOldLinkFiles(); // 同case 0：新建索引链接之后同步清理。
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
	 * 文件被外部清理（FileNotFoundException）时跳过该条目继续：持锁摘除+warn使后续条目前移，
	 * 用同一index重试即得原来的下一个文件；残余条目全部打不开时返回null（此时index已不小于files.size()，
	 * walker按遍历耗尽处理）。
	 */
	public Log4jFileSession get(int index) throws IOException {
		return get(index, null);
	}

	/**
	 * outEntry回传实际打开的条目：同index重试摘除后，回传的是重试最终打开的条目——
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
				// 摘除或并发改指后同index重试：摘除左移得到原后继、改指后重试开新目标；
				// 轮转宽限保留且仍指向失败目标时同index重试必然再FNFE——前移下标跳过，
				// 残余打不开由耗尽收尾（null）。
				if (!removeMissingFile(file, target, e) && file.file == target)
					++index;
			}
		}
		return null;
	}

	/**
	 * COW无锁身份查找（Log4jFile未覆写equals即引用同一性）：walker在hasNext入口/耗尽推进时
	 * 按条目引用重同步currentIndex。列表快照与调用方读到的一致（COW不变式）。
	 */
	public int indexOf(Log4jFile file) {
		return files.indexOf(file);
	}

	/**
	 * 条目指向的文件已被外部清理（FileNotFoundException）：持锁摘除条目并warn。
	 * 持锁复核failedTarget的identity：并发轮转（onFileCreated改指新文件）后条目已指向有效文件时不摘。
 * 轮转宽限：active名条目 + 磁盘存在未登记rotate = 轮转进行中的磁盘证据——log4j轮转
 * 先rename旧内容到rotate名、后重建active，两步之间active路径短暂不存在；此窗口内摘除active条目
 * 会使case-1守卫（last==current名）落空，"索引移交+改指"整体跳过——rotate无既有索引承接
 * （常规补登全新建，正确但多一轮全量重建）。宽限保留条目，交给case-1/repointMissedRotation改指；
	 * 条目指名不存在的文件只影响选中它的查询降级continue（无崩溃），rotate登记后宽限自然解除。
	 * 摘除后文件又回来的恢复不做（罕见），由对账低频重扫补登。
	 * @return 是否实际摘除（未摘除时get的同index重试须防自旋）。
	 */
	private boolean removeMissingFile(Log4jFile file, File failedTarget, FileNotFoundException cause) {
		lock();
		try {
			if (file.file != failedTarget)
				return false; // 并发轮转已改指：条目现指有效文件，不摘
			if (file.file.getName().equals(getCurrentLogFileName()) && hasUnregisteredRotateOnDisk()) {
				// 宽限可观测：正常轮转窗口毫秒级即收敛，本告警持续出现即宽限滞留
				// （active真被外部删除+未登记rotate长期补登受阻）——区分"轮转进行中"与"无限期滞留"
				// 的最低成本手段，滞留条目只造成查询降级，但不可静默。
				logger.warn("log file missing but rotation unconverged, keep entry: {}", failedTarget);
				return false; // 轮转进行中：active条目是case-1改指的载体，不摘
			}
			if (files.remove(file)) {
				logger.warn("log file missing, remove entry: {}", failedTarget, cause);
				return true;
			}
			return false;
		} finally {
			unlock();
		}
	}

	/**
	 * 磁盘上是否存在未登记的rotate名（须持manager锁调用）：即"轮转正在进行"的磁盘证据，
	 * 供removeMissingFile/reconcile摘除循环对active名条目宽限判据。目录不可访问时查无证据，
	 * 维持原摘除语义。
	 */
	private boolean hasUnregisteredRotateOnDisk() {
		var listFiles = new File(logConf.logDir).listFiles();
		if (null == listFiles)
			return false;
		var registered = new HashSet<String>();
		for (var file : files)
			registered.add(file.file.getName());
		for (var f : listFiles) {
			if (!f.isFile())
				continue; // 名字判定唯一权威是testFileName，不做预过滤
			if (1 == testFileName(f.getName(), null) && !registered.contains(f.getName()))
				return true;
		}
		return false;
	}

	/**
	 * OVERFLOW/监听失效触发的对账入口：节流——窗口内重复触发不重复对账（周期任务兜底收敛）。
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
	 * 目录对账：磁盘为真相源，把files收敛到与logDir一致。
	 * 消失条目摘除（与查询路径同一形态：持锁remove+warn）；未登记的合法文件名补登
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
				if (!f.isFile())
					continue; // 同hasUnregisteredRotateOnDisk：名字判定归testFileName
				var date = new OutLong();
				var type = testFileName(f.getName(), date);
				if (type == 0)
					activeOnDisk = true;
				else if (type == 1 && !registered.contains(f.getName()))
					rotates.add(KV.create(date.value, f.getName()));
			}

			rotates.sort(Comparator.comparingLong(KV::getKey));
			// 漏轮转改指排在摘除循环前：active条目若先被摘除（active文件已消失的变体）即失去
			// 携旧索引改指rotate的机会；改指后条目指向存在的rotate文件，摘除循环自然放行。
			repointMissedRotation(rotates);

			// 摘除消失条目：磁盘上已不存在的登记条目（.gz压缩/保留期删除无事件，只能靠重扫发现）。
			// active名条目轮转宽限：repointMissedRotation移交失败中止时条目仍指
			// current名且文件不存在，但它是下轮改指重试的载体——磁盘有未登记rotate即轮转未收敛的证据，
			// 不摘；rotate常规补登登记后宽限自然解除（下轮若文件仍缺失则摘）。
			for (var file : files) {
				if (!file.file.exists()) {
					if (file.file.getName().equals(getCurrentLogFileName()) && hasUnregisteredRotateOnDisk()) {
						// 宽限可观测：同removeMissingFile——5min周期下持续出现本告警
						// 即宽限滞留形态（active真删+rotate补登持续失败），需人工介入。
						logger.warn("log file missing (reconcile) but rotation unconverged, keep entry: {}", file.file);
						continue; // 轮转进行中：active条目是改指载体，不摘
					}
					files.remove(file);
					logger.warn("log file missing (reconcile), remove entry: {}", file.file);
				}
			}

			// 补登：按内容时间归位插入（addByContentTime），不按文件名日期整块插到active之前——
			// 名字日期与内容时序不一致（时钟回拨/人工拷入）时整块插入会打破列表内容时序不变式，
			// endTime提前终止+seek选择据不变式工作，错位条目整文件漏读。active恒为last不变
			//（锁内active推进要求active==last；直接追加会把active挤到中间，触发下方守卫把
			// active重复登记——同一文件双条目，搜索结果重复）。
			// 补登只做头部采样：GB级轮转文件的全量扫描让锁内补登分钟级、watch线程（恢复场景
			// 对账内联在其本尊上）被钉住、新CREATE事件堆积再触发OVERFLOW——"恢复动作自己制造下一轮丢失"。
			// 采样后锁内只剩列表收敛+首条记录入索引（毫秒级，与单文件体量解耦）；余量由buildIndex锁外
			// 续建通道增量补齐。两半缺一不可：只采样不续建=永久残索引、该条目查询永久线性定位。
			if (!rotates.isEmpty()) {
				for (var kv : rotates) {
					var logFile = new File(logConf.logDir, kv.getValue());
					addByContentTime(Log4jFile.of(logFile,
							sampleIndexHead(logFile, openRotateIndex(logFile))));
				}
			}
			if (activeOnDisk && (files.isEmpty() || !files.getLast().file.getName().equals(getCurrentLogFileName()))) {
				var activeFile = new File(logConf.logDir, getCurrentLogFileName());
				// 运行期active索引一律新建（openFreshActiveIndex），不复用current.index——
				// 它可能仍指向旧轮转世代的内容（Windows下被存活mmap钉住不可换绑）。
				files.add(Log4jFile.of(activeFile,
						sampleIndexHead(activeFile, openFreshActiveIndex())));
			}
		} catch (Exception ex) {
			// 单轮对账失败不打断周期任务，下轮重试。
			logger.error("reconcile error", ex);
		} finally {
			unlock();
		}
	}

	/**
	 * 补登漏轮转的case-1"索引移交+条目改指"语义：轮转双CREATE事件被OVERFLOW吞掉/watch失效时，
	 * 磁盘形态是"旧名消失+rotate名出现+active重建"，而既有active条目仍持旧内容的LogIndex（offset全是旧
	 * 内容的文件内位置）——旧时间窗查询命中错文件、buildIndex给旧索引续写制造新旧混合索引且错位跨重启固化。
	 * 检测：存在未登记rotate && active条目索引的末记录offset超出active文件当前长度——自洽索引的offset必落
	 * 在文件长度内，超出即索引描述的是别的内容（即最早漏登rotate承载的旧内容；空索引lowerBound返回-1恒不触发）。
	 * 处置（与onFileCreated case-1同构三步）：
	 * 1. 既有索引移交rotate名（transferIndexToRotate：链接接管承载inode、条目实例保持，链接
	 *    不可行退回复制+新实例——不rename被存活mmap持有的inode；失败即中止改指，回滚语义与
	 *    case-1共用）；
	 * 2. active条目改指rotate（index随移交设定：链接接管=原实例）；
	 * 3. active名留给reconcile既有守卫补登（openFreshActiveIndex全新建）。
	 * 其余漏登rotate（更晚的轮转）走常规全量补登。
	 * 限制：buildIndex已给旧索引混入新内容记录后（offset不再超长）检测不到，维持既有行为。
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
		if (lastOffset <= activeFile.length()) // 文件不存在时length()==0：索引有记录即判失配——失配只证明索引与active不配，配给谁由下方内容抽查裁决
			return;

		var rotateName = rotates.getFirst().getValue(); // 时间序最早的漏登rotate：active索引内容所在
		// 内容配对抽查：lastOffset超长+未登记rotate都是推断，在"active真被外部
		// 误删+磁盘恰有无关rotate名文件（人工拷入/误放/上轮未收敛残留）"叠加形态下双双失真——
		// 直接移交会把现存索引错挂到无关文件名上（不可逆且无告警：错配.index跨重启经补登挂载，
		// 该rotate自身时间窗永久不可查）。读rotate首条日志，时间落在索引时间窗内才认定配对
		//（真漏轮转/mv型归档形态下rotate首条=索引首条，恒配对，收敛行为不变）；窗外或不可读=
		// 证据不足不移交不改指，留给摘除循环宽限+rotate常规补登（全新索引正确配对）收敛。
		if (!matchRotateHead(new File(logConf.logDir, rotateName), activeEntry.index))
			return;
		var rotateIndex = transferIndexToRotate(activeEntry.index, rotateName);
		if (null == rotateIndex) // 失败即中止改指（回滚语义，与case-1共用）
			return;
		logger.warn("reconcile missed rotation: repoint active entry {} -> {} with transferred index",
				activeName, rotateName);
		activeEntry.index = rotateIndex;
		activeEntry.file = new File(logConf.logDir, rotateName);
		rotates.removeFirst(); // 已由改指登记，不再常规补登
	}

	/**
	 * 移交前内容配对抽查：读rotate文件首条可解析日志，时间落在既有索引
	 * [beginTime,endTime]窗内即认可"rotate承载的正是索引描述的内容"。真漏轮转/mv型归档形态下
	 * rotate首条=索引首条（同内容）恒配对；无关文件首条时间在窗外即否决。不可读/无日志=证据不足
	 * 同样否决——移交不可逆，宁可留给摘除+常规补登收敛（补登建全新索引，正确性无损只多一轮）。
	 */
	private boolean matchRotateHead(File rotateFile, LogIndex index) {
		var headTime = headTimeOf(rotateFile);
		return null != headTime && headTime >= index.getBeginTime() && headTime <= index.getEndTime();
	}

	/** 文件首条可解析日志的时间（不可读/无日志=null）：移交配对抽查与装载期active配对校验共用。 */
	private Long headTimeOf(File logFile) {
		try (var log = new Log4jFileSession(logFile, null, logConf.charsetName, logConf.logTimeFormat)) {
			if (!log.hasNext())
				return null;
			return log.next().getTime();
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * 轮转索引移交（case-1与repointMissedRotation共用）：把active条目索引的承载inode以rotate名
	 * 接管——首选createLink(R.index, 条目增长通道)：零复制、条目LogIndex实例与mmap/增长通道
	 * 原样保持（R.index与条目链接同体，后续续建自动落R.index，重启装载直开即全量记录；单一
	 * 事实源、无孤儿inode）。不对任何存活mmap持有的inode做rename/delete（Windows必败，与
	 * removeOldLinkFiles注释的删除必败同机制；createLink是元数据操作，跨入口rename经本仓
	 * jshell实证可行，链接推演同可行——但对"正被同路径mmap持有"的文件无JDK文档保证）：
	 * 链接失败退回确定可行的复制接管（rotate名下新建实例批量复制快照，记录量=索引条数、每10s
	 * 一条、成本低；代价：旧实例移交后失引用、映射由GC/cleaner异步释放（Java无显式unmap），
	 * 其残留链接由removeOldLinkFiles在映射释放后收敛，Windows下收敛前逐次删除告警）。
	 * rotate.index已存在（装载/对账抢先登记，或上轮残留）时不覆盖：返回null由调用方中止，
	 * 留给装载校验/对账常规补登收敛（覆盖会销毁既有内容且Windows不可行）。
	 * 失败必须让调用方中止后续"条目改指+active补登"——继续会让rotate条目与补登的active条目
	 * 错配内容。单一实现收口，防止两条路径的回滚语义漂移。
	 * @return 条目应持有的索引实例（链接接管=原实例原样返回；复制接管=新实例，调用方移交）；
	 *         null=不可移交（调用方须中止）。
	 */
	private LogIndex transferIndexToRotate(LogIndex activeIndex, String rotateFileName) {
		var rotateIndexFile = new File(logConf.logDir, rotateFileName + ".index");
		if (rotateIndexFile.exists()) {
			// 装载/对账已抢先登记该rotate（如装载后才消费的排队CREATE事件）：本通道不覆盖既有
			// 文件（覆盖即销毁既有内容，Windows亦不可行），中止后既有登记按各自配对继续工作。
			logger.error("transfer index fail, rotation repoint aborted, rotate index exists: {}", rotateIndexFile);
			return null;
		}
		try {
			Files.createLink(rotateIndexFile.toPath(), activeIndex.getFile().toPath());
			return activeIndex;
		} catch (FileAlreadyExistsException e) { // exists()检查与链接之间被外部并发抢占：按既存中止
			logger.error("transfer index fail, rotation repoint aborted, rotate index exists: {}", rotateIndexFile);
			return null;
		} catch (IOException linkEx) {
			try {
				var rotateIndex = new LogIndex(rotateIndexFile); // 新文件+新实例（构造内创建文件）
				rotateIndex.addIndex(activeIndex.snapshotRecords());
				logger.warn("transfer index by copy, link takeover fail: {}", linkEx.toString());
				return rotateIndex;
			} catch (Exception copyEx) {
				// 半途文件尽力清理（新实例已失引用；Windows下若映射尚未被GC释放则删失败留待装载期处理）
				if (!rotateIndexFile.delete())
					logger.warn("transfer index fail, cleanup stale file: {}", rotateIndexFile);
				logger.error("transfer index fail, rotation repoint aborted: {}", rotateIndexFile, copyEx);
				return null;
			}
		}
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
				var logFile = new File(logConf.logDir, kv.getValue());
				// 启动装载同样按内容时间归位（addByContentTime）：名字日期与内容时序不一致时
				// 按名序追加会从构造起就打破列表时序不变式（reconcile期间无补登可纠正）。
				addByContentTime(Log4jFile.of(logFile, loadIndex(logFile, openRotateIndex(logFile))));
			}
		}
	}

	/**
	 * 按内容时间归位插入条目（持manager锁调用）：列表不变式=内容时间旧→新——walker顺序遍历、
	 * endTime提前终止（time>endTime即停）与seek从尾向头选条目都以它为前提。插入点=内容时间
	 * （索引beginTime，即首条记录时间）不晚于新条目的最后一个既有条目之后；active条目恒为last
	 * （锁内推进要求active==last），其后区间不可插入。空索引（beginTime=MAX_VALUE，无合格记录）
	 * 时间不可知，插在active之前保持轮转序。
	 */
	private void addByContentTime(Log4jFile entry) {
		var activePos = -1;
		for (var i = files.size() - 1; i >= 0; --i) {
			if (files.get(i).file.getName().equals(getCurrentLogFileName())) {
				activePos = i;
				break;
			}
		}
		var limit = activePos >= 0 ? activePos : files.size(); // 插入上界：active之前
		var insertPos = 0;
		for (var i = limit - 1; i >= 0; --i) {
			if (files.get(i).index.getBeginTime() <= entry.index.getBeginTime()) {
				insertPos = i + 1;
				break;
			}
		}
		files.add(insertPos, entry);
	}

	private void removeOldLinkFiles() {
		var linkDir = new File(logConf.logDir, "indexLinks");
		// 存活条目mmap持有的链接：链接是LogIndex的增长通道（addIndex按链接路径
		// 重开文件扩映射）——不检查持有就删，Linux下条目尾部索引续建必FNFE（每5min ERROR无限重试，
		// 该窗口索引永久缺失），Windows下映射钉住删除必败（链接累积未消除+逐链接warn）。
		// 持锁调用（构造/onFileCreated），files快照与条目生命周期一致；条目被retention/reconcile摘除后
		// 其LogIndex无引用、映射可被GC释放，链接下次清理即可删——累积从无界收敛为与保留窗口内条目同阶。
		var heldLinks = new HashSet<Path>();
		for (var file : files) {
			var indexFile = file.index.getFile();
			if (indexFile != null && linkDir.equals(indexFile.getParentFile()))
				heldLinks.add(indexFile.toPath());
		}
		var links = linkDir.listFiles();
		var maxFile = maxLinkFile(); // max=active增长通道，永不删；轮转移交后旧实例的链接随映射释放收敛
		if (null != links) {
			for (var link : links) {
				if (link != maxFile) {
					if (heldLinks.contains(link.toPath())) {
						logger.debug("skip live index link: {}", link); // 存活句柄链接，随条目生命周期清理
						continue;
					}
					if (!link.delete())
						logger.warn("delete link error: {}", link);
				}
			}
		}
	}

	/**
	 * indexLinks下最大编号文件（无则null）：装载期active候选解析与nextLinkFile分配共用。
	 * 不变量：active索引通道恒为max——装载解析接受max候选、轮转/补登新建取max+1，编号单调递增。
	 */
	private File maxLinkFile() {
		var links = new File(logConf.logDir, "indexLinks").listFiles();
		File maxFile = null;
		var max = 0L;
		if (null != links) {
			for (var link : links) {
				if (link.isDirectory())
					continue;
				final long linkValue;
				try {
					linkValue = Long.parseLong(link.getName());
				} catch (NumberFormatException e) {
					continue; // 自管目录被外部污染（desktop.ini等），跳过，不参与max。
				}
				if (linkValue > max) {
					max = linkValue;
					maxFile = link;
				}
			}
		}
		return maxFile;
	}

	private File nextLinkFile() throws IOException {
		var linkDir = new File(logConf.logDir, "indexLinks");
		Files.createDirectories(linkDir.toPath());
		var max = maxLinkFile();
		return new File(linkDir, String.valueOf((null == max ? 0 : Long.parseLong(max.getName())) + 1));
	}

	/**
	 * 打开（必要时创建）rotate名索引并装载LogIndex，不扫描日志文件：直接以rotate名打开
	 * （文件不存在时LogIndex构造内创建）。旧代码对current名的硬链接特殊处理已随轮转方案移除
	 * （见openActiveIndexAtLoad/openFreshActiveIndex——运行期不再使用current.index名字）。
	 * 内容配对校验（装载loadRotates/对账补登共用收口，判据与active侧indexPairsLogFile同构）：
	 * 磁盘上的rotate名.index可能是陈旧残留（外部清理只删.log、logDatePattern无年份的年度同名
	 * 重现、备份回拷同名不同内容），其time+offset描述的是别的内容——直接装载后loadIndex/
	 * sampleIndexHead以陈旧末记录续建：offset超长则seek落EOF续建静默停止（索引永久陈旧），
	 * 落在文件内则从错位位置续读，该rotate时间窗错读或漏读，且重启走同一无校验路径不能自愈。
	 * 失配按"陈旧残留"处置——删除让位后按原名重建（装载期无存活映射删除必成；对账期可能被
	 * 本进程早前实例的未释放mmap钉住（Windows），此时不删不覆盖——对齐transferIndexToRotate
	 * "不动既有文件"的语义，改在indexLinks下全新索引由buildIndex重建，残留文件原地不动）。
	 * 校验经readIndexHeadTail直读（不经LogIndex实例化）：候选文件不会被mmap钉住，删除路径可行。
	 */
	private LogIndex openRotateIndex(File rotateFile) throws Exception {
		var indexFile = new File(logConf.logDir, rotateFile.getName() + ".index");
		if (indexFile.exists() && !indexPairsLogFile(indexFile, rotateFile)) {
			logger.warn("rotate index not paired with log, drop stale: {}", indexFile);
			if (!indexFile.delete()) {
				logger.warn("drop stale rotate index fail (pinned?), rebuild with fresh link index: {}", indexFile);
				return openFreshActiveIndex();
			}
		}
		return new LogIndex(indexFile);
	}

	/**
	 * 运行期为active新建空索引：独立新inode于indexLinks/(max+1)（LogIndex构造内创建文件）。
	 * 不再使用current.index名字：运行期它可能仍指向旧轮转世代的内容——Windows下该inode被存活
	 * mmap钉住，rename/delete/truncate皆不可为（轮转方案改"重建映射"后，运行期无任何路径能把该
	 * 名字换绑到新inode，复用即错配）；Linux下虽可为，两端统一行为不复用。
	 */
	private LogIndex openFreshActiveIndex() throws Exception {
		return new LogIndex(nextLinkFile());
	}

	/**
	 * 装载期为active解析索引（进程刚启动、无存活映射——rename/delete/truncate/链接均可为，
	 * 这是装载期独有、运行期不再有的窗口）。按序校验配对并择一：
	 * 1) current.index：旧代码磁盘形态/无轮转会话留下的交接名；
	 * 2) indexLinks最大编号：本方案运行期active索引的常驻位置（每次轮转openFreshActiveIndex
	 *    取max+1新建inode，编号单调递增，max即最近世代）；
	 * 3) 全新空索引（loadIndex全量重建，正确性无损只多一轮扫描）。
	 * 校验修复"停机期/装载前轮转"的错配终态（装载自身把旧内容索引配给新active、且rotate已登记
	 * 使repointMissedRotation永不触发——"重启自愈"原不成立）：错配候选被否决，active换正确索引。
	 * 被否决的current.index当场删除（其内容若仍有效必另有承载名：rotate名.index或indexLinks
	 * 链接，删名不删内容）；解析完成后把current.index重建为指向胜出inode的链接——磁盘惯例
	 * "<active>.index随active存在"（旧代码磁盘形态、运维检视、装载候选1的自举来源）。运行期
	 * 轮转不维护该名字：被存活mmap持有的inode不可换绑（Windows），轮转后它滞后一代（指向
	 * 旧内容索引，仍有rotate名承载正确内容），下次装载在此重建。
	 */
	private LogIndex openActiveIndexAtLoad(File activeFile) throws Exception {
		var currentIndexFile = new File(logConf.logDir, getCurrentIndexFileName());
		if (currentIndexFile.exists()) {
			if (indexPairsLogFile(currentIndexFile, activeFile)) {
				// 配对成功：链接接管（装载期无存活映射），沿用"链接是存活索引增长通道"的既有机制。
				var linkFile = nextLinkFile();
				try {
					return new LogIndex(Files.createLink(linkFile.toPath(), currentIndexFile.toPath()).toFile());
				} catch (IOException linkEx) {
					// FAT/exFAT等不支持硬链接的文件系统上createLink抛IOException：不回退则异常
					// 上抛令LogService构造失败、服务起不来（配置本身合法）。降级为直接以
					// current.index为增长通道（对齐transferIndexToRotate链接失败降级+warn的语义）：
					// 原名增长与链接增长等价，后续轮转对它的移交走复制接管、新active走indexLinks
					// 全新文件，同样不依赖链接能力。
					logger.warn("load: link takeover fail, use current index directly: {}", currentIndexFile, linkEx);
					return new LogIndex(currentIndexFile);
				}
			}
			logger.warn("load: current index not paired with active, drop stale: {}", currentIndexFile);
			if (!currentIndexFile.delete())
				logger.warn("load: drop stale current index fail: {}", currentIndexFile);
		}
		LogIndex index;
		var maxLink = maxLinkFile();
		if (null != maxLink && indexPairsLogFile(maxLink, activeFile))
			index = new LogIndex(maxLink);
		else
			index = new LogIndex(nextLinkFile());
		if (!currentIndexFile.exists()) {
			// best-effort重建交接名（候选1被否决删除/首次运行）：失败仅warn，索引通道不受影响。
			try {
				Files.createLink(currentIndexFile.toPath(), index.getFile().toPath());
			} catch (IOException e) {
				logger.warn("load: relink current index fail: {}", currentIndexFile, e);
			}
		}
		return index;
	}

	/**
	 * "索引与日志文件内容配对"校验（判据与repointMissedRotation同构）：装载期active候选
	 * （current.index/indexLinks最大编号）与rotate名残留.index（openRotateIndex）共用。
	 * 1) 空索引恒配对（无内容可失配，装载后由loadIndex重建）；
	 * 2) 末记录offset超出日志文件长度=索引描述的是别的内容（自洽索引的offset必落在文件长度内）；
	 * 3) 内容抽查：文件首条可解析日志时间须落索引[beginTime,endTime]窗内——真配对时索引首
	 *    记录即由该首条采样而来（时间相等）；错配（停机期轮转/陈旧残留）时索引窗口与文件内容
	 *    分属不同世代，首条时间必在窗外。
	 */
	private boolean indexPairsLogFile(File indexFile, File logFile) {
		long[] headTail;
		try {
			headTail = readIndexHeadTail(indexFile);
		} catch (Exception e) {
			return false;
		}
		if (null == headTail)
			return true;
		if (headTail[2] > logFile.length())
			return false;
		var headTime = headTimeOf(logFile);
		return null != headTime && headTime >= headTail[0] && headTime <= headTail[1];
	}

	/**
	 * 候选索引的首末有效记录：不经LogIndex实例化读取（mmap会钉住文件，Windows下随后删除
	 * 被否决的current.index/rotate名残留.index必败）。尾部连续零记录与LogIndex构造器清理同语义跳过
	 * （否则endTime=0必然否决真配对）。
	 * @return {beginTime, endTime, 末记录offset}；null=无有效记录（空索引）。
	 */
	private static long[] readIndexHeadTail(File indexFile) throws IOException {
		try (var raf = new RandomAccessFile(indexFile, "r")) {
			var records = (int)(raf.length() / LogIndex.eIndexRecordSize);
			long endTime = 0;
			long lastOffset = 0;
			while (records > 0) {
				raf.seek((long)(records - 1) * LogIndex.eIndexRecordSize);
				var time = raf.readLong(); // RandomAccessFile与mmap的putLong同为big-endian
				var offset = raf.readLong();
				if (time == 0 && offset == 0) {
					--records;
					continue;
				}
				endTime = time;
				lastOffset = offset;
				break;
			}
			if (0 == records)
				return null;
			raf.seek(0);
			return new long[]{raf.readLong(), endTime, lastOffset};
		}
	}

	/**
	 * 头部采样：扫描到首条记录入索引即停——beginTime可用的最小充分集，不是妥协：
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
		reconcile(); // 低频对账：挂在buildIndexTimer上，先把files收敛到磁盘真相，再推进索引。
		// 锁外增量续建全部非active条目：补登头部采样只保证条目可入列，余量在此收敛——
		// 补登的rotate条目插在active之前，仅推进last==当前名的通道覆盖不到它，
		// 两半缺一不可。锁外正当性：loadIndex(File,LogIndex)只触碰(logFile,index)二元组、不读写files，
		// LogIndex自带rwLock（查询路径本就与其无锁并发），manager锁真正要保的只有files变更与轮转
		// "索引改名+条目改指"的串行——锁内全量扫描并非正确性需求；sealed rotate内容不可变、
		// 不参与改名/改指，锁外安全。新→旧序使近期时间窗最先获得精确跳转；逐条目隔离异常：单文件损坏/
		// 消失只损失该条目本轮续建，不中止整轮。
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
		// active条目维持锁内推进现状：错位检测（repointMissedRotation读endTime对照
		// 文件长度）与case-1改名/改指依赖addIndex与轮转处理同锁串行——移出锁会重新打开刚关闭的竞态窗口。
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
