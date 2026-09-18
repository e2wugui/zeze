package Zeze.Services;

import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.TimeZone;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import Zeze.Config;
import Zeze.Net.Acceptor;
import Zeze.Net.AsyncSocket;
import Zeze.Net.Connector;
import Zeze.Net.FamilyClass;
import Zeze.Net.Protocol;
import Zeze.Net.Selectors;
import Zeze.Net.Service;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Serialize.Serializable;
import Zeze.Transaction.Bean;
import Zeze.Transaction.DispatchMode;
import Zeze.Transaction.TransactionLevel;
import Zeze.Util.FastLock;
import Zeze.Util.ShutdownHook;
import Zeze.Util.Task;
import Zeze.Util.ThreadFactoryWithName;
import Zeze.Util.ZezeCounter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/*
高吞吐量的日志接收记录器. 包括客户端(Agent,日志发送方,支持多个)和服务器端(Service,日志接收方,负责存盘).
功能: 支持日志时间,日志类型和自定义ID三种索引,日志目录写入互斥机制,启动时修复索引,每天轮转日志数据和索引,友好关闭(支持shutdown触发).
高吞吐手段: 单独的写日志线程,从队列里批量取日志,有缓存的文件写入,定期刷给OS.
      日志数据以尽可能紧凑的二进制方式传输和存储, 服务器端不解析数据本身,原样保存.
限制: 单条日志数据限制<1M,单日日志数据文件最大16TB,日志时间限制在(1970年~2527年),系统时间不回调的话,不会出现相同的日志时间戳.
瓶颈及风险: 如果发送的日志速率超过写日志线程的写入速率,会达到传输队列指定的条数或大小上限,导致服务器端接收日志的IO线程暂停工作,
      进而导致客户端无法发送或产生堆积,此时发送方应主动丢弃. 记录的日志时间戳由服务器端在写入时确定. 过时的日志文件需要其它清理手段.
*/
public final class BinLogger extends ReentrantLock {
	private static final @NotNull Logger logger = LogManager.getLogger(BinLogger.class);
	private static final ZezeCounter.LabeledCounterCreator binLoggerCreator
			= ZezeCounter.instance.allocLabeledCounterCreator("BinLogger", "type");
	private static final ZezeCounter.LongCounter sendLogFailCounter = binLoggerCreator.labelValues("SendLogFail");
	private static final ZezeCounter.LongCounter writeLogCounter = binLoggerCreator.labelValues("WriteLog");
	private static final ZezeCounter.LongObserver waitQueueObserver
			= ZezeCounter.instance.getRunTimeObserver("BinLogger.waitQueue");
	private static final int timeZoneOffset = TimeZone.getDefault().getRawOffset(); // 北京时间(+8): 28800_000
	private static final int DEFAULT_PORT = 5004; // 服务的默认端口号
	private static final int MAX_LOG_SIZE = 0xfffff; // 1M-1, 单条日志数据的最大长度(涉及文件格式设计,不能改动)
	private static final int QUEUE_COUNT_LIMIT = 1024 * 1024; // 1M, 日志队列的数量限制
	private static final int QUEUE_SIZE_LIMIT = Math.max(256 * 1024 * 1024, MAX_LOG_SIZE); // 256M, 日志队列的数据总长度限制,不能小于MAX_LOG_SIZE
	private static final int BIN_BUFFER = 1024 * 1024; // bin类型文件的写缓冲区大小,积累到一定量或定期flush给OS
	private static final int OTHER_BUFFER = 64 * 1024; // 同上,用于其它类型文件
	private static final int FLUSH_PERIOD = 1_000; // flush日志文件的时间间隔(毫秒)
	private static final int WRITE_THREAD_IDLE_SLEEP = 100; // 输出日志线程空闲时的sleep时长(毫秒)
	private static final long WRITE_THREAD_JOIN_TIMEOUT = 10_000; // stop等待写线程退出的超时（毫秒）

	public static final class LogData extends Protocol<LogData> {
		public static final int protocolId = Bean.hash32(LogData.class.getName()); // 117415474
		public static final long typeId = makeTypeId(0, protocolId); // 117415474

		static {
			register(typeId, LogData.class);
		}

		public long roleId;
		public long dataType;
		public ByteBuffer data;

		public LogData() {
		}

		public LogData(long roleId, long dataType, @NotNull ByteBuffer data) {
			this.roleId = roleId;
			this.dataType = dataType;
			this.data = data;
		}

		public LogData(long roleId, @NotNull Serializable s) {
			this(roleId, s.typeId(), ByteBuffer.encode(s));
		}

		@Override
		public int getModuleId() {
			return 0;
		}

		@Override
		public int getProtocolId() {
			return protocolId;
		}

		@Override
		public long getTypeId() {
			return typeId;
		}

		@Override
		public int preAllocSize() {
			var dataSize = data.size();
			return 1 + 1 + ByteBuffer.WriteLongSize(roleId) + 8 + ByteBuffer.WriteUIntSize(dataSize) + dataSize;
		}

