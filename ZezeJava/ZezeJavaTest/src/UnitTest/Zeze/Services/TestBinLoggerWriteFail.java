package UnitTest.Zeze.Services;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
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
 * FND3-43回归：写文件异常后的恢复不得重复入库、不得错位。
 * 未修复：整批滞留readLogQueue被下轮整批重写（前i-1条重复入库）；bin半条脏尾落盘而
 * 内存binFileSize未计——后续pos偏移系统性偏小，按pos读取永久错位（重启对账只在pos
 * 偏大时截断，恒真不修）。
 * 修复：失败断点（已完整写成的written条前缀出队）+forceClose+openDay按bin实际长度
 * 对账重开（脏尾成为未索引gap），剩余条目重写。
 * 注入：反射替换binFile为第N次write抛异常的子类（可选先写一半字节模拟脏尾），确定性复现。
 */
@Fast
public class TestBinLoggerWriteFail {
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

	private static long readRecord(String fileName, int index) throws Exception {
		var buf = new byte[8];
		try (var raf = new RandomAccessFile(fileName, "r")) {
			raf.seek(index * 8L);
			raf.readFully(buf);
		}
		return ByteBuffer.ToLong(buf, 0);
	}

	/** 第failAtCall次write(byte[],int,int)抛IOException；可选先写一半字节模拟半条脏尾。 */
	private static final class PoisonBinStream extends BufferedOutputStream {
		private final int failAtCall;
		private final boolean halfBeforeFail;
		private int calls;
		private boolean failed;

		PoisonBinStream(String fileName, int failAtCall, boolean halfBeforeFail) throws IOException {
			super(new FileOutputStream(fileName, true));
			this.failAtCall = failAtCall;
			this.halfBeforeFail = halfBeforeFail;
		}

		@Override
		public synchronized void write(byte[] b, int off, int len) throws IOException {
			if (!failed && calls++ == failAtCall) {
				failed = true;
				if (halfBeforeFail)
					super.write(b, off, len / 2); // 半条落盘（留在缓冲，close/flush时到OS）
				throw new IOException("poison write fail");
			}
			super.write(b, off, len);
		}
	}

	private interface Assertion {
		void check(String binFileName, String posFileName) throws Exception;
	}

	private void writeFailScenario(int failAtCall, boolean halfBeforeFail, Assertion assertion) throws Exception {
		var serviceClass = BinLogger.BinLoggerService.class;
		var toDayStamp = method(serviceClass, "toDayStamp", long.class);
		var toDayStr = method(serviceClass, "toDayStr", int.class);
		var startLogger = method(serviceClass, "startLogger");
		var stopLogger = method(serviceClass, "stopLogger");
		var processLogData = method(serviceClass, "processLogData", BinLogger.LogData.class);
		var startedField = field(serviceClass, "started");
		var writeLogThreadField = field(serviceClass, "writeLogThread");
		var writeLogQueueField = field(serviceClass, "writeLogQueue");
		var binFileField = field(serviceClass, "binFile");

		var dir = Files.createTempDirectory("binlogger-writefail");
		var logPath = dir.toString().replace('\\', '/') + '/';
		var today = (int)toDayStamp.invoke(null, System.currentTimeMillis());
		var fnPrefix = logPath + toDayStr.invoke(null, today);

		var service = new BinLogger.BinLoggerService(dir.toString());
		try {
			startedField.set(service, true); // 绕过网络监听，只启动写日志线程
			startLogger.invoke(service);
			// 毒化bin：第failAtCall次写抛异常（可选半条先落盘）。
			binFileField.set(service, new PoisonBinStream(fnPrefix + ".bin", failAtCall, halfBeforeFail));

			for (int k = 0; k < 3; k++)
				processLogData.invoke(service,
						new BinLogger.LogData(12345L, 1L, ByteBuffer.Wrap(new byte[LOG_SIZE])));

			// 等写线程处理完（含未修复时的整批重写轮）并刷给OS：close由stopLogger完成，
			// 这里轮询pos行数到3后再多等一轮，保证旧代码的重写轮也已发生。
			var posFile = new File(fnPrefix + ".pos");
			for (int i = 0; i < 1000 && posFile.length() < 3 * 8L; i++)
				Thread.sleep(10);
			Thread.sleep(300);

			assertion.check(fnPrefix + ".bin", fnPrefix + ".pos");
		} finally {
			startedField.set(service, false);
			writeLogQueueField.set(service, null);
			var thread = (Thread)writeLogThreadField.get(service);
			if (thread != null) {
				thread.join(5000);
				if (thread.isAlive())
					thread.interrupt();
			}
			stopLogger.invoke(service); // close会flush剩余缓冲到OS，文件长度定局
		}
	}

	@Test
	public void testNoDuplicateOnWriteFail() throws Exception {
		// 第2条(entry#1)的bin写一次性失败：恢复后必须恰好三条、偏移连续对齐。
		writeFailScenario(1, false, (binFileName, posFileName) -> {
			Assertions.assertEquals(3 * (long)LOG_SIZE, new File(binFileName).length(),
					"写失败恢复不得重复入库（FND3-43：滞留队列整批重写）");
			Assertions.assertEquals(3 * 8L, new File(posFileName).length(), "pos行数必须与条数一致");
			for (int i = 0; i < 3; i++) {
				var pos = readRecord(posFileName, i);
				Assertions.assertEquals(((long)i * LOG_SIZE) << 20 | LOG_SIZE, pos,
						"pos偏移必须与bin实际布局一致（第" + i + "行）");
			}
		});
	}

	@Test
	public void testTornTailReconciled() throws Exception {
		// 第2条写了一半字节后失败：脏尾8字节成为未索引gap，剩余条目从实际长度(16+8=24)对齐重写。
		writeFailScenario(1, true, (binFileName, posFileName) -> {
			Assertions.assertEquals(24 + 2 * (long)LOG_SIZE, new File(binFileName).length(),
					"半条脏尾后必须按bin实际长度对齐重写（脏尾成为gap）");
			Assertions.assertEquals(3 * 8L, new File(posFileName).length());
			var pos1 = readRecord(posFileName, 1);
			Assertions.assertEquals(24L << 20 | LOG_SIZE, pos1,
					"脏尾后的pos偏移必须从实际长度开始（FND3-43：内存binFileSize脱钩）");
		});
	}
}
