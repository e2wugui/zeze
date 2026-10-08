package Zeze.MQ;

import harness.Extra;
import harness.Fast;
import java.io.File;
import java.util.ArrayDeque;
import java.util.Random;
import Zeze.Application;
import Zeze.Builtin.MQ.BMessage;
import Zeze.MQ.MQFileWithIndex;
import Zeze.Util.OutLong;
import Zeze.Util.RocksDatabase;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@Fast
@Extra
public class TestFileWithIndexed {
	@Test
	public void testFile() throws Exception {
		var home = "testFileWithIndexed";
		Application.deleteDirectory(new File(home));
		var database = new RocksDatabase(home);
		var file = new MQFileWithIndex(home, database, "topic", 0);
		file.trunkFileSize = 2048;
		file.makeIndexPeriod = 10;
		try {
			var queueOrigin = new ArrayDeque<BMessage.Data>();
			var rand = new Random();
			for (var i = 0; i < 256; ++i) {
				var message = new BMessage.Data();
				for (var j = 0; j < 5; ++j)
					message.getProperties().put(String.valueOf(j), String.valueOf(rand.nextInt()));
				file.appendMessage(message);
				queueOrigin.offer(message);
			}
			{
				var queue = new ArrayDeque<BMessage.Data>();
				var first = new OutLong();
				var last = new OutLong();
				file.calculateFill(queue, first, last, 300);
				file.fillMessage(queue, first.value, last.value);
				var queueEquals = new ArrayDeque<>(queueOrigin);
				Assertions.assertEquals(queueEquals.size(), queue.size());
				for (var i = 0; i < queue.size(); ++i) {
					var origin = queueEquals.poll();
					var fill = queue.poll();
					Assertions.assertEquals(origin, fill);
				}
			}
			// 这里本不需要循环256次，确保全部清空才写了这么多。
			for (var i = 0; i < 256; ++i) {
				var poll = rand.nextInt(10);
				for (var j = 0; j < poll; ++j) {
					file.increaseFirstMessageId();
					queueOrigin.poll();
				}
				{
					var queue = new ArrayDeque<BMessage.Data>();
					var first = new OutLong();
					var last = new OutLong();
					file.calculateFill(queue, first, last, 300);
					file.fillMessage(queue, first.value, last.value);
					//System.out.println("=====>" + i);
					var queueEquals = new ArrayDeque<>(queueOrigin);
					Assertions.assertEquals(queueEquals.size(), queue.size());
					for (var k = 0; k < queue.size(); ++k) {
						var origin = queueEquals.poll();
						var fill = queue.poll();
						Assertions.assertEquals(origin, fill);
					}
				}
			}
		} finally {
			database.close();
			file.close();
			// 注释掉这一行可以看到持久化的结果。
			Application.deleteDirectory(new File(home));
		}
	}
}
