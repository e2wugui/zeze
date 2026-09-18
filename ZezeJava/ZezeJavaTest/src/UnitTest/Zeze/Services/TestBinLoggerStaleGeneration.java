package UnitTest.Zeze.Services;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.util.List;
import java.util.concurrent.locks.Lock;

import Zeze.Serialize.ByteBuffer;
import Zeze.Services.BinLogger;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * FND8-59 回归：stop 超时放弃 join 后旧写线程仍存活，同实例 start 重启把共享 started
 * 置回 true，旧线程的全部退出判定失效而“复活”，与新写线程双写五文件。
 * 修复引入代际令牌：旧线程无论滞留在哪条路径（本例锁竞争/慢批），醒来发现代际不符
 * 即退出，且不动新一代的写队列（不置 null、不偷批）。
 */
@Fast
public class TestBinLoggerStaleGeneration {
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

	private static void stopService(Object service, Field startedField, Field writeLogQueueField,
									Field writeLogThreadField, Method stopLogger) throws Exception {
		startedField.set(service, false);
		writeLogQueueField.set(service, null);
		var thread = (Thread)writeLogThreadField.get(service);
		if (thread != null) {
			thread.join(5000);
			if (thread.isAlive())
				thread.interrupt();
		}
		stopLogger.invoke(service);
	}

	@Test
	public void testStaleWriteThreadExitsAndKeepsNewQueue() throws Exception {
		var serviceClass = BinLogger.BinLoggerService.class;
		var toDayStamp = method(serviceClass, "toDayStamp", long.class);
		var toDayStr = method(serviceClass, "toDayStr", int.class);
		var startLogger = method(serviceClass, "startLogger");
		var stopLogger = method(serviceClass, "stopLogger");
		var processLogData = method(serviceClass, "processLogData", BinLogger.LogData.class);
		var startedField = field(serviceClass, "started");
		var writeLogThreadField = field(serviceClass, "writeLogThread");
		var writeLogQueueField = field(serviceClass, "writeLogQueue");
		var loggerGenerationField = field(serviceClass, "loggerGeneration");
		var queueLockField = field(serviceClass, "queueLock");

		var dir = Files.createTempDirectory("a5-binlogger-stale");
		var service = new BinLogger.BinLoggerService(dir.toString());
		try {
			startedField.set(service, true); // 绕过super.start()的网络监听, 只启动写日志线程
			startLogger.invoke(service);
			var gen1 = ((Number)loggerGenerationField.get(service)).longValue();
			var thread1 = (Thread)writeLogThreadField.get(service);

			// 持有queueLock令写线程滞留在循环顶（等锁），生产者线程同样等锁入队一批；
			// 此期间模拟"stop超时放弃join后同实例start重启"对代际的推进（startLogger
			// 第一句++loggerGeneration），再放锁让双方竞争拿锁。
			var queueLock = (Lock)queueLockField.get(service);
			queueLock.lock();
			var producer = new Thread(() -> {
				try {
					processLogData.invoke(service, new BinLogger.LogData(1L, 1L, ByteBuffer.Wrap(new byte[LOG_SIZE])));
				} catch (Throwable e) {
					throw new RuntimeException(e);
				}
			});
			producer.start();
			loggerGenerationField.setLong(service, gen1 + 1);
			queueLock.unlock();

			producer.join(5000);
			thread1.join(5000);
			Assertions.assertFalse(producer.isAlive());
			Assertions.assertFalse(thread1.isAlive(), "旧代写线程必须按代际令牌退出，不得复活");
			// stale退出不得置空/偷取写队列：谁先拿到锁，批都必须原样留在队列里
			var wlq = (List<?>)writeLogQueueField.get(service);
			Assertions.assertNotNull(wlq, "stale退出不得置空写队列（会连带杀死新代写线程）");
			Assertions.assertEquals(1, wlq.size(), "stale退出不得动写队列内容");

			// 真实重启：stopLogger收尾（旧线程已死，join立即返回）→startLogger开新代
			stopService(service, startedField, writeLogQueueField, writeLogThreadField, stopLogger);
			startedField.set(service, true);
			startLogger.invoke(service);
			var gen2 = ((Number)loggerGenerationField.get(service)).longValue();
			Assertions.assertTrue(gen2 > gen1, "startLogger必须推进代际");
			var thread2 = (Thread)writeLogThreadField.get(service);
			Assertions.assertNotSame(thread1, thread2);

			// 新代写线程正常消费
			var binFile = new File(dir.toString(), toDayStr.invoke(null,
					toDayStamp.invoke(null, System.currentTimeMillis())) + ".bin");
			processLogData.invoke(service, new BinLogger.LogData(2L, 1L, ByteBuffer.Wrap(new byte[LOG_SIZE])));
			for (int i = 0; i < 1000 && binFile.length() < LOG_SIZE; i++)
				Thread.sleep(10);
			Assertions.assertEquals(LOG_SIZE, binFile.length(), "新代写线程必须正常落盘");
			Assertions.assertTrue(thread2.isAlive(), "新代写线程必须仍在运行");
		} finally {
			stopService(service, startedField, writeLogQueueField, writeLogThreadField, stopLogger);
		}
	}
}
