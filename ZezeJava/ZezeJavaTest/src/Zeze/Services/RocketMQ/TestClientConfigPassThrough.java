package Zeze.Services.RocketMQ;

import Zeze.Application;
import Zeze.Config;
import harness.Fast;
import harness.FastServerIds;
import org.apache.rocketmq.client.ClientConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * ClientConfig 透传契约（FND30 rocketmq-02）：构造器接收 ClientConfig 的参数形态承诺
 * "传入即生效"——修复前只透传 namesrvAddr，namespace/instanceName 等被静默丢弃：
 * namespace 丢弃=多租户形态下无报错的静默消息不可达（对端消息进 NS1%topic 空间而
 * 本端订阅裸 topic，两端字符串看起来都"正确"）；instanceName 丢弃=同 JVM 同组多实例
 * 部署在 start 时才报 clientId 重复，报错远离误配根因。
 */
@Fast
@ResourceLock("rocketmq.producer.processSlot") // 构造 Producer 占用进程级静态计数，与其它 Producer 测试串行
public class TestClientConfigPassThrough {
	private static final int SERVER_ID = FastServerIds.TEST_CLIENT_CONFIG_PASS_THROUGH;

	private Application app;

	private static Application newApp(int serverId, String name) throws Exception {
		var conf = new Config();
		conf.setServiceManager("disable");
		conf.setServerId(serverId);
		conf.setDefaultTableConf(new Config.TableConf()); // 裸Config不会补默认值
		var dbConf = new Config.DatabaseConf();
		dbConf.setDatabaseType(Config.DbType.Memory);
		dbConf.setDatabaseUrl("rocketmq_cc_passthrough_" + serverId);
		conf.getDatabaseConfMap().putIfAbsent("", dbConf);
		return new Application(name, conf);
	}

	@BeforeEach
	public void setUp() throws Exception {
		app = newApp(SERVER_ID, "TestClientConfigPassThrough");
	}

	@AfterEach
	public void tearDown() throws Exception {
		app.stop();
	}

	private static ClientConfig customizedConfig() {
		var cc = new ClientConfig();
		cc.setNamespace("NS1");
		cc.setInstanceName("t1");
		cc.setUnitName("unitA");
		cc.setNamesrvAddr("127.0.0.1:9876");
		return cc;
	}

	/** Producer：传入 ClientConfig 的路由/身份字段必须落到内部 producer（修复前仅 namesrvAddr 生效→红）。 */
	@Test
	public void producerPassesThroughClientConfig() {
		var p = new Producer(app, "testCcPassThrough", customizedConfig());
		try {
			var inner = p.getProducer();
			assertEquals("NS1", inner.getNamespace(),
					"namespace 丢弃=多租户形态下无报错的静默消息不可达（修复前不透传→null）");
			assertEquals("t1", inner.getInstanceName(),
					"instanceName 丢弃=同 JVM 同组多实例部署 clientId 冲突远离根因（修复前保持默认 DEFAULT）");
			assertEquals("unitA", inner.getUnitName(), "unitName 同属路由/身份字段，须一并透传");
			assertEquals("127.0.0.1:9876", inner.getNamesrvAddr(), "namesrvAddr 既有透传行为不回归");
		} finally {
			p.stop();
			p.UnRegisterZezeTables(app);
		}
	}

	/** Consumer：与 Producer 同一透传契约。 */
	@Test
	public void consumerPassesThroughClientConfig() {
		var c = new Consumer(app, "testCcPassThrough", customizedConfig());
		var inner = c.getConsumer();
		assertEquals("NS1", inner.getNamespace(),
				"namespace 丢弃=多租户形态下无报错的静默消息不可达（修复前不透传→null）");
		assertEquals("t1", inner.getInstanceName(),
				"instanceName 丢弃=同 JVM 同组多实例部署 clientId 冲突远离根因（修复前保持默认 DEFAULT）");
		assertEquals("unitA", inner.getUnitName(), "unitName 同属路由/身份字段，须一并透传");
		assertEquals("127.0.0.1:9876", inner.getNamesrvAddr(), "namesrvAddr 既有透传行为不回归");
	}
}
