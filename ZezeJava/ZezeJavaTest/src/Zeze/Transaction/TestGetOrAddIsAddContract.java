package Zeze.Trans;

import harness.FastServerIds;
import java.util.concurrent.atomic.AtomicInteger;

import Zeze.Application;
import Zeze.Config;
import Zeze.Util.OutObject;
import Zeze.Util.TaskSpec;
import demo.Module1.tMemorySize;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * FND16 txn-02 红绿钉板：TableX.getOrAdd(key, OutObject) 必须双向赋值——已存在路径
 * 赋 false（修复前不赋值保持 null，调用方 if(isAdd.value) 拆箱 NPE 埋雷；对齐
 * RocksDatabase.getOrAddTable 家族惯例）。main 现存 3 处调用均以 new OutObject<>(false)
 * 预初始化防御（恰证 API 语义之坑已被各自绕过），本钉板固化双向契约。
 */
@Fast
public class TestGetOrAddIsAddContract {
	// 独立serverId隔离本地zeze_cache目录与其他@Fast测试（800段TestHotRollbackMemoryTable、
	// 810段TestHot02UpgradeIncompatibleFailFast；810曾与本类撞段，同JVM并发FileMutex互斥失败）。
	private static final AtomicInteger nextServerId = new AtomicInteger(FastServerIds.TEST_GET_OR_ADD_IS_ADD_CONTRACT);

	private static Application newApp() throws Exception {
		var conf = new Config();
		conf.setServiceManager("disable");
		conf.setServerId(nextServerId.getAndIncrement());
		conf.setDefaultTableConf(new Config.TableConf());
		var dbConf = new Config.DatabaseConf();
		dbConf.setDatabaseUrl("fnd16_txn02_" + conf.getServerId());
		conf.getDatabaseConfMap().putIfAbsent("", dbConf);
		return new Application("TestFnd16Txn02@" + conf.getServerId(), conf);
	}

	@Test
	public void testIsAddBidirectional() throws Exception {
		var app = newApp();
		try {
			var table = new tMemorySize();
			app.addTable("", table);
			app.start();
			// 未预初始化的OutObject（暴露陷阱形态；断言用Boolean引用比较，不拆箱）。
			var addFirst = new OutObject<Boolean>();
			var rc = TaskSpec.ofProcedure(app.newProcedure(() -> {
				table.getOrAdd(1L, addFirst); // 新增路径：true
				return 0L;
			}, "TestFnd16Txn02.step1")).call();
			Assertions.assertEquals(0L, rc);
			Assertions.assertEquals(Boolean.TRUE, addFirst.value, "新增路径必须赋true");

			var addLoadPath = new OutObject<Boolean>();
			var addCachePath = new OutObject<Boolean>();
			rc = TaskSpec.ofProcedure(app.newProcedure(() -> {
				// 新事务cr==null → load命中（已提交）：load路径的已存在分支。
				table.getOrAdd(1L, addLoadPath);
				// 同事务cache命中（cr!=null且newestValue非null）。
				table.getOrAdd(1L, addCachePath);
				return 0L;
			}, "TestFnd16Txn02.step2")).call();
			Assertions.assertEquals(0L, rc);
			Assertions.assertEquals(Boolean.FALSE, addLoadPath.value,
					"已存在路径（load命中）必须赋false（修复前保持null）");
			Assertions.assertEquals(Boolean.FALSE, addCachePath.value,
					"已存在路径（cache命中）必须赋false（修复前保持null）");
		} finally {
			app.stop();
		}
	}
}