		@Override
		public void preAllocSize(int size) {
		}

		@Override
		public void encode(@NotNull ByteBuffer bb) {
			bb.WriteByte(FamilyClass.Protocol);
			bb.WriteByte(1); // version
			bb.WriteLong(roleId);
			bb.WriteLong8(dataType);
			bb.WriteBytes(data.Bytes, data.ReadIndex, data.size());
		}

		@Override
		public void decode(@NotNull IByteBuffer bb) {
			var header = bb.ReadUInt();
			if ((header & FamilyClass.FamilyClassMask) != FamilyClass.Protocol) {
				throw new IllegalStateException("invalid header(" + header + ") for decoding protocol "
					+ getClass().getName());
			}
			if ((header & FamilyClass.BitResultCode) != 0)
				bb.SkipLong(); // resultCode
			int version = bb.ReadByte();
			if (version != 1)
				throw new UnsupportedOperationException("version=" + version);
			roleId = bb.ReadLong();
			dataType = bb.ReadLong8();
			data = ByteBuffer.Wrap(bb.ReadBytes());
		}

		@Override
		public @NotNull String toString() {
			return "{" + roleId + ',' + dataType + ',' + data.toString() + '}';
		}
	}

	public static final class BinLoggerAgent extends Service {
		private Connector connector;

		public BinLoggerAgent() {
			this(null);
		}

		public BinLoggerAgent(@Nullable Config config) {
			super("BinLoggerAgent", config);

			var opt = getConfig().getHandshakeOptions();
			if (opt.getKeepCheckPeriod() == 0)
				opt.setKeepCheckPeriod(5);
			if (opt.getKeepRecvTimeout() == 0)
				opt.setKeepRecvTimeout(60);
			if (opt.getKeepSendTimeout() == 0)
				opt.setKeepSendTimeout(30);
		}

		@Override
		public void start() throws Exception {
			lock();
			try {
				if (connector != null)
					stop();
				var cfg = getConfig();
				int n = cfg.connectorCount();
				if (n != 1)
					throw new IllegalStateException("connectorCount = " + n + " != 1");
				cfg.forEachConnector(c -> this.connector = c);
				super.start();
			} finally {
				unlock();
			}
		}

		public @NotNull BinLoggerAgent start(@NotNull String host, int port) throws Exception {
			lock();
			try {
				if (connector != null)
					stop();
				connector = new Connector(host, port, true);
				connector.SetService(this);
				connector.setAutoReconnect(true);
				// 手工connector路径不复位keepalive熔断（FND8-62）：stop()经super.stop()置
				// keepAliveCheckStopped后，本路径若不super.start()，重启后所有TcpSocket构造
				// 都被熔断快路径拒绝，构造器配置的5s检查/60s收/30s发超时全部失效（静默死链
				// 不再被检测关闭）。先复位再建socket，与无参start的顺序语义一致。
				super.start();
				connector.start();
				return this;
			} finally {
				unlock();
			}
		}

		@Override
		public void stop() throws Exception {
			lock();
			try {
				if (connector != null) {
					connector.stop();
					connector = null;
				}
				super.stop();
			} finally {
				unlock();
			}
		}

		public void waitReady() {
			connector.WaitReady();
		}

		public boolean sendLog(long roleId, @NotNull Serializable log) {
			var so = connector.getSocket();
			if (so != null && so.Send(new LogData(roleId, log)))
				return true;
			sendLogFailCounter.increment();
			return false;
		}
	}

	public static final class BinLoggerService extends Service {
		private final @NotNull String logPath; // 日志保存的路径,以"/"结尾,日志文件名(除后缀名)是8位日期数字
		private @Nullable RandomAccessFile lockFile; // 以"LOCK"命名的文件,用于BinLogger对象独占日志写入权限
		private @Nullable FileLock fileLock; // lockFile的目录独占锁，与lockFile同生命周期，stopLogger释放
		private BufferedOutputStream binFile; // Bean(Data)结构日志经过二进制序列化紧凑连续保存的文件
		private BufferedOutputStream posFile; // 每条日志在bin文件中的位置和大小,小端保存为8字节整数,其中位置占高44位(最大支持16T),大小占低20位(最大支持1M-1)
		private BufferedOutputStream tsFile; // 每条日志的时间戳,小端保存为8字节整数,其中高44位是UTC毫秒时间戳,低20位是该时间戳的日志序号(从0开始)
		private BufferedOutputStream dtFile; // 每条日志的Bean类型,小端保存为8字节整数
		private BufferedOutputStream idFile; // 每条日志的所属ID,小端保存为8字节整数,通常为角色ID
		private final @NotNull FastLock queueLock = new FastLock(); // 写日志队列的锁
		private final @NotNull Condition queueLockCond = queueLock.newCondition(); // 写日志队列的锁等待条件
		private Thread writeLogThread; // 处理并输出日志文件的线程
		private ArrayList<LogData> writeLogQueue; // 写日志队列
		private long writeLogQueueSize; // 写日志队列已写入的数据总大小
		private long binFileSize; // 当前bin文件大小
		private long lastFlushMs; // 上次flush文件的毫秒时间戳
		private int curDayStamp; // 当前的日期戳
		private volatile boolean started; // 是否已经开始服务（写线程读作退出信号，需跨线程可见）
		// 写线程代际令牌（FND8-59）：stop超时放弃join后写线程可能仍存活（阻塞在文件IO、
		// 慢大批内——成功路径原本无停机检查），同实例start重启会把共享started置回true，
		// 旧线程的全部退出判定（读共享started）失效而"复活"，与新写线程双写五文件。
		// 每次startLogger先推进代际，写线程只认自己出生时的代际：迟到的旧线程无论共享
		// 标志被谁改写都会退出，且退出不触碰新代的任何共享状态。
		private volatile long loggerGeneration;
		private boolean waitingQueue; // 写日志队列是否已满导致等待

