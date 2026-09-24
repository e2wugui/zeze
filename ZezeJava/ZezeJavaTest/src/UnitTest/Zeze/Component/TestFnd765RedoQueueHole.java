package UnitTest.Zeze.Component;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import Zeze.Builtin.RedoQueue.BTaskId;
import Zeze.Component.RedoQueue;
import Zeze.Config;
import Zeze.Serialize.ByteBuffer;
import Zeze.Util.RocksDatabase;
import harness.Fast;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Property;
import org.junit.jupiter.api.Test;

/**
 * FND7-65 回归：RedoQueue 泵读水位下一跳取不到任务时静默 return——put 失败留下的
 * 空洞使队列永久停摆且无日志无重试，不可诊断。
 * 修复：add 先以 lastTaskId+1 落盘成功再推进内存 lastTaskId；value==null 分支补
 * FATAL（洞不会自愈，让运维介入）。
 * 测试（@Fast 可测"停摆告警"半边）：add 两个任务后直接用 tableTaskQueue.delete
 * 删掉第一个制造空洞，再 add 触发泵，断言 FATAL 日志（修复前无任何日志）。
 */
@Fast
public class TestFnd765RedoQueueHole {

	@Test
	public void testHoleTriggersFatalLog() throws Exception {
		var queueName = "fnd7_65_redo_" + System.nanoTime(); // 唯一rocksdb目录
		var config = new Config();
		config.setServiceManager("disable");
		var queue = new RedoQueue(queueName, config);
		try {
			queue.start();
			queue.add(1, new BTaskId()); // taskId=1
			queue.add(1, new BTaskId()); // taskId=2（无socket，泵在发送前返回，任务留存）

			// 直接制造空洞：删除taskId=1
			var tableField = RedoQueue.class.getDeclaredField("tableTaskQueue");
			tableField.setAccessible(true);
			var table = (RocksDatabase.Table)tableField.get(queue);
			var key = ByteBuffer.Allocate(9);
			key.WriteLong(1L);
			table.delete(key.Bytes, 0, key.WriteIndex);

			// 触发泵：add第三个任务，泵读lastDoneTaskId+1=1命中空洞
			var coreLogger = (Logger)LogManager.getLogger(RedoQueue.class);
			var appender = new RecordingAppender();
			appender.start();
			coreLogger.addAppender(appender);
			try {
				queue.add(1, new BTaskId()); // taskId=3
			} finally {
				coreLogger.removeAppender(appender);
				appender.stop();
			}
			var fatalLogged = appender.events.stream()
					.anyMatch(e -> e.getLevel() == Level.FATAL
							&& e.getMessage().getFormattedMessage().contains("task queue hole"));
			assertTrue(fatalLogged, "命中空洞必须FATAL告警（修复前静默停摆无日志）");
		} finally {
			queue.stop();
			deleteRecursively(Path.of(queueName));
		}
	}

	private static void deleteRecursively(Path root) throws Exception {
		if (!Files.exists(root))
			return;
		try (var walk = Files.walk(root)) {
			walk.sorted(Comparator.reverseOrder()).forEach(p -> {
				try {
					Files.delete(p);
				} catch (Exception ignored) {
				}
			});
		}
	}

	/** 最小录制appender：捕获指定logger的事件供断言（log4j-core不带test appender）。 */
	private static final class RecordingAppender extends AbstractAppender {
		private final List<LogEvent> events = new CopyOnWriteArrayList<>();

		private RecordingAppender() {
			super("fnd765-recorder", null, null, true, Property.EMPTY_ARRAY);
		}

		@Override
		public void append(LogEvent event) {
			events.add(event.toImmutable());
		}
	}
}
