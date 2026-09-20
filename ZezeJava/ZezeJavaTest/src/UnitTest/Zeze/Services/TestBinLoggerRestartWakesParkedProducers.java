package UnitTest.Zeze.Services;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.atomic.AtomicBoolean;

import Zeze.Serialize.ByteBuffer;
import Zeze.Services.BinLogger;
import Zeze.Transaction.EmptyBean;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * S1-F1 回归：stop 超时放弃 join 后写线程（可能已死或经 stale 路径退出且不 signal），
 * 满队等 cond 的生产者再无人唤醒；同实例 startLogger 重启若只无锁复位队列（新队列为空，
 * 新写线程只在换队时 signal），旧生产者将永久挂起 IO 线程。
 * 修复：startLogger 复位 waitingQueue/writeLogQueue 必须持 queueLock 并 signalAll。
 * 附：S1-F2 重启数据往返烟测（openDay 打开/提交分离后 start/stop/start 正常落盘）、
 * S1-F4 agent.stop() 后 sendLog 返回 false 不再 NPE。
 */
@Fast
public class TestBinLoggerRestartWakesParkedProducers {
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

	private static void stopService(Object service, Field startedField, Field writeLogThreadField,
									Method stopLogger) throws Exception {
		startedField.set(service, false);
		var thread = (Thread)writeLogThreadField.get(service);
		if (thread != null) {
			thread.join(5000);
			if (thread.isAlive())
				thread.interrupt();
		}
		stopLogger.invoke(service);
	}

	@Test
	public void testRestartWakesParkedProducer() throws Exception {
		var serviceClass = BinLogger.BinLoggerService.class;
		var startLogger = method(serviceClass, "startLogger");
		var stopLogger = method(serviceClass, "stopLogger");
		var startedField = field(serviceClass, "started");
		var writeLogThreadField = field(serviceClass, "writeLogThread");
		var waitingQueueField = field(serviceClass, "waitingQueue");
		var queueLockField = field(serviceClass, "queueLock");
		var queueLockCondField = field(serviceClass, "queueLockCond");

		var dir = Files.createTempDirectory("wt5-binlogger-restart-wake");
		var service = new BinLogger.BinLoggerService(dir.toString());
		try {
			startedField.set(service, true); // 绕过super.start()的网络监听，只启动写日志线程
			startLogger.invoke(service);

			// 模拟满队park的生产者：持queueLock置waitingQueue=true后await（真实生产者在
			// processLogData中的park形态）。
			var queueLock = (Lock)queueLockField.get(service);
			var cond = (Condition)queueLockCondField.get(service);
			var awakened = new AtomicBoolean(false);
			var parked = new Thread(() -> {
				queueLock.lock();
				try {
					waitingQueueField.setBoolean(service, true);
					cond.await();
					awakened.set(true);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
				} catch (ReflectiveOperationException e) {
					throw new RuntimeException(e);
				} finally {
					queueLock.unlock();
				}
			});
			parked.start();
			for (int i = 0; i < 1000 && !waitingQueueField.getBoolean(service); i++)
				Thread.sleep(10);
			Assertions.assertTrue(waitingQueueField.getBoolean(service), "模拟生产者必须已park");

			// 停机：写线程经队列空路径退出（不signal——正是hazard所在），parked生产者仍挂起。
			stopService(service, startedField, writeLogThreadField, stopLogger);
			Assertions.assertTrue(parked.isAlive(), "stop的空队列退出路径不唤醒生产者（修复不改该点）");

			// 同实例重启：startLogger复位队列必须持锁signalAll，唤醒parked生产者。
			startedField.set(service, true);
			startLogger.invoke(service);
			parked.join(5000);
			Assertions.assertFalse(parked.isAlive(), "startLogger复位队列必须signalAll唤醒parked生产者");
			Assertions.assertTrue(awakened.get());
			Assertions.assertFalse(waitingQueueField.getBoolean(service), "waitingQueue必须随重启复位");
		} finally {
			stopService(service, startedField, writeLogThreadField, stopLogger);
		}
	}

	/** S1-F2：openDay打开/提交分离重构后的重启数据往返（stop→start→继续落盘）。 */
	@Test
	public void testRestartDataRoundtrip() throws Exception {
		var serviceClass = BinLogger.BinLoggerService.class;
		var toDayStamp = method(serviceClass, "toDayStamp", long.class);
		var toDayStr = method(serviceClass, "toDayStr", int.class);
		var startLogger = method(serviceClass, "startLogger");
		var stopLogger = method(serviceClass, "stopLogger");
		var processLogData = method(serviceClass, "processLogData", BinLogger.LogData.class);
		var startedField = field(serviceClass, "started");
		var writeLogThreadField = field(serviceClass, "writeLogThread");
		var binFileField = field(serviceClass, "binFile");

		var dir = Files.createTempDirectory("wt5-binlogger-restart-roundtrip");
		var service = new BinLogger.BinLoggerService(dir.toString());
		try {
			startedField.set(service, true);
			startLogger.invoke(service);
			processLogData.invoke(service, new BinLogger.LogData(1L, 1L, ByteBuffer.Wrap(new byte[LOG_SIZE])));
			var binFile = java.nio.file.Path.of(dir.toString(), String.valueOf(
					toDayStr.invoke(null, toDayStamp.invoke(null, System.currentTimeMillis()))) + ".bin").toFile();
			for (int i = 0; i < 1000 && binFile.length() < LOG_SIZE; i++)
				Thread.sleep(10);
			Assertions.assertEquals(LOG_SIZE, binFile.length(), "首轮必须正常落盘");

			// 重启：stopLogger（关闭五件套并释放目录锁）→ startLogger（重新对账打开并提交）
			stopService(service, startedField, writeLogThreadField, stopLogger);
			Assertions.assertNull(binFileField.get(service), "stopLogger必须关闭并清空当日五件套");
			startedField.set(service, true);
			startLogger.invoke(service);
			Assertions.assertNotNull(binFileField.get(service), "startLogger必须重新提交当日五件套");
			processLogData.invoke(service, new BinLogger.LogData(2L, 1L, ByteBuffer.Wrap(new byte[LOG_SIZE])));
			for (int i = 0; i < 1000 && binFile.length() < LOG_SIZE * 2L; i++)
				Thread.sleep(10);
			Assertions.assertEquals(LOG_SIZE * 2L, binFile.length(), "重启后必须继续在既有文件上追加（对账不回退）");
		} finally {
			stopService(service, startedField, writeLogThreadField, stopLogger);
		}
	}

	/** S1-F4：stop()置空connector后sendLog按"发送失败"返回false，不再向调用方抛NPE。 */
	@Test
	public void testSendLogAfterStopReturnsFalse() {
		var agent = new BinLogger.BinLoggerAgent(); // 未start，connector==null（与stop后同态）
		Assertions.assertFalse(agent.sendLog(1L, EmptyBean.Data.instance));
	}
}
