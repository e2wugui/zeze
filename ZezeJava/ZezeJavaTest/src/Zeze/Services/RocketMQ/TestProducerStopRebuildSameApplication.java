package Zeze.Services.RocketMQ;

import harness.Extra;
import Zeze.Application;
import Zeze.Config;
import harness.Fast;
import harness.FastServerIds;
import org.apache.rocketmq.client.ClientConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * stop() 与构造器的 tSent 注册必须成对收口：构造器把 tSent 登记进 Application，
 * stop() 停机后反注册（removeTable 并关表）——同一 Application 上"stop 旧实例 →
 * new 新实例"（broker 迁移重建、配置热替换后重建、测试夹具复用 app）不得在构造器
 * 的表注册处抛 duplicate table。修复前 stop 只停 producer/清理定时器/排空回查
 * 线程池，tSent 永远留在 app 的表注册表里，重建必抛
 * IllegalStateException("duplicate table id=1695098005")，该 producerGroup 的事务
 * 消息发送能力在调用方补手工 UnRegisterZezeTables 前不可恢复——配对动作完全依赖
 * 调用方"知道"要手工补，且部署契约 javadoc 未载明。
 */
@Fast
@ResourceLock("rocketmq.producer.processSlot") // 构造 Producer 占用进程级静态计数，与其它 Producer 测试串行
@Extra
public class TestProducerStopRebuildSameApplication {
	private static final int SERVER_ID = FastServerIds.TEST_PRODUCER_STOP_REBUILD_SAME_APPLICATION;

	private Application app;

	private static Application newApp(int serverId, String name) throws Exception {
		var conf = new Config();
		conf.setServiceManager("disable");
		conf.setServerId(serverId);
		conf.setDefaultTableConf(new Config.TableConf()); // 裸Config不会补默认值
		var dbConf = new Config.DatabaseConf();
		dbConf.setDatabaseType(Config.DbType.Memory);
		dbConf.setDatabaseUrl("rocketmq_stop_rebuild_" + serverId);
		conf.getDatabaseConfMap().putIfAbsent("", dbConf);
		return new Application(name, conf);
	}

	@BeforeEach
	public void setUp() throws Exception {
		app = newApp(SERVER_ID, "TestProducerStopRebuildSameApplication");
	}

	@AfterEach
	public void tearDown() throws Exception {
		app.stop();
	}

	/** stop 后同 Application 重建：构造器重新注册 tSent 必须成功（修复前必撞
	 * duplicate table——第二个实例的 RegisterZezeTables 在 addTable 的表 id 查重处抛
	 * IllegalStateException，重建始终失败直到调用方手工补反注册）。 */
	@Test
	public void rebuildAfterStopOnSameApplicationDoesNotThrow() throws Exception {
		var p1 = new Producer(app, "testStopRebuild", new ClientConfig());
		p1.stop();
		var p2 = assertDoesNotThrow(() -> new Producer(app, "testStopRebuild", new ClientConfig()),
				"stop 必须反注册 tSent：同 app 重建不得撞 duplicate table（修复前必抛）");
		p2.stop();
	}
}
