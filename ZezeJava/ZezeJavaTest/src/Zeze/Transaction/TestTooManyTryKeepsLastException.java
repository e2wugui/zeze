package Zeze.Trans;

import java.util.concurrent.atomic.AtomicReference;

import Zeze.Application;
import Zeze.Config;
import Zeze.Transaction.GoBackZeze;
import Zeze.Transaction.Procedure;
import Zeze.Transaction.Transaction;
import harness.Fast;
import harness.FastServerIds;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Transaction.perform 重试耗尽（TooManyTry）只返回错误码，驱动重做的原始异常
 * （GoBackZeze 等）与过程日志不随结果带出，事后只能翻日志且限频日志可能丢失末轮信息。
 *
 * 修复：perform 暂存最近一个异常轮的原始异常，终局 error 全栈记录，
 * 并经 Transaction.getLastRoundException() 可读。
 *
 * 构造：action 每轮直接 throwRedo（GoBackZeze 异常形态走 catch 的 case Redo 重试，
 * 持锁不释放不 sleep，256 轮快速耗尽），断言 TooManyTry 后事务上仍能读到末轮异常。
 */
@Fast
public class TestTooManyTryKeepsLastException {
	// 独立serverId隔离本地zeze_cache目录与其他@Fast测试（16375，growth=1；上限16383）。
	private static final int SERVER_ID = FastServerIds.TEST_TOO_MANY_TRY_LAST_EXCEPTION;

	private static Application newApp() throws Exception {
		var conf = new Config();
		conf.setServiceManager("disable");
		conf.setServerId(SERVER_ID);
		conf.setDefaultTableConf(new Config.TableConf());
		var dbConf = new Config.DatabaseConf();
		dbConf.setDatabaseUrl("too_many_try_last_ex_" + conf.getServerId());
		conf.getDatabaseConfMap().putIfAbsent("", dbConf);
		return new Application("TestTooManyTryKeepsLastException", conf);
	}

	@Test
	public void testTooManyTryRetainsLastRoundException() throws Exception {
		var app = newApp();
		app.start();
		try {
			var txnRef = new AtomicReference<Transaction>();
			var result = app.newProcedure(() -> {
				txnRef.set(Transaction.getCurrent());
				Transaction.getCurrent().throwRedo(0, "Redo: constant conflict");
				return Procedure.Success; // unreachable
			}, "alwaysRedo").call();

			Assertions.assertEquals(Procedure.TooManyTry, result);
			var last = txnRef.get().getLastRoundException();
			Assertions.assertNotNull(last, "重试耗尽后应能读到最后一个异常轮的原始异常");
			Assertions.assertInstanceOf(GoBackZeze.class, last);
			Assertions.assertTrue(last.getMessage().contains("constant conflict"),
					() -> "末轮异常应保留原始消息: " + last.getMessage());
		} finally {
			app.stop();
		}
	}

	@Test
	public void testLastRoundExceptionClearedOnNextPerform() throws Exception {
		var app = newApp();
		app.start();
		try {
			var txnRef = new AtomicReference<Transaction>();
			Assertions.assertEquals(Procedure.TooManyTry, app.newProcedure(() -> {
				txnRef.set(Transaction.getCurrent());
				Transaction.getCurrent().throwRedo(0, "Redo: constant conflict");
				return Procedure.Success;
			}, "alwaysRedo").call());

			// 同线程下一笔正常事务：诊断残留必须清零，不得串笔。
			Assertions.assertEquals(Procedure.Success, app.newProcedure(() -> Procedure.Success, "clean").call());
			Assertions.assertNull(txnRef.get().getLastRoundException(),
					"新事务 perform 开始时 lastRoundException 应清零");
		} finally {
			app.stop();
		}
	}
}