		// FND5-47：瞬态输出故障（Windows备份/防毒短暂锁定当天日志文件、NAS抖动）的自愈周期
		// 常超过原3×100ms判据，一刀切halt误杀可自愈故障（连同ShutdownHook跳过）。恢复失败
		// 改按观察窗判死：退避100ms翻倍（封顶5s）持续重开，累计观察60s仍失败才halt——保留
		// "宁停不错"终态，持久故障（磁盘满类）仍在有限时间内终止；成功写完整批即清窗。
		// FND4-68修的stop挂死根因是恢复循环无退出条件，与退避无关（!started检查在前）。
		private static final long WRITE_RECOVER_HALT_WINDOW_MS = 60_000;
		private static final long WRITE_RECOVER_BACKOFF_FIRST_MS = 100;
		private static final long WRITE_RECOVER_BACKOFF_CAP_MS = 5_000;

		/** 恢复失败第backoffExp次重试（0基）的退避时长：100ms翻倍，封顶5s。 */
		static long recoverBackoffMs(int backoffExp) {
			return Math.min(WRITE_RECOVER_BACKOFF_FIRST_MS << Math.min(backoffExp, 6), WRITE_RECOVER_BACKOFF_CAP_MS);
		}

		public BinLoggerService(@Nullable String logPath) {
			this(null, logPath);
		}

		public BinLoggerService(@Nullable Config config, @Nullable String logPath) {
			super("BinLoggerService", config != null ? config : new Config().loadAndParse());
			if (logPath == null)
				logPath = "";
			else {
				logPath = logPath.trim().replace('\\', '/');
				if (!logPath.endsWith("/"))
					logPath += '/';
			}
			this.logPath = logPath;

			var opt = getConfig().getHandshakeOptions();
			if (opt.getKeepCheckPeriod() == 0)
				opt.setKeepCheckPeriod(5);
			if (opt.getKeepRecvTimeout() == 0)
				opt.setKeepRecvTimeout(60);
			if (opt.getKeepSendTimeout() == 0)
				opt.setKeepSendTimeout(30);

			AddFactoryHandle(LogData.typeId, new ProtocolFactoryHandle<>(LogData::new, this::processLogData,
				TransactionLevel.None, DispatchMode.Direct));
			ShutdownHook.add(this::stop);
		}

		@Override
		public void start() throws Exception {
			start(null, DEFAULT_PORT);
		}

		// 参数host,port优先; 如果传null/<=0则以config为准; 如果config也没配置则用默认值null/DEFAULT_PORT
		public void start(@Nullable String host, int port) throws Exception {
			lock();
			try {
				if (started)
					stop();
				started = true;
				logger.info("BinLoggerService starting ...");
				var sc = getConfig();
				if (sc.acceptorCount() == 0)
					sc.addAcceptor(new Acceptor(port > 0 ? port : DEFAULT_PORT, host));
				else {
					sc.forEachAcceptor2(acceptor -> {
						if (host != null)
							acceptor.setIp(host);
						if (port > 0)
							acceptor.setPort(port);
						return false;
					});
				}
				startLogger();
				super.start();
			} finally {
				unlock();
			}
		}

		@Override
		public void stop() throws Exception {
			lock();
			try {
				if (!started)
					return;
				started = false;
				logger.info("BinLoggerService stopping ...");
				try {
					super.stop();
				} finally {
					stopLogger();
				}
			} finally {
				unlock();
			}
		}

		private static final class RecoveryFile implements Closeable {
			final @NotNull RandomAccessFile raf;
			final @NotNull FileChannel fc;
			final long size;

			private RecoveryFile(@NotNull String fileName) throws IOException {
				RandomAccessFile f = null;
				try {
					raf = f = new RandomAccessFile(fileName, "rw");
					fc = f.getChannel();
					size = fc.size();
				} catch (Throwable e) { // rethrow
					forceClose(f);
					throw e;
				}
			}

			void tryTruncate(long size) throws IOException {
				if (this.size != size)
					fc.truncate(size);
			}

			@Override
			public void close() {
				forceClose(raf);
			}
		}

