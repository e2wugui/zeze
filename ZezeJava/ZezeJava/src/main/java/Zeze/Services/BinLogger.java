package Zeze.Services;

import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.util.Date;
import java.util.TimeZone;
import java.util.concurrent.Executors;
import Zeze.Collections.BeanFactory;
import Zeze.Config;
import Zeze.Net.Acceptor;
import Zeze.Net.AsyncSocket;
import Zeze.Net.Connector;
import Zeze.Net.FamilyClass;
import Zeze.Net.Protocol;
import Zeze.Net.Selectors;
import Zeze.Net.Service;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.NioByteBuffer;
import Zeze.Serialize.Serializable;
import Zeze.Transaction.Bean;
import Zeze.Transaction.DispatchMode;
import Zeze.Transaction.TransactionLevel;
import Zeze.Util.Json;
import Zeze.Util.Task;
import Zeze.Util.ThreadFactoryWithName;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class BinLogger {
	private static final Logger logger = LogManager.getLogger(BinLogger.class);
	private static final int timeZoneOffset = TimeZone.getDefault().getRawOffset(); // 北京时间(+8): 28800_000
	private static final long MAX_LOG_SIZE = 1L << 30;
	private static final int DEFAULT_PORT = 5004;

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
			this(roleId, s.typeId(), BeanFactory.toByteBuffer(s, null));
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
		public void decode(@NotNull ByteBuffer bb) {
			var header = bb.ReadInt();
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
		public synchronized void start() throws Exception {
			if (connector != null)
				stop();
			var cfg = getConfig();
			int n = cfg.connectorCount();
			if (n != 1)
				throw new IllegalStateException("connectorCount = " + n + " != 1");
			cfg.forEachConnector(c -> this.connector = c);
			super.start();
		}

		public synchronized @NotNull BinLoggerAgent start(@NotNull String host, int port) throws Exception {
			if (connector != null)
				stop();
			connector = new Connector(host, port, true);
			connector.SetService(this);
			connector.setAutoReconnect(true);
			connector.start();
			return this;
		}

		@Override
		public synchronized void stop() throws Exception {
			if (connector != null) {
				connector.stop();
				connector = null;
			}
			super.stop();
		}

		public void waitReady() {
			connector.WaitReady();
		}

		public boolean sendLog(long roleId, @NotNull Serializable log) {
			var so = connector.getSocket();
			return so != null && so.Send(new LogData(roleId, log));
		}

		@Override
		public void dispatchProtocol(long typeId, @NotNull ByteBuffer bb,
									 @NotNull ProtocolFactoryHandle<?> factoryHandle,
									 @NotNull AsyncSocket so) throws Exception {
			try {
				decodeProtocol(typeId, bb, factoryHandle, so).handle(this, factoryHandle); // 所有协议处理几乎无阻塞,可放心直接跑在IO线程上
			} catch (Throwable e) { // logger.error
				logger.error("dispatchProtocol exception:", e);
			}
		}
	}

	public static final class BinLoggerService extends Service {
		private static final int BIN_BUFFER = 1024 * 1024;
		private static final int OTHER_BUFFER = 64 * 1024;

		private final @NotNull String logPath;
		private @Nullable RandomAccessFile lockFile;
		private @Nullable BufferedOutputStream binFile, posFile, tsFile, dtFile, idFile;
		private int curDayStamp;
		private boolean started;

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
		}

		@Override
		public void start() throws Exception {
			start(null, DEFAULT_PORT);
		}

		// 参数host,port优先; 如果传null/<=0则以config为准; 如果config也没配置则用默认值null/DEFAULT_PORT
		public synchronized void start(@Nullable String host, int port) throws Exception {
			if (started)
				stop();
			started = true;

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
			// ShutdownHook.add(BinLogger::trySaveFile);
			super.start();
		}

		@Override
		public synchronized void stop() throws Exception {
			started = false;
			try {
				super.stop();
			} finally {
				stopLogger();
			}
		}

		@Override
		public void dispatchProtocol(long typeId, @NotNull ByteBuffer bb,
									 @NotNull ProtocolFactoryHandle<?> factoryHandle,
									 @NotNull AsyncSocket so) throws Exception {
			try {
				decodeProtocol(typeId, bb, factoryHandle, so).handle(this, factoryHandle); // 所有协议处理几乎无阻塞,可放心直接跑在IO线程上
			} catch (Throwable e) { // logger.error
				logger.error("dispatchProtocol exception:", e);
			}
		}

		private long processLogData(@NotNull LogData p) {
			//TODO
			return 0;
		}

		private static int toDayStamp(long utcMs) {
			return (int)((utcMs + timeZoneOffset) / 86400_000);
		}

		@SuppressWarnings("deprecation")
		private static String toDayStr(int dayStamp) { // 20231117
			var date = new Date(dayStamp * 86400_000L - timeZoneOffset);
			return String.format("%4d%2d%2d", date.getYear() + 1900, date.getMonth() + 1, date.getDate());
		}

		private void startLogger() throws IOException {
			try {
				// 1.目录上锁
				lockFile = new RandomAccessFile(logPath + "LOCK", "rw");
				if (lockFile.getChannel().tryLock() == null)
					throw new IOException("tryLock LOCK file failed");
				// 2.修复当前打开的文件
				curDayStamp = toDayStamp(System.currentTimeMillis());
				var fileNamePrefix = logPath + toDayStr(curDayStamp);
				recover(fileNamePrefix);
				// 3.打开当日文件
				binFile = new BufferedOutputStream(new FileOutputStream(fileNamePrefix + ".bin", true), BIN_BUFFER);
				posFile = new BufferedOutputStream(new FileOutputStream(fileNamePrefix + ".pos", true), OTHER_BUFFER);
				tsFile = new BufferedOutputStream(new FileOutputStream(fileNamePrefix + ".ts", true), OTHER_BUFFER);
				dtFile = new BufferedOutputStream(new FileOutputStream(fileNamePrefix + ".dt", true), OTHER_BUFFER);
				idFile = new BufferedOutputStream(new FileOutputStream(fileNamePrefix + ".id", true), OTHER_BUFFER);
			} catch (Throwable e) {
				try {
					stopLogger();
				} catch (Exception ignored) {
				}
				throw e;
			}
		}

		private static void forceClose(Closeable c) {
			try {
				c.close();
			} catch (Exception ignored) {
			}
		}

		private void stopLogger() throws IOException {
			if (idFile != null) {
				forceClose(idFile);
				idFile = null;
			}
			if (dtFile != null) {
				forceClose(dtFile);
				dtFile = null;
			}
			if (tsFile != null) {
				forceClose(tsFile);
				tsFile = null;
			}
			if (posFile != null) {
				forceClose(posFile);
				posFile = null;
			}
			if (binFile != null) {
				forceClose(binFile);
				binFile = null;
			}
			if (lockFile != null) {
				forceClose(lockFile);
				lockFile = null;
			}
		}

		private static final class RecoveryFile implements Closeable {
			final RandomAccessFile raf;
			final FileChannel fc;
			final long size;

			private RecoveryFile(String fileName) throws IOException {
				RandomAccessFile f = null;
				try {
					raf = f = new RandomAccessFile(fileName, "rw");
					fc = f.getChannel();
					size = fc.size();
				} catch (Throwable e) {
					if (f != null)
						f.close();
					throw e;
				}
			}

			void tryTruncate(long size) throws IOException {
				if (this.size != size)
					fc.truncate(size);
			}

			@Override
			public void close() throws IOException {
				raf.close();
			}
		}

		private static void recover(@NotNull String fileNamePrefix) throws IOException {
			try (var binF = new RecoveryFile(fileNamePrefix + ".bin");
				 var posF = new RecoveryFile(fileNamePrefix + ".pos");
				 var tsF = new RecoveryFile(fileNamePrefix + ".ts");
				 var dtF = new RecoveryFile(fileNamePrefix + ".dt");
				 var idF = new RecoveryFile(fileNamePrefix + ".id")) {
				logger.info("recovery: bin,pos,ts,dt,id size = {},{},{},{},{}",
						binF.size, posF.size, tsF.size, dtF.size, idF.size);
				var otherMinSize = Math.min(Math.min(Math.min(posF.size, tsF.size), dtF.size), idF.size) & ~7;
				posF.tryTruncate(otherMinSize);
				tsF.tryTruncate(otherMinSize);
				dtF.tryTruncate(otherMinSize);
				idF.tryTruncate(otherMinSize);
				if (otherMinSize == 0) {
					binF.tryTruncate(0);
					return;
				}
				var buf = new byte[8];
				for (long otherSize = otherMinSize; otherSize > 0; otherSize -= 8) {
					posF.raf.seek(otherSize - 8);
					posF.raf.read(buf);
					var pos = ByteBuffer.ToLong(buf, 0);
					if (pos < binF.size) {
						MappedByteBuffer mbb = null;
						try {
							mbb = binF.fc.map(MapMode.READ_ONLY, pos, Math.min(binF.size - pos, MAX_LOG_SIZE));
							var nbb = NioByteBuffer.Wrap(mbb);
							nbb.SkipUnknownField(ByteBuffer.BEAN);
							//TODO
						} finally {
							if (mbb != null)
								Json.getUnsafe().invokeCleaner(mbb);
						}
					}
				}
			}
		}

		private void writeLogger() {
			//TODO
		}

		private void rollLogger() {
			//TODO
		}
	}

	public static void main(String @NotNull [] args) throws Exception {
		Thread.setDefaultUncaughtExceptionHandler((t, e) -> {
			//noinspection CallToPrintStackTrace
			e.printStackTrace();
			logger.error("uncaught exception in {}:", t, e);
		});

		String host = null;
		int port = 0;
		int threadCount = 0;
		String path = "log";

		for (int i = 0; i < args.length; ++i) {
			switch (args[i]) {
			case "-host":
				host = args[++i];
				if (host.isBlank())
					host = null;
				break;
			case "-port":
				port = Integer.parseInt(args[++i]);
				break;
			case "-threads":
				threadCount = Integer.parseInt(args[++i]);
				break;
			case "-path":
				path = args[++i].trim();
				break;
			default:
				throw new IllegalArgumentException("unknown argument: " + args[i]);
			}
		}

		if (threadCount < 1)
			threadCount = Runtime.getRuntime().availableProcessors();
		Task.initThreadPool(Task.newCriticalThreadPool("ZezeTaskPool"),
				Executors.newSingleThreadScheduledExecutor(new ThreadFactoryWithName("ZezeScheduledPool")));
		if (Selectors.getInstance().getCount() < threadCount)
			Selectors.getInstance().add(threadCount - Selectors.getInstance().getCount());

		new BinLoggerService(path).start(host, port);
		synchronized (Thread.currentThread()) {
			Thread.currentThread().wait();
		}
	}
}
