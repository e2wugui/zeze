package MQ;

import Zeze.Builtin.MQ.BMessage;
import Zeze.Builtin.MQ.BOptions;
import Zeze.Config;
import Zeze.MQ.MQ;
import Zeze.MQ.MQAgent;
import Zeze.MQ.MQConsumer;
import Zeze.MQ.MQManager;
import Zeze.MQ.MQProducer;
import Zeze.Util.Task;
import org.junit.Test;
import org.rocksdb.RocksDBException;

public class TestMQ {
	@Test
	public void testMQ() throws Exception {
		Task.tryInitThreadPool();

		var master = new Zeze.MQ.Master.Main("mqmaster.xml");
		var manager0 = new MQManager("mqmanager0", "mqmanager0.xml");
		var manager1 = new MQManager("mqmanager1", "mqmanager1.xml");
		var manager2 = new MQManager("mqmanager2", "mqmanager2.xml");
		MQProducer producer = null;
		MQConsumer consumer = null;
		try {
			master.start();
			manager0.start();
			manager1.start();
			manager2.start();
			var topic = "topicTest";
			try {
				MQ.createMQ(topic, 6, new BOptions.Data(BOptions.Single));
			} catch (Exception ex) {
				// skip
			}
			producer = new MQProducer(topic);
			producer.sendMessage(new BMessage.Data());

			consumer = new MQConsumer(topic, (pushMessage) -> {
				System.out.println("consumer " + pushMessage.getTopic());
			});

		} finally {
			if (producer != null)
				producer.close();
			if (consumer != null)
				consumer.close();
			manager0.stop();
			manager1.stop();
			manager2.stop();
			master.stop();
		}
	}
}
