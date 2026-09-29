package Zeze.Transaction;

import java.util.concurrent.atomic.AtomicInteger;

import Zeze.Application;
import Zeze.Config;
import Zeze.History.Helper;
import Zeze.Transaction.Procedure;
import demo.Module1.BSimple;
import demo.Module1.BValue;
import demo.Module1.Table1;
import harness.Fast;
import harness.FastServerIds;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * kimi-audit01 T-20回归：DynamicBean作为集合元素时事务日志键跨元素碰撞。
 * LogDynamic的savepoint键与getBean/getTypeId/setBean的查找键均为
 * parent().objectId()+variableId()：集合元素（list[dynamic]等）的parent是同一集合对象，
 * variableId不参与唯一性（用户工厂元素恒为生成varId、decode元素恒为0）——同组所有元素
 * 键完全相同。同事务内：e2.getBean()命中e1的setBean日志（读污染）；e2.setBean复用e1的
 * 日志覆盖value，finalCommit把e2的新bean错写进e1、e2的修改静默丢失。
 * 测试：同事务对两个list43元素先后setBean，断言e2读不受污染、提交后两个元素各得其所
 * （修复前：e2读到e1的新bean；提交后e1被错写、e2丢失）。覆盖用户工厂元素（varId=43）
 * 与重启decode装载元素（varId=0）两种键形态。
 */
@Fast
public class TestDynamicBeanElementLogKey {
	private static final AtomicInteger NextId = new AtomicInteger(FastServerIds.TEST_DYNAMIC_BEAN_ELEMENT_LOG_KEY);

	private Application app;
	private Table1 table1;

	private Application startApp() throws Exception {
		Helper.registerLogs();
		var conf = new Config();
		conf.setServiceManager("disable");
		conf.setServerId(NextId.incrementAndGet());
		conf.setDefaultTableConf(new Config.TableConf());
		var dbConf = new Config.DatabaseConf();
		// 固定url：重启用例需两次Application共享同一Memory库存储（同JVM按url静态分桶）
		dbConf.setDatabaseUrl(FastServerIds.URL_TEST_DYNAMIC_BEAN_ELEMENT_LOG_KEY);
		conf.getDatabaseConfMap().putIfAbsent("", dbConf);
		app = new Application("TestDynamicBeanElementLogKey" + conf.getServerId(), conf);
		app.setSchemas(new demo.Schemas());
		app.addTable(conf.getTableConf("demo_Module1_Table1").getDatabaseName(), new Table1());
		app.start();
		table1 = (Table1)app.getTable("demo_Module1_Table1");
		Assertions.assertNotNull(table1);
		return app;
	}

	private static BSimple simple(int v) {
		var bs = new BSimple();
		bs.setInt_1(v);
		return bs;
	}

	/** 准备：list43=[e1(int_1=1), e2(int_1=2)]。 */
	private void prepareTwoElements(long key) throws Exception {
		Assertions.assertEquals(Procedure.Success, app.newProcedure(() -> {
			var bv = new BValue();
			var list = bv.getList43();
			var e1 = BValue.newDynamicBean_List43();
			e1.setBean(simple(1));
			list.add(e1);
			var e2 = BValue.newDynamicBean_List43();
			e2.setBean(simple(2));
			list.add(e2);
			table1.put(key, bv);
			return Procedure.Success;
		}, "TestDynamicBeanElementLogKey.Prepare").call());
	}

	/** 同事务内对e1、e2先后setBean，捕获e2在e1.setBean之后、e1在e2.setBean之后的读值。 */
	private int[] modifyBothElementsSameTxn(long key) throws Exception {
		final int[] readDuringTxn = {0, 0};
		Assertions.assertEquals(Procedure.Success, app.newProcedure(() -> {
			var list = table1.getOrAdd(key).getList43();
			var e1 = list.get(0);
			var e2 = list.get(1);
			e1.setBean(simple(111));
			readDuringTxn[0] = ((BSimple)e2.getBean()).getInt_1(); // e2读不得被e1的setBean污染
			e2.setBean(simple(222));
			readDuringTxn[1] = ((BSimple)e1.getBean()).getInt_1(); // e1读不得被e2的setBean改写
			return Procedure.Success;
		}, "TestDynamicBeanElementLogKey.Modify").call());
		return readDuringTxn;
	}

	private void verifyBothElements(long key, int expectE1, int expectE2) throws Exception {
		Assertions.assertEquals(Procedure.Success, app.newProcedure(() -> {
			var list = table1.getOrAdd(key).getList43();
			Assertions.assertEquals(expectE1, ((BSimple)list.get(0).getBean()).getInt_1(),
					"e1的setBean结果不得被e2覆盖（错写）");
			Assertions.assertEquals(expectE2, ((BSimple)list.get(1).getBean()).getInt_1(),
					"e2的setBean结果不得静默丢失");
			return Procedure.Success;
		}, "TestDynamicBeanElementLogKey.Verify").call());
	}

	/** 用户工厂元素：两个元素varId同为43（生成常量），键=list.objectId+43碰撞。 */
	@Test
	public void testUserFactoryElementsSameTxnSetBean() throws Exception {
		var app = startApp();
		try {
			prepareTwoElements(8_200_000L);
			var read = modifyBothElementsSameTxn(8_200_000L);
			Assertions.assertEquals(2, read[0], "同事务内e2.getBean()不得读到e1的setBean日志（读污染）");
			Assertions.assertEquals(111, read[1], "同事务内e1.getBean()不得被e2的setBean覆盖");
			verifyBothElements(8_200_000L, 111, 222);
		} finally {
			app.stop();
		}
	}

	/** decode装载元素：重启后元素由工厂new DynamicBean(0,..)创建，varId=0碰撞。 */
	@Test
	public void testDecodedElementsAfterRestartSameTxnSetBean() throws Exception {
		startApp();
		try {
			prepareTwoElements(8_201_000L);
			app.stop();
			startApp(); // 重启走decode装载，元素varId=0
			var read = modifyBothElementsSameTxn(8_201_000L);
			Assertions.assertEquals(2, read[0], "重启decode元素同样不得读污染");
			Assertions.assertEquals(111, read[1], "重启decode元素同样不得被覆盖");
			verifyBothElements(8_201_000L, 111, 222);
		} finally {
			app.stop();
		}
	}
}
