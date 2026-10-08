package Zeze.Services.RocketMQ;

import harness.Extra;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import Zeze.Application;
import Zeze.Config;
import harness.Fast;
import harness.FastServerIds;
import org.apache.rocketmq.client.ClientConfig;
import org.apache.rocketmq.common.message.Message;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceLock;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 环境事务（外层 Zeze 存储过程）内调用 sendMessage（普通消息）必须 fail-fast：
 * 普通消息同步立即投递，与所在事务的提交/回滚完全脱钩——外层过程返回非0/抛异常
 * 回滚即"幽灵消息"（本地无变更但消息已投递、不可回收），外层锁冲突 redo 重跑
 * （Transaction.perform 最多256轮）每轮再发（同一笔本地事务重复投递）。与
 * sendMessageWithTransaction 的既有防线（FND19）同判据同危害，但该入口此前零
 * 防护，且 WithTransaction 的报错文案还指路"改用 sendMessage 发非事务消息"，
 * 把误用引向无防线入口。修复后同款 fail-fast：环境事务内两入口均拒绝。
 * 事务感知发送的惯用法：TaskSpec.ofAction(() -> producer.send(msg)).run()
 * 注册到外层事务 whileCommit（提交后执行、回滚跳过）。
 */
@Fast
@ResourceLock("rocketmq.producer.processSlot") // Producer 进程内单例占位是静态状态，与其它 Producer 测试串行
@Extra
public class TestProducerPlainSendRejectsEnvTransaction {
	// 独立serverId+派生url：@Fast类并行时避免zeze_cache目录与DatabaseMemory同名url互撞。
	private static final int SERVER_ID = FastServerIds.TEST_PRODUCER_PLAIN_SEND_REJECTS_ENV_TRANSACTION;

	private Application app;
	private Producer producer;

	@BeforeEach
	public void setUp() throws Exception {
		var conf = new Config();
		conf.setServiceManager("disable");
		conf.setServerId(SERVER_ID);
		conf.setDefaultTableConf(new Config.TableConf()); // 裸Config不会补默认值
		var dbConf = new Config.DatabaseConf();
		dbConf.setDatabaseType(Config.DbType.Memory);
		dbConf.setDatabaseUrl("rocketmq_plain_envtxn_" + SERVER_ID);
		conf.getDatabaseConfMap().putIfAbsent("", dbConf);
		app = new Application("TestProducerPlainSendRejectsEnvTransaction", conf);
		// 不 start producer：防线必须在触网之前拦下调用；负对照止步于 client 未启动的报错。
		producer = new Producer(app, "testPlainEnvTxnReject", new ClientConfig());
		app.start();
	}

	@AfterEach
	public void tearDown() throws Exception {
		producer.stop(); // stop 自带 tSent 反注册（成对收口）
		app.stop();
	}

	/**
	 * 过程 action 内（getCurrent() 非空）调用即抛 UnsupportedOperationException，
	 * 拦在触网之前：外层过程正常提交。修复前消息放行进 client（producer 未 start
	 * 时以 MQClientException 面目越界，真实部署则直接投递成幽灵消息/redo重复投递）。
	 */
	@Test
	public void sendInsideProcedureActionFailsFastBeforeNetwork() throws Exception {
		var caught = new AtomicReference<UnsupportedOperationException>();
		var msg = new Message("topicPlainEnvTxn", "body".getBytes(StandardCharsets.UTF_8));
		var rc = app.newProcedure(() -> {
			try {
				producer.sendMessage(msg);
			} catch (UnsupportedOperationException e) {
				caught.set(e);
			}
			return 0L; // 捕获后让外层过程正常提交：防线不得污染外层事务
		}, "TestProducerPlainSendRejectsEnvTransaction").call();
		assertEquals(0L, rc, "外层过程应正常提交（修复前：调用越界进 client 抛错污染外层过程）");
		var ex = caught.get();
		assertNotNull(ex, "环境事务内调用 sendMessage 必须抛 UnsupportedOperationException");
		// 报错须自带去处：在事务外发送
		assertTrue(ex.getMessage().contains("事务外"), "报错须指明出路（事务外发送），实际: " + ex.getMessage());
	}

	/** 负对照：事务外线程不被环境事务防线拦截——调用越过桥入口，在 client 侧因 producer 未 start 而报错。 */
	@Test
	public void sendOutsideTransactionNotRejectedByEnvGuard() {
		var msg = new Message("topicPlainEnvTxn", "body".getBytes(StandardCharsets.UTF_8));
		var ex = assertThrows(Exception.class, () -> producer.sendMessage(msg),
				"事务外调用应越过环境事务防线（producer 未 start，止步于 client 侧报错）");
		assertNotEquals(UnsupportedOperationException.class, ex.getClass(),
				"环境事务防线只拦事务内调用，事务外不应被拦，实际: " + ex);
	}
}
