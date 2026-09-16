package UnitTest.Zeze.Services;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;

import Zeze.Serialize.ByteBuffer;
import Zeze.Services.BinLogger;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * FND6-29姊妹漏网：写异常恢复分支重开后缺停机复查。FND5-39把!started检查放在重开
 * 之前，只覆盖「先停机后进恢复」；停机落在检查之后、且openDay因NFS/磁盘抖动停滞
 * 跨越stop放弃join的点时，stopLogger已forceClose重开前字段并释放目录锁，openDay
 * 重开的新五件套无人负责关闭（泄漏到进程结束），重试写残余批与新实例双写同日bin/pos。
 * 修复：openDay返回后复查!started，停机关闭新五件套、经exitOnStop丢弃残余批退出。
 * 注入：binFile替换为「write抛异常+close阻塞」的闸流——写线程停在恢复分支
 * forceClose(binFile)里（检查已过、openDay未跑），测试置started=false（模拟stop
 * 放弃join后的状态），放行后重开完成。断言（stopLogger收尾之后）：当日bin为空
 * ——修复前残余批写入重开流缓冲、经收尾close落盘(非0)，修复后批被丢弃(恒0)。
 */
@Fast
public class TestBinLoggerRecoverStopDiscard {
	private static final int LOG_SIZE = 16 * 1024;

	static {
		// BinLogger外层类静态初始化经ZezeCounter.instance.getRunTimeObserver触达
		// PerfCounter——需先初始化（Application.start内为tryInit，单测环境手工补）。
		Zeze.Util.Task.tryInitThreadPool();
		Zeze.Util.ZezeCounter.tryInit();
	}

	/** write()抛IOException触发恢复分支；close()先阻塞在stall闸上再真关闭（模拟重开耗时长、跨过stop放弃点）。 */
	private static final class WriteFailCloseStallStream extends BufferedOutputStream {
		final CountDownLatch stall = new CountDownLatch(1);
		final CountDownLatch entered = new CountDownLatch(1);

		WriteFailCloseStallStream(String fileName) throws IOException {
			super(new FileOutputStream(fileName, true));
		}

		@Override
		public synchronized void write(byte[] b, int off, int len) throws IOException {
			throw new IOException("injected write failure");
		}

		@Override
		public synchronized void close() throws IOException {
			entered.countDown();
			try {
				stall.await();
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
			}
			super.close();
		}
	}

	@Test
	public void testStopDuringRecoverReopenDiscardsBatchAndClosesNewHandles() throws Exception {
		var serviceClass = BinLogger.BinLoggerService.class;
		var toDayStamp = method(serviceClass, "toDayStamp", long.class);
		var toDayStr = method(serviceClass, "toDayStr", int.class);
		var startLogger = method(serviceClass, "startLogger");
		var stopLogger = method(serviceClass, "stopLogger");
		var processLogData = method(serviceClass, "processLogData", BinLogger.LogData.class);
		var startedField = field(serviceClass, "started");
		var binFileField = field(serviceClass, "binFile");

		var dir = Files.createTempDirectory("binlogger-recoverstop");
		var today = (int)toDayStamp.invoke(null, System.currentTimeMillis());
		var todayBin = Path.of(dir.toString(), toDayStr.invoke(null, today) + ".bin");

		var service = new BinLogger.BinLoggerService(dir.toString());
		try {
			startedField.set(service, true); // 绕过网络监听，只启动写日志线程
			startLogger.invoke(service);
			Assertions.assertTrue(Files.exists(todayBin), "startLogger的openDay应已创建当日bin");

			var poison = new WriteFailCloseStallStream(todayBin.toString());
			binFileField.set(service, poison);

			processLogData.invoke(service,
					new BinLogger.LogData(12345L, 1L, ByteBuffer.Wrap(new byte[LOG_SIZE])));
			Assertions.assertTrue(poison.entered.await(10, java.util.concurrent.TimeUnit.SECONDS),
					"写线程必须已进入恢复分支的forceClose(binFile)停顿（!started检查已过）");

			// 模拟stop超时放弃join后的状态：started=false。
			startedField.set(service, false);
			poison.stall.countDown(); // 放行：forceClose完成、openDay重开新五件套

			var thread = (Thread)field(serviceClass, "writeLogThread").get(service);
			thread.join(10_000);
			Assertions.assertFalse(thread.isAlive(), "写线程必须退出");
		} finally {
			startedField.set(service, false);
			stopLogger.invoke(service); // 收尾：join写线程并关闭残留流（收尾close会落盘缓冲）
		}

		// FND6-29姊妹核心（在stopLogger之后断言）：修复前重开后无复查，残余批经退避后
		// 重试写入重开流的缓冲、经收尾close落盘 → 非空；修复后重开遇停机直接丢弃残余批
		// 并关闭新五件套，任何时点都不落盘 → 恒为空。
		Assertions.assertEquals(0, Files.size(todayBin),
				"恢复重开遇停机必须丢弃残余批并关闭新五件套，不得写入");
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
