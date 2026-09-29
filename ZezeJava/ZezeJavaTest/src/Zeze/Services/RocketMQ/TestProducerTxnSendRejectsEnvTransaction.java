package Zeze.Services.RocketMQ;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
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
 * 环境事务（外层 Zeze 存储过程）内调用 sendMessageWithTransaction 必须 fail-fast：
 * executeLocalTransaction 里的本地事务此时走嵌套 savepoint 合并、不落盘，而
 * rocketmq-client 在其返回 COMMIT_MESSAGE 后立即向 broker 发出 EndTransaction(COMMIT)——
 * 外层事务随后回滚即成"幽灵消息"（本地无变更但消息已投递），外层冲突 redo 重跑则
 * 再发一条新 UNIQ_KEY 的半消息（同一笔本地事务重复投递）。修复前桥对调用环境
 * 零防护零警告，消息静默发出。
 */
@Fast
@ResourceLock("rocketmq.producer.processSlot") // Producer 进程内单例占位是静态状态，与 TestProducerSingleInstancePerProcess 串行
public class TestProducerTxnSendRejectsEnvTransaction {
	// 独立serverId+派生url：@Fast类并行时避免zeze_cache目录与DatabaseMemory同名url互撞（对齐TestCacheDirLock）。
	private static final int SERVER_ID = FastServerIds.TEST_PRODUCER_TXN_SEND_REJECTS_ENV_TRANSACTION;

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
		dbConf.setDatabaseUrl("rocketmq_envtxn_reject_" + SERVER_ID);
		conf.getDatabaseConfMap().putIfAbsent("", dbConf);
		app = new Application("TestProducerTxnSendRejectsEnvTransaction", conf);
		// 表注册须在 start 前（demo App 同序）：start 打开表并建立 cache，后注册的表不会打开。
		// 不 start producer：环境事务防线必须在触网之前拦下调用；负对照止步于 client 未启动的报错。
		producer = new Producer(app, "testEnvTxnReject", new ClientConfig());
		app.start();
	}

	@AfterEach
	public void tearDown() throws Exception {
		producer.stop(); // stop 自带 tSent 反注册（成对收口）
		app.stop();
	}

	/**
	 * 过程 action 内（savepoint 嵌套形态：getCurrent() 非空且已有外层保存点）调用即抛
	 * UnsupportedOperationException，且拦在一切副作用之前：外层过程正常提交、tSent
	 * 不残留预插行。
	 */
	@Test
	public void sendInsideProcedureActionFailsFastBeforeSideEffects() throws Exception {
		cleanTSent();
		var caught = new AtomicReference<UnsupportedOperationException>();
		var msg = new Message("topicEnvTxn", "body".getBytes(StandardCharsets.UTF_8));
		var rc = app.newProcedure(() -> {
			try {
				producer.sendMessageWithTransaction(msg, () -> 0);
			} catch (UnsupportedOperationException e) {
				caught.set(e);
			}
			return 0L; // 捕获后让外层过程正常提交：防线拦在副作用之前，不得污染外层事务
		}, "TestProducerTxnSendRejectsEnvTransaction").call();
		assertEquals(0L, rc, "外层过程应正常提交（修复前：调用静默走嵌套 savepoint 路径，本地事务不落盘而 COMMIT 已发给 broker）");
		var ex = caught.get();
		assertNotNull(ex, "环境事务内调用必须抛 UnsupportedOperationException");
		// 报错须自带去处：在事务外发送，或改用非事务消息
		assertTrue(ex.getMessage().contains("事务外"), "报错须指明出路（事务外发送/改用非事务消息），实际: " + ex.getMessage());
		// 防线必须先于一切副作用：tSent 不得残留预插行
		assertTrue(walkAllTSentKeys().isEmpty(), "拦下时不得已写入 tSent（预插行/uniqKey 行）");
	}

	/** 负对照：事务外线程不被环境事务防线拦截——调用越过桥入口，在 client 侧因 producer 未 start 而报错。 */
	@Test
	public void sendOutsideTransactionNotRejectedByEnvGuard() throws Exception {
		cleanTSent();
		var msg = new Message("topicEnvTxn", "body".getBytes(StandardCharsets.UTF_8));
		var ex = assertThrows(Exception.class, () -> producer.sendMessageWithTransaction(msg, () -> 0),
				"事务外调用应越过环境事务防线（producer 未 start，止步于 client 侧报错）");
		assertNotEquals(UnsupportedOperationException.class, ex.getClass(),
				"环境事务防线只拦事务内调用，事务外不应被拦，实际: " + ex);
		// 越过防线即已执行 tSent 预插（真实落盘过程），清理留给下一个用例的 cleanTSent。
	}

	private void cleanTSent() throws Exception {
		var keys = walkAllTSentKeys();
		if (!keys.isEmpty())
			app.newProcedure(() -> {
				for (var key : keys)
					producer._tSent.remove(key);
				return 0L;
			}, "TestProducerTxnSendRejectsEnvTransaction.clean").call();
	}

	// DatabaseMemory 表无 storage（walk 不可用），用 walkMemory 遍历缓存（事务外调用）。
	private ArrayList<String> walkAllTSentKeys() throws Exception {
		var keys = new ArrayList<String>();
		producer._tSent.walkMemory((key, value) -> {
			keys.add(key);
			return true; // 继续
		});
		return keys;
	}
}