		private static void forceClose(@Nullable Closeable f) {
			if (f != null) {
				try {
					f.close();
				} catch (Exception ignored) {
				}
			}
		}

		/** 丢弃退出：置空写队列并唤醒等满的生产者（processLogData见null即break，本条按drop
		 * 处理），写线程不再回到外层循环。轮转窗口停机退出（FND6-29）与写异常恢复分支的
		 * exitOnStop（FND5-39）共用。 */
		private void discardWriteQueueForExit() {
			queueLock.lock();
			try {
				writeLogQueue = null;
				if (waitingQueue) {
					waitingQueue = false;
					queueLockCond.signalAll();
				}
			} finally {
				queueLock.unlock();
			}
		}

		static int toDayStamp(long utcMs) { // UTC毫秒时间戳 => 日期戳(天数)；包内可见供DST单测
			// 与toDayStr同源的DST感知口径（FND4-75）：原用timeZoneOffset=getRawOffset（不含夏令时
			// 偏移），DST时区中切换日的天边界与toDayStr渲染日期错开一天（轮转文件名与数据实际
			// 归属日期不符；五文件同批打开索引一致性不损坏）。
			return (int)Instant.ofEpochMilli(utcMs).atZone(ZoneId.systemDefault()).toLocalDate().toEpochDay();
		}

		static @NotNull String toDayStr(int dayStamp) { // 日期戳(天数) => "yyyyMMdd"
			var date = java.time.LocalDate.ofEpochDay(dayStamp); // 与toDayStamp互逆（同一日期事实源）
			return String.format("%04d%02d%02d", date.getYear(), date.getMonthValue(), date.getDayOfMonth());
		}

		private void startLogger() throws Exception {
			// 代际先于一切动作推进（含下方所有失败路径）：startLogger任一步失败重启，
			// 旧代写线程也已过时，醒来只会走代际退出。
			var myGeneration = ++loggerGeneration;
			logger.info("lock logPath: '{}'", logPath);
			try {
				// 1.目录上锁
				Files.createDirectories(Path.of(logPath));
				lockFile = new RandomAccessFile(logPath + "LOCK", "rw");
				fileLock = lockFile.getChannel().tryLock();
				if (fileLock == null)
					throw new IOException("tryLock LOCK file failed");
				// 2.修复并打开当天的所有日志和索引文件
				curDayStamp = toDayStamp(System.currentTimeMillis());
				openDay(curDayStamp);
				// 3.开启输出日志线程
				writeLogQueue = new ArrayList<>();
				writeLogQueueSize = 0;
				waitingQueue = false;
				writeLogThread = new Thread(() -> writeLogThread(myGeneration), "WriteLogThread");
				writeLogThread.setPriority(Thread.NORM_PRIORITY + 2); // 稍调高点优先级,确保输出日志吞吐性能
				writeLogThread.start();
			} catch (Throwable e) { // rethrow
				started = false;
				stopLogger();
				throw e;
			}
		}

		// 打开指定日期戳的全套日志和索引文件: 先按bin/pos对账修复索引, 再以追加方式打开, 最后一次性替换当前状态.
		// 启动与跨天轮转共用此唯一入口, 当日的binFileSize等状态都在这里重新建立, 避免两处各写一份而漏项.
		// 打开过程中抛异常时不改变任何当前状态, 于是轮转失败仍可继续用旧文件写, 下一轮再重试.
		private void openDay(int dayStamp) throws Exception {
			var fnPrefix = logPath + toDayStr(dayStamp);
			final long newBinFileSize;
			try (var binF = new RecoveryFile(fnPrefix + ".bin");
				 var posF = new RecoveryFile(fnPrefix + ".pos");
				 var tsF = new RecoveryFile(fnPrefix + ".ts");
				 var dtF = new RecoveryFile(fnPrefix + ".dt");
				 var idF = new RecoveryFile(fnPrefix + ".id")) {
				newBinFileSize = binF.size;
				logger.info("recovery: bin,pos,ts,dt,id.size={},{},{},{},{}; fileNamePrefix='{}'",
					newBinFileSize, posF.size, tsF.size, dtF.size, idF.size, fnPrefix);
				var otherTruncateSize = Math.min(Math.min(Math.min(posF.size, tsF.size), dtF.size), idF.size) & ~7;
				var buf = new byte[8];
				for (; otherTruncateSize >= 8; otherTruncateSize -= 8) {
					posF.raf.seek(otherTruncateSize - 8);
					posF.raf.read(buf);
					var posLen = ByteBuffer.ToLong(buf, 0);
					if ((posLen >>> 20) + (posLen & 0xfffff) <= newBinFileSize)
						break;
				}
				logger.info("recovery: truncate size={}", otherTruncateSize);
				posF.tryTruncate(otherTruncateSize);
				tsF.tryTruncate(otherTruncateSize);
				dtF.tryTruncate(otherTruncateSize);
				idF.tryTruncate(otherTruncateSize);
			}
			BufferedOutputStream newBinFile = null, newPosFile = null, newTsFile = null, newDtFile = null, newIdFile = null;
			try {
				newBinFile = new BufferedOutputStream(new FileOutputStream(fnPrefix + ".bin", true), BIN_BUFFER);
				newPosFile = new BufferedOutputStream(new FileOutputStream(fnPrefix + ".pos", true), OTHER_BUFFER);
				newTsFile = new BufferedOutputStream(new FileOutputStream(fnPrefix + ".ts", true), OTHER_BUFFER);
				newDtFile = new BufferedOutputStream(new FileOutputStream(fnPrefix + ".dt", true), OTHER_BUFFER);
				newIdFile = new BufferedOutputStream(new FileOutputStream(fnPrefix + ".id", true), OTHER_BUFFER);
			} catch (Throwable e) { // rethrow
				forceClose(newIdFile);
				forceClose(newDtFile);
				forceClose(newTsFile);
				forceClose(newPosFile);
				forceClose(newBinFile);
				throw e;
			}
			binFileSize = newBinFileSize;
			binFile = newBinFile;
			posFile = newPosFile;
			tsFile = newTsFile;
			dtFile = newDtFile;
			idFile = newIdFile;
			lastFlushMs = System.currentTimeMillis();
		}

