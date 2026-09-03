package UnitTest.Zeze.Trans;

import java.util.concurrent.atomic.AtomicInteger;

import Zeze.Application;
import Zeze.Config;
import Zeze.History.Helper;
import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.Changes;
import Zeze.Transaction.ChangeListener;
import Zeze.Transaction.Log;
import Zeze.Transaction.LogDynamic;
import Zeze.Transaction.Procedure;
import Zeze.Util.LongHashMap;
import demo.Module1.BSimple;
import demo.Module1.BValue;
import demo.Module1.Table1;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * FND-T4-1回归：同一事务内"修改dynamic内部bean字段 + setBean换新bean"，
 * finalCommit的Collect树必须携带setBean信息（LogDynamic.value分支）。
 * 修复前Changes.collect对DynamicBean不查事务日志（与Collection分支不对称）：
 * collect迭代顺序为"内部字段日志先于setBean日志"时（LongHashMap桶序，由objectId
 * 分配决定，两种顺序都真实存在），新建value=null的LogDynamic占位，真正的setBean
 * 日志在父LogBean.variables的putIfAbsent中竞争失败被静默丢弃——History/Listener
 * 编码只剩内部bean字段日志，follower应用后typeId与bean替换永久丢失。
 */
@Fast
public class TestDynamicBeanCollect {
	private static final AtomicInteger NextId = new AtomicInteger();

	private Table1 table1;
	private final Collector collector = new Collector();
	private long fieldKey; // savepoint日志key：内部bean.objectId+1
	private long dynKey;   // savepoint日志key：record value.objectId+VAR_dynamic14

	/** 捕获listener视图里dynamic14与bean12(CollOne)的变量日志。 */
	private static final class Collector implements ChangeListener {
		private Log dynamicLog;
		private Log bean12Log;

		void reset() {
			dynamicLog = null;
			bean12Log = null;
		}

		@Override
		public void OnChanged(Object key, Changes.Record r) {
			if (r.getState() == Changes.Record.Edit) {
				var logBean = r.getLogBean();
				var variables = logBean != null ? logBean.getVariables() : null;
				if (variables != null) {
					var d = variables.get(Table1.VAR_dynamic14);
					if (d != null)
						dynamicLog = d;
					var b = variables.get(Table1.VAR_bean12);
					if (b != null)
						bean12Log = b;
				}
			}
		}
	}

	private Application startApp() throws Exception {
		Helper.registerLogs(); // decode侧需要日志工厂（真实follower环境由History注册）
		var conf = new Config();
		conf.setServiceManager("disable");
		conf.setServerId(NextId.incrementAndGet());
		conf.setDefaultTableConf(new Config.TableConf());
		var dbConf = new Config.DatabaseConf();
		dbConf.setDatabaseUrl("test_dynamic_collect_" + conf.getServerId());
		conf.getDatabaseConfMap().putIfAbsent("", dbConf); // Memory库，独立url=独立存储
		var app = new Application("TestDynamicBeanCollect" + conf.getServerId(), conf);
		app.setSchemas(new demo.Schemas());
		// 生成的Module注册依赖完整demo.App（协议工厂等），这里直接注册本测试用到的表；
		// 表注册必须在app.start()（打开数据库）之前完成。
		app.addTable(conf.getTableConf("demo_Module1_Table1").getDatabaseName(), new Table1());
		app.start();
		table1 = (Table1)app.getTable("demo_Module1_Table1");
		Assertions.assertNotNull(table1);
		table1.getChangeListenerMap().addListener(collector);
		return app;
	}

	@Test
	public void testCollectKeepsSetBeanLog() throws Exception {
		var app = startApp();
		try {
			boolean seenFieldLogFirst = false; // 分支甲：内部字段日志先迭代
			boolean seenSetBeanLogFirst = false; // 分支乙：setBean日志先迭代
			for (int i = 0; i < 500 && !(seenFieldLogFirst && seenSetBeanLogFirst); i++) {
				for (int pad = 0; pad < i; pad++)
					new demo.Bean1(); // 扰动objectId分配，使两种桶序都能出现
				modifyInnerThenSetBean(app, 8_000_000L + i);
				// 不变量：任意迭代顺序下，collect出的日志都必须携带setBean信息
				assertFollowerHasSetBean(collector.dynamicLog);
				if (fieldLogIteratesFirst(fieldKey, dynKey))
					seenFieldLogFirst = true;
				else
					seenSetBeanLogFirst = true;
			}
			Assertions.assertTrue(seenFieldLogFirst, "未覆盖到内部字段日志先于setBean日志的迭代顺序");
			Assertions.assertTrue(seenSetBeanLogFirst, "未覆盖到setBean日志先于内部字段日志的迭代顺序");
		} finally {
			app.stop();
		}
	}

