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
 * FND6-29 回归：跨天轮转分支缺停机复查。stop超时放弃join后（stopLogger已forceClose
 * 当时字段并释放目录锁），写线程恰在轮转中：openDay重开的新五件套赋给字段后无人负责
 * 关闭（句柄泄漏到进程结束），继续写完整批还会在同目录重启新实例时双写同日bin/pos。
 * 修复：轮转完成后复查!started，停机即关闭新句柄、丢弃残余批直接退出（与FND5-39
 * 恢复分支的停机优先于落盘同口径）。
 * 注入：curDayStamp置为昨日强制下轮触发轮转；旧binFile替换为“close阻塞”的闸流——
 * 写线程停在forceClose(oldBinFile)里（轮转进行中），测试置started=false（模拟stop
 * 放弃join后的状态），放行后轮转完成。断言：当日bin文件保持空（残余批被丢弃未写）、
 * 写队列已置空、写线程退出。
 */
@Fast
public class TestBinLoggerRotateStopDiscard {
	// 判别原理：bin流的BIN_BUFFER(1MB)写缓冲大于单条上限(MAX_LOG_SIZE=1M-1)，批内write
	// 滞留缓冲；stopLogger收尾forceClose的close()会把缓冲落盘——故在stopLogger之后断言
	// 当日bin为空：修复前残余批经收尾close落盘(非0)，修复后批被丢弃永远不落盘(0)。
	private static final int LOG_SIZE = 16 * 1024;

	static {
		// BinLogger外层类静态初始化经ZezeCounter.instance.getRunTimeObserver触达
		// PerfCounter——需先初始化（Application.start内为tryInit，单测环境手工补）。
		Zeze.Util.Task.tryInitThreadPool();
		Zeze.Util.ZezeCounter.tryInit();
	}

	/** close()先阻塞在stall闸上，放行后才真正关闭（模拟轮转耗时长、跨过stop放弃点）。 */
	private static final class CloseStallStream extends BufferedOutputStream {
		final CountDownLatch stall = new CountDownLatch(1);
		final CountDownLatch entered = new CountDownLatch(1);

		CloseStallStream(String fileName) throws IOException {
			super(new FileOutputStream(fileName, true));
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
	public void testStopDuringRotationDiscardsBatchAndClosesNewHandles() throws Exception {
		var serviceClass = BinLogger.BinLoggerService.class;
		var toDayStamp = method(serviceClass, "toDayStamp", long.class);
		var toDayStr = method(serviceClass, "toDayStr", int.class);
		var startLogger = method(serviceClass, "startLogger");
		var stopLogger = method(serviceClass, "stopLogger");
		var processLogData = method(serviceClass, "processLogData", BinLogger.LogData.class);
		var startedField = field(serviceClass, "started");
		var curDayStampField = field(serviceClass, "curDayStamp");
		var binFileField = field(serviceClass, "binFile");

		var dir = Files.createTempDirectory("binlogger-rotatestop");
		var today = (int)toDayStamp.invoke(null, System.currentTimeMillis());
		var todayBin = Path.of(dir.toString(), toDayStr.invoke(null, today) + ".bin");

		var service = new BinLogger.BinLoggerService(dir.toString());
		try {
			startedField.set(service, true); // 绕过网络监听，只启动写日志线程
			startLogger.invoke(service);
			Assertions.assertTrue(Files.exists(todayBin), "startLogger的openDay应已创建当日bin");
			Assertions.assertEquals(0, Files.size(todayBin), "初始当日bin为空");

			var poison = new CloseStallStream(todayBin.toString());
			binFileField.set(service, poison); // 旧binFile替换为阻塞闸流
			curDayStampField.setInt(service, today - 1); // 强制下轮触发轮转

			processLogData.invoke(service,
					new BinLogger.LogData(12345L, 1L, ByteBuffer.Wrap(new byte[LOG_SIZE])));
			Assertions.assertTrue(poison.entered.await(10, java.util.concurrent.TimeUnit.SECONDS),
					"写线程必须已进入轮转中的forceClose停顿");

			// 模拟stop超时放弃join后的状态：started=false。
			startedField.set(service, false);
			poison.stall.countDown(); // 放行：轮转完成

			var thread = (Thread)field(serviceClass, "writeLogThread").get(service);
			thread.join(10_000);
			Assertions.assertFalse(thread.isAlive(), "写线程必须退出");
		} finally {
			startedField.set(service, false);
			stopLogger.invoke(service); // 收尾：join写线程并关闭残留流
		}

		// FND6-29核心（在stopLogger之后断言）：修复前残余批滞留缓冲、经收尾close落盘 → 非空；
		// 修复后轮转遇停机直接丢弃残余批，任何时点都不落盘 → 恒为空。
		Assertions.assertEquals(0, Files.size(todayBin),
				"轮转遇停机必须丢弃残余批，不得写入新五件套");
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
