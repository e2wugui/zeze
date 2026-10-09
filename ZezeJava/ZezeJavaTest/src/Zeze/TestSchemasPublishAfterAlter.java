package Zeze;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.DatabaseMemory;
import Zeze.Transaction.DatabaseRelationalMapping;
import harness.Fast;
import harness.FastServerIds;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Schemas元数据的发布必须晚于物理DDL：兼容检查阶段保存新版Schemas会把
 * 目标状态提前当成"已完成"——其后开表或ALTER临时失败、进程退出，重启读取
 * 的previous已是新版结构，newRelationalTable差分为空，升级被静默跳过
 * （物理表仍旧结构，代码按新列读写持续失败；多表时只有部分表实际升级而
 * 元数据覆盖全部）。检查（schemasCompatible）只判兼容不落盘；DDL全部
 * 成功后由publishSchemas发布，DDL成功、发布前崩溃的重启由tryAlter的
 * 物理列幂等过滤兜底重放。
 */
@Fast
public class TestSchemasPublishAfterAlter {

	private static final AtomicInteger NextId = new AtomicInteger(FastServerIds.TAKEOVER_POOL);

	private static Schemas schemas(int variableCount) {
		var s = new Schemas();
		var bean = new Schemas.Bean("PublishDeferredBean", false);
		for (int i = 1; i <= variableCount; i++) {
			var v = new Schemas.Variable();
			v.id = i;
			v.name = "v" + i;
			v.typeName = "long";
			bean.addVariable(v);
		}
		s.addBean(bean);
		s.addTable(new Schemas.Table("PublishDeferredTable", "long", "PublishDeferredBean"));
		return s;
	}

	private static DatabaseRelationalMapping mapping() {
		return new DatabaseRelationalMapping() {
			@Override
			public Zeze.Transaction.Database.Table openRelationalTable(String name) {
				throw new UnsupportedOperationException();
			}

			@Override
			public Map<String, String> getSqlTypeMap() {
				return Map.of("long", "bigint");
			}
		};
	}

	@Test
	public void compatibleCheckDoesNotPublishBeforeAlter() throws Exception {
		var conf = new Config();
		conf.setNoDatabase(true);
		conf.setServiceManager("disable");
		conf.setDefaultTableConf(new Config.TableConf());
		var app = new Application("TestSchemasPublishAfterAlter", conf);
		var dbConf = new Config.DatabaseConf();
		dbConf.setDatabaseUrl("schemas_publish_deferred_" + NextId.getAndIncrement());
		var db = new DatabaseMemory(null, dbConf);
		app.getDatabases().put("", db);

		// 种入旧版元数据（B: v1）
		var key = ByteBuffer.Allocate();
		key.WriteString("zeze.Schemas.V4." + conf.getServerId());
		var previousBytes = ByteBuffer.Allocate();
		schemas(1).encode(previousBytes);
		db.getDirectOperates().saveDataWithSameVersion(key, previousBytes, 0);

		// 当前版本增加一列（B: v1+v2），执行兼容检查
		app.setSchemas(schemas(2));
		app.schemasCompatible();

		// 崩溃窗口断言：检查阶段（DDL前）元数据不得被发布——
		// 模拟"ALTER未执行即中断后重启"，重算差分仍应有待加列。
		var pending = Schemas.newRelationalTable(mapping(),
				app.getSchemas().tables.get("PublishDeferredTable"),
				app.getSchemasPrevious().tables.get("PublishDeferredTable"));
		assertEquals(1, pending.add.size(),
				"兼容检查不得发布元数据：发布后失败重启拿到空差分，升级被静默跳过");

		// DDL全部成功后发布：重启（从库重读元数据）差分清零
		app.publishSchemas();
		var saved = db.getDirectOperates().getDataWithVersion(key);
		var restartPrevious = new Schemas();
		restartPrevious.decode(saved.data);
		restartPrevious.compile();
		var afterPublish = Schemas.newRelationalTable(mapping(),
				app.getSchemas().tables.get("PublishDeferredTable"),
				restartPrevious.tables.get("PublishDeferredTable"));
		assertEquals(0, afterPublish.add.size(), "发布后重启差分应为空");
	}
}
