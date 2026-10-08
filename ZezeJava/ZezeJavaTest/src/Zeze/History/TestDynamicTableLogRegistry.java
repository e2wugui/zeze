package Zeze.History;

import harness.Extra;
import Zeze.Application;
import Zeze.Config;
import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.Collections.LogMap1;
import Zeze.Transaction.Collections.LogSortedMap1;
import Zeze.Transaction.Collections.Map1Meta;
import Zeze.Transaction.Collections.SortedMap1Meta;
import Zeze.Transaction.Log;
import Zeze.Transaction.TableDynamic;
import demo.Module1.BValue;
import harness.Fast;
import harness.FastServerIds;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.pcollections.Empty;

/**
 * 后启动态开表的回放日志注册回归：registerAllTableLogs 只在启动期
 * atomicOpenDatabase 执行一次，此后经 Application.openDynamicTable 打开的表
 * （enableHttpSession 后启分支、运行期 TableDynamic）其值 bean 的集合日志
 * typeId 永不注册——写侧 History.buildLogChanges 正常编码含该 typeId 的
 * LogBean 树落库，回放端 Log.create 抛 unknown log typeId 成毒记录卡死游标。
 * 修复：openDynamicTable 路径同样执行依赖扫描注册（表登记与日志工厂注册同构）。
 */
@Fast
@Extra
public class TestDynamicTableLogRegistry {

	// 独立serverId+派生url：@Fast类并行时DatabaseMemory静态Map按url分桶互撞规避。
	private static final int SERVER_ID = FastServerIds.TEST_DYNAMIC_TABLE_LOG_REGISTRY;

	private static Application newApp() throws Exception {
		var conf = new Config();
		conf.setServiceManager("disable");
		conf.setTakeoverMode("off");
		conf.setServerId(SERVER_ID);
		conf.setDefaultTableConf(new Config.TableConf());
		var dbConf = new Config.DatabaseConf();
		dbConf.setDatabaseUrl("dynamic_table_log_registry_" + SERVER_ID);
		conf.getDatabaseConfMap().put("", dbConf);
		conf.setHistory("TestDynamicTableLogRegistry");
		return new Application("TestDynamicTableLogRegistry", conf);
	}

	/**
	 * 启动后动态开值bean含集合字段的表，其集合日志 typeId 必须可回放解码。
	 * 选 BValue 的 map&lt;long,long&gt;（_map15）与 sortedmap&lt;int,int&gt;（_sortedmap1）：
	 * 两者均不在纯 Application 启动闭包（AutoKey/Queue/History/DelayRemove/Takeover/
	 * Onz 自动模块）的签名集内，注册缺失必现而非被巧合掩蔽。
	 */
	@Test
	public void testDynamicTableCollectionLogsRegisteredOnOpen() throws Exception {
		var app = newApp();
		try {
			app.start(); // 启动闭包注册完毕；不含 BValue（demo 模块未挂载）

			var mapTypeId = new LogMap1<Long, Long>(null, 0, null, Empty.map(),
					Map1Meta.get(Long.class, Long.class)).getTypeId();
			var sortedMapTypeId = new LogSortedMap1<Integer, Integer>(null, 0, null, Empty.sortedMap(),
					SortedMap1Meta.get(Integer.class, Integer.class)).getTypeId();

			// 运行期动态开表（TableDynamic 构造器内即 openDynamicTable）。
			new TableDynamic<Long, BValue>(app, "TestDynamicTableLogRegistry_t1",
					key -> {
						var bb = ByteBuffer.Allocate();
						bb.WriteLong(key);
						return bb;
					},
					bb -> bb.ReadLong(),
					BValue::new, false, null, Long.class, BValue.class);

			Assertions.assertDoesNotThrow(() -> Log.create(mapTypeId, 0),
					"动态开表后其值bean的map<long,long>日志typeId必须已注册"
							+ "（缺失则回放端unknown log typeId毒记录卡死游标）");
			Assertions.assertDoesNotThrow(() -> Log.create(sortedMapTypeId, 0),
					"动态开表后其值bean的sortedmap<int,int>日志typeId必须已注册");
		} finally {
			app.stop();
		}
	}
}
