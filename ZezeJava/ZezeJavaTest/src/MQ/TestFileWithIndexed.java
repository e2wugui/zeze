package MQ;

import java.io.File;
import java.util.ArrayDeque;
import java.util.Random;
import Zeze.Application;
import Zeze.Builtin.MQ.BMessage;
import Zeze.MQ.MQFileWithIndex;
import Zeze.Util.RocksDatabase;
import org.junit.Assert;
import org.junit.Test;

public class TestFileWithIndexed {
	@Test
	public void testFile() throws Exception {
		var home = "testFileWithIndexed";
		Application.deleteDirectory(new File(home));
		var database = new RocksDatabase(home);
		MQFileWithIndex.trunkFileSize = 128;
		MQFileWithIndex.makeIndexPeriod = 10;
		var file = new MQFileWithIndex(home, database, "topic", 0);
		try {
			var queueOrigin = new ArrayDeque<BMessage.Data>();
			for (var i = 0; i < 256; ++i) {
				var message = new BMessage.Data();
				for (var j = 0; j < 5; ++j)
					message.getProperties().put(String.valueOf(j), "0");
				file.appendMessage(message);
				queueOrigin.offer(message);
			}
			{
				var queue = new ArrayDeque<BMessage.Data>();
				file.fillMessage(queue, 300);
				var queueEquals = new ArrayDeque<>(queueOrigin);
				Assert.assertEquals(queueEquals.size(), queue.size());
				for (var i = 0; i < queue.size(); ++i) {
					var origin = queueEquals.poll();
					var fill = queue.poll();
					Assert.assertEquals(origin, fill);
				}
			}
			var rand = new Random();
			// 这里本不需要循环256次，确保全部清空才写了这么多。
			for (var i = 0; i < 256; ++i) {
				var poll = rand.nextInt(10);
				for (var j = 0; j < poll; ++j) {
					file.increaseFirstMessageId();
					queueOrigin.poll();
				}
				{
					var queue = new ArrayDeque<BMessage.Data>();
					file.fillMessage(queue, 300);
					//System.out.println("=====>" + i);
					var queueEquals = new ArrayDeque<>(queueOrigin);
					Assert.assertEquals(queueEquals.size(), queue.size());
					for (var k = 0; k < queue.size(); ++k) {
						var origin = queueEquals.poll();
						var fill = queue.poll();
						Assert.assertEquals(origin, fill);
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
