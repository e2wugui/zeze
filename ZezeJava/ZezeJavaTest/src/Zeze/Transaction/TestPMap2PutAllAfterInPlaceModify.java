package Zeze.Transaction;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import Zeze.Application;
import Zeze.Config;
import Zeze.Transaction.Collections.LogMap2;
import Zeze.Transaction.Collections.LogSortedMap2;
import demo.Module1.BSimple;
import demo.Module1.tflush;
import harness.Fast;
import harness.FastServerIds;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * 2系Map（值为可变Bean）的putAll按身份记账：同一事务先原位修改值bean
 * （changed按身份关联当前值），再putAll一个与修改后内容equals相等的
 * 新实例——继承自1系的值等过滤会跳过replaced记账，changed又因旧bean
 * 已非当前值被身份过滤丢弃，该键增量整体丢失，follower/History回放停在
 * 旧值（leader内存值正确，分歧只在复制侧）。1系（不可变值）equals过滤
 * 语义不变，由TestSetMapPartialChangePhantomDelta覆盖。
 */
@Fast
public class TestPMap2PutAllAfterInPlaceModify {

	// 动态发号（桌预留100-199段）：本类每轮全量运行消耗2个号。


	private static Application newApp(int serverId) throws Exception {
		var conf = new Config();
		conf.setServiceManager("disable");
		conf.setServerId(serverId);
		conf.setDefaultTableConf(new Config.TableConf());
		var dbConf = new Config.DatabaseConf();
		dbConf.setDatabaseUrl("pmap2_putall_inplace_" + serverId);
		conf.getDatabaseConfMap().putIfAbsent("", dbConf);
		var app = new Application("TestPMap2PutAllInPlace" + serverId, conf);
		app.setSchemas(new demo.Schemas());
		app.addTable(conf.getTableConf("demo_Module1_tflush").getDatabaseName(), new tflush());
		app.start();
		return app;
	}

	private static BSimple bean(int int1) {
		var bs = new BSimple();
		bs.setInt_1(int1);
		return bs;
	}

	@Test
	public void inPlaceModifyThenPutAllEqualInstanceKeepsDelta() throws Exception {
		var app = newApp(FastServerIds.takeoverPoolNext());
		var table = (tflush)app.getTable("demo_Module1_tflush");
		try {
			Assertions.assertEquals(Procedure.Success, app.newProcedure(() -> {
				table.getOrAdd(1L).getMap41().put(1L, bean(5));
				table.getOrAdd(1L).getSortedmap2().put(1, bean(5));
				return Procedure.Success;
			}, "seed").call());

			Assertions.assertEquals(Procedure.Success, app.newProcedure(() -> {
				// PMap2路径
				var map = table.getOrAdd(1L).getMap41();
				var logKey = map.parent().objectId() + map.variableId();
				Assertions.assertNull(Transaction.getCurrent().getLog(logKey), "操作前不得有日志");
				map.get(1L).setInt_1(42); // 原位修改：changed按身份关联当前值
				var fresh = bean(42);      // 与修改后内容equals相等的新实例
				map.putAll(Map.of(1L, fresh));

				var log = (LogMap2<Long, BSimple>)Transaction.getCurrent().getLog(logKey);
				Assertions.assertTrue(log.getReplaced().containsKey(1L),
						"PMap2：原位修改后putAll等值新实例，该键必须记入replaced"
								+ "（值等过滤与changed身份过滤叠加会整体丢增量）");
				Assertions.assertSame(fresh, log.getReplaced().get(1L), "记账携带新实例完整值");
				Assertions.assertFalse(log.getRemoved().contains(1L));

				// PSortedMap2同构路径
				var smap = table.getOrAdd(1L).getSortedmap2();
				var sLogKey = smap.parent().objectId() + smap.variableId();
				Assertions.assertNull(Transaction.getCurrent().getLog(sLogKey), "操作前不得有日志");
				smap.get(1).setInt_1(42);
				var sFresh = bean(42);
				smap.putAll(Map.of(1, sFresh));

				var sLog = (LogSortedMap2<Integer, BSimple>)Transaction.getCurrent().getLog(sLogKey);
				Assertions.assertTrue(sLog.getReplaced().containsKey(1),
						"PSortedMap2：原位修改后putAll等值新实例，该键必须记入replaced");
				Assertions.assertSame(sFresh, sLog.getReplaced().get(1));
				return Procedure.Success;
			}, "delta").call());

			Assertions.assertEquals(Procedure.Success, app.newProcedure(() -> {
				Assertions.assertEquals(42, table.getOrAdd(1L).getMap41().get(1L).getInt_1());
				Assertions.assertEquals(42, table.getOrAdd(1L).getSortedmap2().get(1).getInt_1());
				return Procedure.Success;
			}, "verify").call());
		} finally {
			app.stop();
		}
	}
}