		private void stopLogger() throws Exception {
			if (writeLogThread != null) {
				logger.info("waiting for writeLogThread ...");
				// 写线程停机时丢弃残余批退出（见writeLogThread的!started分支），正常这里很快返回；
				// 带超时兜底：异常滞留时不得让stop()/ShutdownHook永久挂起（FND4-68，原join无超时）。
				writeLogThread.join(WRITE_THREAD_JOIN_TIMEOUT);
				if (writeLogThread.isAlive())
					logger.error("writeLogThread not exit in {}ms after stop, give up waiting.",
							WRITE_THREAD_JOIN_TIMEOUT);
				writeLogThread = null;
			}
			forceClose(idFile);
			forceClose(dtFile);
			forceClose(tsFile);
			forceClose(posFile);
			forceClose(binFile);
			if (fileLock != null) {
				try {
					fileLock.release();
				} catch (IOException e) { // logger.error
					logger.error("release fileLock exception.", e);
				}
				fileLock = null;
			}
			forceClose(lockFile);
			idFile = null;
			dtFile = null;
			tsFile = null;
			posFile = null;
			binFile = null;
			lockFile = null;
			logger.info("unlock logPath: '{}'", logPath);
		}

		@Override
		public void dispatchProtocol(long typeId, @NotNull ByteBuffer bb,
									 @NotNull ProtocolFactoryHandle<?> factoryHandle,
									 @Nullable AsyncSocket so) {
			try {
				decodeProtocol(typeId, bb, factoryHandle, so).handle(this, factoryHandle); // 直接跑在IO线程上
			} catch (Throwable e) { // logger.error
				logger.error("BinLoggerService.dispatchProtocol exception:", e);
			}
		}

		private long processLogData(@NotNull LogData p) throws InterruptedException {
			int dataSize = p.data.size();
			if (dataSize > MAX_LOG_SIZE) {
				logger.warn("too long size of LogData: roleId={}, type={}, size={}, sender={}",
					p.roleId, p.dataType, dataSize, p.getSender());
			} else {
				var timeBegin = 0L;
				queueLock.lock();
				try {
					for (; ; ) {
						var wlq = writeLogQueue;
						if (wlq == null)
							break;
						var newQueueSize = writeLogQueueSize + dataSize;
						if (newQueueSize <= QUEUE_SIZE_LIMIT && wlq.size() < QUEUE_COUNT_LIMIT) {
							writeLogQueueSize = newQueueSize;
							wlq.add(p);
							return 0;
						}
						if (timeBegin == 0)
							timeBegin = ZezeCounter.ENABLE ? System.nanoTime() : 0;
						waitingQueue = true;
						queueLockCond.await();
					}
				} finally {
					queueLock.unlock();
				}
				if (timeBegin != 0)
					waitQueueObserver.observe(System.nanoTime() - timeBegin);
				logger.info("drop LogData: roleId={}, type={}, size={}, sender={}",
					p.roleId, p.dataType, dataSize, p.getSender());
			}
			return 0;
		}

