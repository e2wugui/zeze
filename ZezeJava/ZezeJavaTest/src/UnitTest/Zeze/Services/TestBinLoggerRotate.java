package UnitTest.Zeze.Services;

import java.io.File;
import java.io.FileOutputStream;
import java.io.RandomAccessFile;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;

import Zeze.Serialize.ByteBuffer;
import Zeze.Services.BinLogger;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * FND3-41 回归：跨天轮转必须在唯一的“开盘”入口重建当日状态。
 * 修复前轮转既不重置 binFileSize 也不按 bin/pos 对账，还用截断方式打开新文件，
 * 于是轮转后写入的 pos 偏移沿用上一天的文件大小，与从 0 开始的当天 bin 整体错位，
 * 进程重启恢复时当天的 pos/ts/dt/id 索引会被全部清零。
 * 本用例把 curDayStamp 拨回前一天来触发轮转，断言轮转既不丢弃当天已有数据，
 * 写入的 pos 偏移也必须与 bin 文件的实际布局一致。
 */
@Fast
public class TestBinLoggerRotate {
	private static final int LOG_SIZE = 16;

	private static Field field(Class<?> cls, String name) throws Exception {
		var f = cls.getDeclaredField(name);
		f.setAccessible(true);
		return f;
	}

	private static Method method(Class<?> cls, String name, Class<?>... parameterTypes) throws Exception {
		var m = cls.getDeclaredMethod(name, parameterTypes);
		m.setAccessible(true);
		return m;
	}

	private static void writeRecord(String fileName, long value) throws Exception {
		var buf = new byte[8];
		ByteBuffer.longLeHandler.set(buf, 0, value);
		try (var out = new FileOutputStream(fileName, true)) {
			out.write(buf);
		}
	}

	private static long readRecord(String fileName, int index) throws Exception {
		var buf = new byte[8];
		try (var raf = new RandomAccessFile(fileName, "r")) {
			raf.seek(index * 8L);
			raf.readFully(buf);
		}
		return ByteBuffer.ToLong(buf, 0);
	}

	@Test
	public void testRotateRebuildsDayState() throws Exception {
		var serviceClass = BinLogger.BinLoggerService.class;
		var toDayStamp = method(serviceClass, "toDayStamp", long.class);
		var toDayStr = method(serviceClass, "toDayStr", int.class);
		var startLogger = method(serviceClass, "startLogger");
		var stopLogger = method(serviceClass, "stopLogger");
		var processLogData = method(serviceClass, "processLogData", BinLogger.LogData.class);
		var startedField = field(serviceClass, "started");
		var curDayStampField = field(serviceClass, "curDayStamp");
		var writeLogThreadField = field(serviceClass, "writeLogThread");
		var writeLogQueueField = field(serviceClass, "writeLogQueue");
		var binFileSizeField = field(serviceClass, "binFileSize");

		var dir = Files.createTempDirectory("binlogger-rotate");
		var logPath = dir.toString().replace('\\', '/') + '/';
		var today = (int)toDayStamp.invoke(null, System.currentTimeMillis());
		var fnPrefix = logPath + toDayStr.invoke(null, today);
		// 当天已经有数据: bin里一条LOG_SIZE的记录, 四个索引各一条指向它的记录.
		try (var out = new FileOutputStream(fnPrefix + ".bin")) {
			out.write(new byte[LOG_SIZE]);
		}
		writeRecord(fnPrefix + ".pos", LOG_SIZE); // 偏移0, 长度LOG_SIZE
		writeRecord(fnPrefix + ".ts", System.currentTimeMillis() << 20);
		writeRecord(fnPrefix + ".dt", 1L);
		writeRecord(fnPrefix + ".id", 12345L);

		var service = new BinLogger.BinLoggerService(dir.toString());
		try {
			startedField.set(service, true); // 绕过super.start()的网络监听, 只启动写日志线程
			startLogger.invoke(service); // 打开当天文件, binFileSize = 已有的LOG_SIZE
			Assertions.assertEquals(LOG_SIZE, ((Number)binFileSizeField.get(service)).longValue());
			// 拨回前一天, 让写线程把这一批日志当作跨天, 触发轮转
			curDayStampField.set(service, today - 1);
			processLogData.invoke(service, new BinLogger.LogData(12345L, 1L, ByteBuffer.Wrap(new byte[LOG_SIZE])));
			// 等写线程做轮转/写入并把缓冲区刷给OS(空闲1秒后flush)
			var binFileName = fnPrefix + ".bin";
			var posFileName = fnPrefix + ".pos";
			var posFile = new File(posFileName);
			for (int i = 0; i < 1000 && posFile.length() < 16; i++)
				Thread.sleep(10);
			Assertions.assertEquals(2 * LOG_SIZE, new File(binFileName).length(), "轮转不能丢弃当天已有数据");
			Assertions.assertEquals(2 * 8L, posFile.length());
			var pos = readRecord(posFileName, 1);
			Assertions.assertEquals(LOG_SIZE, pos >>> 20, "轮转后写入的pos偏移必须与bin文件实际布局一致");
			Assertions.assertEquals(LOG_SIZE, pos & 0xfffff);
		} finally {
			startedField.set(service, false); // 让写日志线程退出
			writeLogQueueField.set(service, null);
			var thread = (Thread)writeLogThreadField.get(service);
			if (thread != null) {
				thread.join(5000);
				if (thread.isAlive())
					thread.interrupt();
			}
			stopLogger.invoke(service);
		}
	}
}
