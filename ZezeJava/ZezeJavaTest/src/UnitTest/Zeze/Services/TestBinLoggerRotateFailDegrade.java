package UnitTest.Zeze.Services;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;

import Zeze.Serialize.ByteBuffer;
import Zeze.Services.BinLogger;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * FND8-60 回归：跨天轮转 openDay 抛异常时控制流直接跳到外层 catch，本批一条不写、
 * 轮转持续失败期间日志完全停写，且与 openDay 注释“失败仍可继续用旧文件写”的意图相悖。
 * 修复后轮转块内 catch 异常、退避后落入写入段——降级续写旧文件，curDayStamp 不推进。
 * 本用例把当天 .ts 置只读（服务已持有的句柄不受影响，但 openDay 重开必失败）、
 * 拨回 curDayStamp 触发轮转，断言批仍写入当天文件且轮转未推进。
 */
@Fast
public class TestBinLoggerRotateFailDegrade {
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

	@Test
	public void testRotateFailKeepsWritingCurrentFiles() throws Exception {
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

		var dir = Files.createTempDirectory("a5-binlogger-rotatefail");
		var service = new BinLogger.BinLoggerService(dir.toString());
		var tsFile = new File(dir.toFile(), "unused");
		try {
			startedField.set(service, true); // 绕过super.start()的网络监听, 只启动写日志线程
			startLogger.invoke(service);
			var today = (int)toDayStamp.invoke(null, System.currentTimeMillis());
			var fnPrefix = dir.toString().replace('\\', '/') + '/' + toDayStr.invoke(null, today);
			var binFile = new File(fnPrefix + ".bin");
			var posFile = new File(fnPrefix + ".pos");
			tsFile = new File(fnPrefix + ".ts");

			// 基线：写一条并等落盘
			processLogData.invoke(service, new BinLogger.LogData(1L, 1L, ByteBuffer.Wrap(new byte[LOG_SIZE])));
			for (int i = 0; i < 1000 && binFile.length() < LOG_SIZE; i++)
				Thread.sleep(10);
			Assertions.assertEquals(LOG_SIZE, binFile.length());

			// 轮转必然失败：当天.ts置只读——服务已持有的流不受影响（继续可写），
			// 但轮转openDay重开(RandomAccessFile "rw")必抛FileNotFoundException
			Assertions.assertTrue(tsFile.setReadOnly(), "置只读必须成功（Windows/非root Linux均生效）");
			curDayStampField.set(service, today - 1); // 拨回前一天触发轮转（目标=今天）
			processLogData.invoke(service, new BinLogger.LogData(2L, 1L, ByteBuffer.Wrap(new byte[LOG_SIZE])));

			// 降级续写：轮转失败后本批必须继续写当天（旧）文件
			for (int i = 0; i < 1000 && binFile.length() < 2 * LOG_SIZE; i++)
				Thread.sleep(10);
			Assertions.assertEquals(2 * LOG_SIZE, binFile.length(), "轮转失败必须降级续写旧文件，不得停写");
			for (int i = 0; i < 1000 && posFile.length() < 2 * 8L; i++)
				Thread.sleep(10);
			Assertions.assertEquals(2 * 8L, posFile.length(), "索引与数据同步写入");
			// 轮转未推进：curDayStamp保持拨回值（未成功轮转）
			Assertions.assertEquals(today - 1, ((Number)curDayStampField.get(service)).intValue(),
				"轮转失败curDayStamp不得推进");
		} finally {
			tsFile.setWritable(true); // 恢复可写，避免残留只读临时文件
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