		private void writeLogThread(long myGeneration) {
			var readLogQueue = new ArrayList<LogData>(); // 读日志队列
			var lastTs = 0L;
			var buf = new byte[8];
			var rotateBackoffExp = 0; // 轮转失败重试退避指数（FND8-60，成功清零）
			var rotateRetryAfterMs = 0L; // 下次允许尝试轮转的时间戳（FND8-60，降级续写期间的退避重试）
			for (int queueSize; ; ) {
				try {
					queueLock.lock();
					try {
						if (myGeneration != loggerGeneration) {
							// FND8-59：stale写线程（stop超时放弃join后同实例start重启产生）。
							// 此刻队列可能已属新一代：不得置null（会令新代线程在wlq==null跟着
							// 退出、processLogData断流）、不得signal生产者，仅丢弃本地残批退出。
							if (!readLogQueue.isEmpty())
								logger.error("writeLogThread exit on stale generation: discard {} resident logs",
									readLogQueue.size());
							break;
						}
						var wlq = writeLogQueue;
						if (wlq == null) { // 阻止了写队列后,也处理完读队列,可以退出了
							// FND8-60：轮转失败滞留等路径下readLogQueue可能还有未写批，与其余三条
							// 停机退出路径（轮转窗口/写异常/重开复查）同口径补记丢弃日志——
							// 丢弃本身是FND4-68终止契约。
							if (!readLogQueue.isEmpty())
								logger.error("writeLogThread exit on stopped queue: discard {} resident logs",
									readLogQueue.size());
							break;
						}
						queueSize = wlq.size();
						if (queueSize > 0) {
							var reusedQueue = readLogQueue; // 换回写侧的队列：正常为空（写完即clear）
							writeLogQueue = reusedQueue;
							readLogQueue = wlq;
							// 计数随内容走（FND4-71）：轮转失败等滞留路径下复用队列可能残留整批
							// （既没写也没clear），残留字节数并入写侧计数——原直接置0丢弃残留计数，
							// 滞留期间QUEUE_SIZE_LIMIT被残留批突破（限流口径漂移、瞬时超限）。
							long residualBytes = 0;
							for (var residual : reusedQueue)
								residualBytes += residual.data.size();
							writeLogQueueSize = residualBytes;
							if (waitingQueue) {
								waitingQueue = false;
								queueLockCond.signalAll();
							}
						}
						if (!started) {
							// FND8-60：写侧残留批（轮转失败滞留路径复用队列残留，见FND4-71）随置null
							// 被丢弃，补记日志（与各停机discard同口径）。
							if (!writeLogQueue.isEmpty())
								logger.error("writeLogThread stopping: discard {} queued logs",
									writeLogQueue.size());
							writeLogQueue = null; // 阻止日志再进入队列
						}
					} finally {
						queueLock.unlock();
					}
					var curMs = System.currentTimeMillis();
					if (queueSize > 0) {
						if (curMs != lastTs >>> 20)
							lastTs = curMs << 20;
						var dayStamp = toDayStamp(curMs);
						if (dayStamp != curDayStamp) { // 判断是否要轮转日志文件
							if (myGeneration != loggerGeneration) {
								// FND8-59：stale不轮转——openDay会整体重赋五字段，抢占新代的流。
								logger.error("writeLogThread exit on stale generation during rotation: discard {} logs",
									queueSize);
								break;
							}
							if (curMs >= rotateRetryAfterMs) { // FND8-60：退避窗口内不重试，降级续写旧文件
								var oldBinFile = binFile;
								var oldPosFile = posFile;
								var oldTsFile = tsFile;
								var oldDtFile = dtFile;
								var oldIdFile = idFile;
								var rotated = false;
								try {
									// 先打开新一天的全套文件再关旧文件: 打开失败时旧文件仍可继续写, 且curDayStamp不推进, 下一轮重试轮转.
									openDay(dayStamp);
									rotated = true;
								} catch (Throwable e) { // logger.error
									// 降级续写（FND8-60，兑现上一行注释承诺的行为）：openDay原子失败
									// 不改当前状态，旧五件套仍有效，本批继续写旧文件——ts索引携带绝对
									// 时间戳且lastTs跨轮转连续，午夜后条目落入昨日文件不破坏索引；
									// 重启对账按文件独立进行。失败尝试会留下空的当日新文件（RecoveryFile
									// "rw"即创建），后续成功的openDay按尺寸0正常处理，无害。退避限制
									// 重试频率（每次重试重做五文件对账I/O）。真正的持久输出故障（旧文件
									// 也写不动）自然落入既有写异常60s观察窗判死，无需新判死机制。
									var backoffMs = recoverBackoffMs(rotateBackoffExp++);
									rotateRetryAfterMs = curMs + backoffMs;
									logger.error("rotate open day {} fail, backoff {}ms, keep writing day {} files",
										toDayStr(dayStamp), backoffMs, toDayStr(curDayStamp), e);
								}
								if (rotated) {
									forceClose(oldIdFile);
									forceClose(oldDtFile);
									forceClose(oldTsFile);
									forceClose(oldPosFile);
									forceClose(oldBinFile);
									rotateBackoffExp = 0;
									if (myGeneration != loggerGeneration) {
										// FND8-59：openDay（阻塞IO）跨越了重启：curDayStamp不推进、
										// 不再触碰共享状态，丢弃本地残批退出。
										logger.error("writeLogThread exit on stale generation after rotation: discard {} logs",
											queueSize);
										break;
									}
									curDayStamp = dayStamp;
									// FND6-29：轮转窗口停机复查。stop超时放弃join后stopLogger已forceClose当时
									// 字段并释放目录锁，openDay在此之后重开的新五件套无人负责关闭（句柄泄漏到
									// 进程结束）；继续写完整批还会在同目录重启新实例时双写同日bin/pos索引交叉损坏。
									// 停机优先于落盘（FND4-68终止契约，与写异常恢复分支的!started同口径）：
									// 关闭新句柄、丢弃残余批直接退出。
									if (!started) {
										logger.error("writeLogThread exit on stopping during rotation: discard {} logs",
											queueSize);
										forceClose(idFile);
										forceClose(dtFile);
										forceClose(tsFile);
										forceClose(posFile);
										forceClose(binFile);
										discardWriteQueueForExit();
										break; // 退出外层for(;;)
									}
								}
							}
						}
						// 失败断点（FND3-43）：completed=已完整写成（五文件齐）的条数。写异常时
						// 当前条可能已部分写入bin（脏尾）且内存binFileSize与文件实际长度脱钩：
						// 关闭当前流，走openDay对账入口按bin实际长度截齐索引并重建binFileSize
						// ——脏尾成为未索引gap（按pos读取永不触碰），追加从实际长度重新对齐；
						// 剩余条目内联重写（滞留到下轮swap会等流量、且整批重写造成前缀重复）。
						var completed = 0;
						var recoverBackoffExp = 0; // 恢复失败重试指数（成功写完整批清零）
						var recoverWindowMs = 0L; // 恢复失败累计观察窗（FND5-47，成功清零）
						var exitOnStop = false;
						var exitStale = false; // 代际过时退出（FND8-59）：不动writeLogQueue（可能属新代）
						while (completed < queueSize) { // 把当前队列里的日志全部写入日志和索引文件,用相同的毫秒时间戳应该没问题
							try {
								for (int i = completed; i < queueSize; i++) {
									if (myGeneration != loggerGeneration) {
										// FND8-59：慢大批期间同实例重启——成功路径原本无停机检查，
										// 整批会写进新代流后才退出；逐条检查把损坏窗口缩到单条。
										logger.error("writeLogThread exit on stale generation in batch: discard {} logs, completed={}/{}",
											queueSize - completed, completed, queueSize);
										exitStale = true;
										break;
									}
									var logData = readLogQueue.get(i);
									var data = logData.data;
									var dataSize = data.size();
									binFile.write(data.Bytes, data.ReadIndex, dataSize);
									ByteBuffer.longLeHandler.set(buf, 0, (binFileSize << 20) + dataSize);
									posFile.write(buf);
									binFileSize += dataSize;
									ByteBuffer.longLeHandler.set(buf, 0, lastTs++);
									tsFile.write(buf);
									ByteBuffer.longLeHandler.set(buf, 0, logData.dataType);
									dtFile.write(buf);
									ByteBuffer.longLeHandler.set(buf, 0, logData.roleId);
									idFile.write(buf);
									completed = i + 1;
								}
									recoverBackoffExp = 0;
									recoverWindowMs = 0L;
							} catch (Throwable e) { // logger.error
								// 当前条可能半写：completed不推进，恢复后重写它（脏尾成gap）。
								logger.error("writeLogThread write exception. completed={}, day={}",
									completed, curDayStamp, e);
								if (myGeneration != loggerGeneration) {
									// FND8-59：写异常恢复前先查代际——重启后五字段属新代，
									// forceClose会关掉新代的流、openDay会抢占新代的流：不触碰
									// 共享状态，丢弃残余批退出。
									logger.error("writeLogThread exit on stale generation on write exception: discard {} logs, completed={}/{}",
										queueSize - completed, completed, queueSize);
									exitStale = true;
									break;
								}
								// FND5-39：停机检查必须先于重开——stop超时放弃join后，写线程稍后
								// 从慢速写抛异常进恢复分支：原顺序先forceClose+openDay再查!started，
								// 会把当日五件套重开赋给字段后退出，nobody再关闭（句柄泄漏到进程
								// 结束+stop返回期间继续写；目录锁已释放时与新实例双写同日文件）。
								// 停机优先于落盘：丢弃残余批直接退出（FND4-68终止契约不变）。
								if (!started) {
									logger.error("writeLogThread exit on stopping: discard {} logs, completed={}/{}",
										queueSize - completed, completed, queueSize);
									exitOnStop = true;
									break;
								}
								try {
									forceClose(idFile);
									forceClose(dtFile);
									forceClose(tsFile);
									forceClose(posFile);
									forceClose(binFile);
									openDay(curDayStamp); // 失败保持closed流：下次write再抛，再次进入恢复
									if (myGeneration != loggerGeneration) {
										// FND8-59：重开（阻塞IO）跨越了重启：不再forceClose字段
										// （关闭责任归新代stopLogger），丢弃残余批退出。
										logger.error("writeLogThread exit on stale generation during recover reopen: discard {} logs, completed={}/{}",
											queueSize - completed, completed, queueSize);
										exitStale = true;
										break; // 退出内层while，经exitStale路径退出外层for(;;)
									}
									// FND6-29姊妹：恢复分支重开后的停机复查。停机落在上方检查之后、
									// 且openDay耗时跨越stop放弃join的点（NFS/磁盘抖动停滞正是本分支
									// 的威胁模型前提）时，stopLogger已forceClose重开前的字段并释放
									// 目录锁，重开的新五件套此后无人负责关闭（泄漏到进程结束）；重试
									// 写残余批还会与新实例双写同日bin/pos。停机优先于落盘（FND4-68）：
									// 关闭新句柄，经exitOnStop统一丢弃残余批退出。
									if (!started) {
										logger.error("writeLogThread exit on stopping during recover reopen: discard {} logs, completed={}/{}",
											queueSize - completed, completed, queueSize);
										forceClose(idFile);
										forceClose(dtFile);
										forceClose(tsFile);
										forceClose(posFile);
										forceClose(binFile);
										exitOnStop = true;
										break; // 退出内层while，经exitOnStop路径丢弃残余批并退出外层for(;;)
									}
								} catch (Throwable ex) { // logger.error
									logger.error("reopen after write exception fail.", ex);
								}
								// 宁停不错（家族halt口径，对齐Transaction毒化处理）保留终态，但按观察窗
								// 判死（FND5-47）：累计退避观察WRITE_RECOVER_HALT_WINDOW_MS仍失败=不可恢复
								// 的输出故障，fatal终止；窗口内的失败视为可自愈瞬态，持续退避重开。
								var backoffMs = recoverBackoffMs(recoverBackoffExp++);
								recoverWindowMs += backoffMs;
								if (recoverWindowMs >= WRITE_RECOVER_HALT_WINDOW_MS) {
									logger.fatal("writeLogThread recover failed for {}ms (window), halt. completed={}/{}, day={}",
										recoverWindowMs, completed, queueSize, curDayStamp);
									LogManager.shutdown();
									Runtime.getRuntime().halt(543543);
								}
								// 恢复失败（如磁盘满）时退避限制重试频率，避免紧密重开环。
								//noinspection BusyWait
								Thread.sleep(backoffMs);
							}
						}
						readLogQueue.clear();
						if (exitOnStop) {
							discardWriteQueueForExit();
							break; // 退出外层for(;;)
						}
						if (exitStale)
							break; // FND8-59：残批已随clear丢弃；不动writeLogQueue（可能属新代）
						writeLogCounter.inc(queueSize);
					} else {
						if (curMs - lastFlushMs >= FLUSH_PERIOD) { // 定时刷新到OS
							lastFlushMs = curMs;
							binFile.flush();
							posFile.flush();
							tsFile.flush();
							dtFile.flush();
							idFile.flush();
						}
						if (started) {
							//noinspection BusyWait
							Thread.sleep(WRITE_THREAD_IDLE_SLEEP);
						}
					}
				} catch (Throwable e) { // logger.error
					logger.error("writeLogThread exception:", e);
					try {
						//noinspection BusyWait
						Thread.sleep(WRITE_THREAD_IDLE_SLEEP);
					} catch (InterruptedException ex) {
						throw Task.forceThrow(ex);
					}
				}
			}
		}
	}