	@Test
	public void testCollectDynamicFieldLogOnly() throws Exception {
		var app = startApp();
		try {
			Assertions.assertEquals(Procedure.Success, app.newProcedure(() -> {
				var a = new demo.Bean1();
				a.setV1(111);
				var bv = new BValue();
				bv.setDynamic14(a);
				table1.put(8_100_000L, bv);
				return Procedure.Success;
			}, "TestDynamicBeanCollect.Prepare").call());

			collector.reset();
			Assertions.assertEquals(Procedure.Success, app.newProcedure(() -> {
				var v = table1.getOrAdd(8_100_000L);
				((demo.Bean1)v.getDynamic14().getBean()).setV1(222); // 只改内部字段，无setBean
				v.getBean12().setInt_1(123); // 同时确认CollOne(Collection)路径不受影响
				return Procedure.Success;
			}, "TestDynamicBeanCollect.ModifyFieldOnly").call());

			// 无setBean时仍走logBean分支（增量字段日志），typeId不变
			var bb = ByteBuffer.Allocate();
			Assertions.assertNotNull(collector.dynamicLog);
			collector.dynamicLog.encode(bb);
			var dLog = new LogDynamic(null, 0, null);
			dLog.decode(ByteBuffer.Wrap(bb.getBytes(0, bb.size())));
			var follower = BValue.newDynamicBean_Dynamic14();
			follower.setBean(new demo.Bean1());
			follower.followerApply(dLog);
			Assertions.assertEquals(BValue.DynamicTypeId_Dynamic14_demo_Bean1, follower.getTypeId());
			Assertions.assertEquals(222, ((demo.Bean1)follower.getBean()).getV1());
			// CollOne(Collection)分支：变量日志仍被正常收集
			Assertions.assertNotNull(collector.bean12Log, "CollOne路径的变量日志未被收集");
		} finally {
			app.stop();
		}
	}

	/** 同一事务内：先改dynamic内部bean字段，再setBean换新bean。 */
	private void modifyInnerThenSetBean(Application app, long key) {
		Assertions.assertEquals(Procedure.Success, app.newProcedure(() -> {
			var a = new demo.Bean1();
			a.setV1(111);
			var bv = new BValue();
			bv.setDynamic14(a);
			table1.put(key, bv);
			return Procedure.Success;
		}, "TestDynamicBeanCollect.Prepare").call());

		collector.reset();
		Assertions.assertEquals(Procedure.Success, app.newProcedure(() -> {
			var v = table1.getOrAdd(key);
			var a = (demo.Bean1)v.getDynamic14().getBean();
			a.setV1(222); // savepoint put顺序：先字段日志(key=a.objectId+1)
			var bs = new BSimple();
			bs.setInt_1(999);
			v.setDynamic14(bs); // 后setBean日志(key=v.objectId+VAR_dynamic14)
			fieldKey = a.objectId() + 1;
			dynKey = v.objectId() + Table1.VAR_dynamic14;
			return Procedure.Success;
		}, "TestDynamicBeanCollect.Modify").call());
	}

	/** 编码->解码->followerApply，模拟follower/增量消费者视角。 */
	private static void assertFollowerHasSetBean(Log dynamicLog) {
		Assertions.assertNotNull(dynamicLog, "listener未收到dynamic14的修改日志");
		Assertions.assertInstanceOf(LogDynamic.class, dynamicLog);
		var bb = ByteBuffer.Allocate();
		dynamicLog.encode(bb);
		var dLog = new LogDynamic(null, 0, null);
		dLog.decode(ByteBuffer.Wrap(bb.getBytes(0, bb.size())));
		// follower端当前是旧状态（Bean1），应用日志后必须得到setBean的新状态
		var follower = BValue.newDynamicBean_Dynamic14();
		follower.setBean(new demo.Bean1());
		follower.followerApply(dLog);
		Assertions.assertEquals(BValue.DynamicTypeId_Dynamic14_demo_Module1_BSimple, follower.getTypeId(),
				"必须携带setBean的typeId变更（value分支），不能只编码内部bean字段日志");
		var bs = Assertions.assertInstanceOf(BSimple.class, follower.getBean());
		Assertions.assertEquals(999, bs.getInt_1());
	}

	/** 复现savepoint日志map的迭代顺序：相同两个key、相同put顺序（字段日志先put）。 */
	private static boolean fieldLogIteratesFirst(long fieldKey, long dynKey) {
		var probe = new LongHashMap<Object>();
		probe.put(fieldKey, null);
		probe.put(dynKey, null);
		for (var it = probe.iterator(); it.moveToNext(); )
			return it.key() == fieldKey;
		return false;
	}
}
