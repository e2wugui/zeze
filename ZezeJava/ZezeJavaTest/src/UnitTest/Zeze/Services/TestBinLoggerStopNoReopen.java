package UnitTest.Zeze.Services;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.util.concurrent.CountDownLatch;

import Zeze.Serialize.ByteBuffer;
import Zeze.Services.BinLogger;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * FND5-39 回归：BinLogger stop超时放弃join后（10s写停顿场景），写线程从
 * 慢速写抛异常进恢复分支——原顺序先forceClose+openDay再查!started：当日
 * 五件套被重开赋给字段后线程退出，nobody再关闭（句柄泄漏到进程结束+
 * stop已返回期间继续写；目录锁已释放时与第二个实例双写同日文件）。
 * 修复：恢复分支先判!started丢弃退出，不再openDay。
 * 注入：binFile替换为“阻塞后失败”的毒流——写线程停在write里等闸，
 * 测试置started=false（模拟stop放弃join后的状态），放行后write抛异常
 * 进恢复分支。断言字段上仍是毒流实例（未被openDay替换）。
 */
@Fast
public class TestBinLoggerStopNoReopen {
	private static final int LOG_SIZE = 16;

	static {
		// BinLogger外层类静态初始化经ZezeCounter.instance.getRunTimeObserver触达
		// PerfCounter——需先初始化（Application.start内为tryInit，单测环境手工补）。
		Zeze.Util.Task.tryInitThreadPool();
		Zeze.Util.ZezeCounter.tryInit();
	}

	/** 第一次write阻塞在stall闸上，放行后抛IOException（模拟慢速写最终失败）。 */
	private static final class StallThenFailStream extends BufferedOutputStream {
		final CountDownLatch stall = new CountDownLatch(1);
		final CountDownLatch entered = new CountDownLatch(1);
		private boolean failed;

		StallThenFailStream(String fileName) throws IOException {
			super(new FileOutputStream(fileName, true));
		}

		@Override
		public synchronized void write(byte[] b, int off, int len) throws IOException {
			if (!failed) {
				entered.countDown();
				try {
					stall.await();
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				}
				failed = true;
				throw new IOException("stall then fail");
			}
			super.write(b, off, len);
		}
	}

	@Test
	public void testStopAfterWriteStallDoesNotReopen() throws Exception {
		var serviceClass = BinLogger.BinLoggerService.class;
		var toDayStamp = method(serviceClass, "toDayStamp", long.class);
		var toDayStr = method(serviceClass, "toDayStr", int.class);
		var startLogger = method(serviceClass, "startLogger");
		var stopLogger = method(serviceClass, "stopLogger");
		var processLogData = method(serviceClass, "processLogData", BinLogger.LogData.class);
		var startedField = field(serviceClass, "started");
		var binFileField = field(serviceClass, "binFile");

		var dir = Files.createTempDirectory("binlogger-stopnoreopen");
		var logPath = dir.toString().replace('\\', '/') + '/';
		var today = (int)toDayStamp.invoke(null, System.currentTimeMillis());
		var fnPrefix = logPath + toDayStr.invoke(null, today);

		var service = new BinLogger.BinLoggerService(dir.toString());
		try {
			startedField.set(service, true); // 绕过网络监听，只启动写日志线程
			startLogger.invoke(service);
			var poison = new StallThenFailStream(fnPrefix + ".bin");
			binFileField.set(service, poison);

			processLogData.invoke(service,
					new BinLogger.LogData(12345L, 1L, ByteBuffer.Wrap(new byte[LOG_SIZE])));
			Assertions.assertTrue(poison.entered.await(10, java.util.concurrent.TimeUnit.SECONDS), "写线程必须已进入write停顿");

			// 模拟stop超时放弃join后的状态：started=false（stop已关闭五件套并释放目录锁）。
			startedField.set(service, false);
			poison.stall.countDown(); // 放行：write抛异常进恢复分支

			// 等写线程走完恢复分支退出。
			var thread = (Thread)field(serviceClass, "writeLogThread").get(service);
			thread.join(10_000);
			Assertions.assertFalse(thread.isAlive(), "写线程必须退出");

			// FND5-39核心：恢复分支不得openDay重开——字段必须仍是毒流实例（未被新流替换）。
			Assertions.assertSame(poison, binFileField.get(service),
					"stop后的恢复分支不得openDay重开文件（FND5-39）");
		} finally {
			startedField.set(service, false);
			stopLogger.invoke(service); // 收尾：join写线程并关闭残留流
		}
	}

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
}