	public static void main(@NotNull String @NotNull [] args) throws Exception {
		Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
			//noinspection CallToPrintStackTrace
			e.printStackTrace();
			logger.error("uncaught exception in {}:", t, e);
		});

		String host = null;
		int port = 0;
		int threadCount = 0;
		String path = "binlog";

		for (int i = 0; i < args.length; ++i) {
			switch (args[i]) {
			case "-host": // 指定监听的IP,不指定则监听所有IP,如果当前目录存在zeze.xml且定义了BinLoggerService,则默认以配置为准
				host = args[++i];
				if (host.isBlank())
					host = null;
				break;
			case "-port": // 指定监听端口号,默认DEFAULT_PORT常量,如果当前目录存在zeze.xml且定义了BinLoggerService,则默认以配置为准
				port = Integer.parseInt(args[++i]);
				break;
			case "-threads": // 网络IO线程数,默认0表示CPU核数
				threadCount = Integer.parseInt(args[++i]);
				break;
			case "-path": // 输出二进制日志的相对路径,默认"binlog"
				path = args[++i].trim();
				break;
			default:
				throw new IllegalArgumentException("unknown argument: '" + args[i] + '\'');
			}
		}

		if (threadCount < 1)
			threadCount = Runtime.getRuntime().availableProcessors();
		Task.initThreadPool(Task.newCriticalThreadPool("ZezeTaskPool"),
			Executors.newSingleThreadScheduledExecutor(
				new ThreadFactoryWithName("ZezeScheduledPool", Thread.NORM_PRIORITY + 2)));
		if (Selectors.getInstance().getCount() < threadCount)
			Selectors.getInstance().add(threadCount - Selectors.getInstance().getCount());
		ZezeCounter.tryInit();

		new BinLoggerService(path).start(host, port);
		synchronized (Thread.currentThread()) {
			Thread.currentThread().wait();
		}
	}
}
