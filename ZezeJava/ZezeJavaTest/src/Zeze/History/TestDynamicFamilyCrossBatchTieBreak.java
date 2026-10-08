package Zeze.History;

import harness.Extra;
import java.math.BigDecimal;
import Zeze.Builtin.HotDistribute.BVariable;
import Zeze.Transaction.Collections.Map2Meta;
import Zeze.Transaction.DynamicBean;
import Zeze.Transaction.EmptyBean;
import Zeze.Transaction.GTable.GTable2;
import Zeze.Transaction.Log;
import harness.Fast;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.ResourceAccessMode;
import org.junit.jupiter.api.parallel.ResourceLock;

import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * FND33 history-01 回归：dynamic 家族同 typeId 冲突的字典序决胜此前只在单次
 * applyRegistrations 的 DependsResult 容器内生效——增量开表
 * （openDynamicTable→registerTableLogs，单表全新 DependsResult）批次与启动期
 * 已注册的同 typeId 家族冲突时走 Log.register 的 putIfAbsent 先到先得：新家族
 * 工厂被静默丢弃（dynamic 家族 getTypeName 相同，仅 debug 留痕），后开表的
 * dynamic 集合日志回放仍用旧家族工厂解码，注册终态由注册时机决定、跨重启翻转。
 * 修复：登记改为进程级累积状态，跨批次同样按家族来源名字典序决胜——新到家族
 * 更小则定向替换注册槽位（Log.replaceRegistered 条件替换），更大则 warn 留痕。
 * 键隔离：map 用 binary/decimal 键、gtable 用 (binary,long)——仓库 schema 无
 * dynamic 变量，这些 typeId 桶（含 gtable 的内层 (Long,Dynamic) 桶）仅本测试
 * 触达，不污染真实注册。
 */
@Fast
@ResourceLock(value = "history-helper-logger", mode = ResourceAccessMode.READ_WRITE) // 必须READ_WRITE：默认READ对READ不互斥，本类按设计触发cross-batch warn，与DropLogged/GcC02的appender断言必须真互斥
@Extra
public class TestDynamicFamilyCrossBatchTieBreak {

	/** 字典序较大的家族（批1占位者）。 */
	public static final class HostZulu {
		public static DynamicBean newDynamicBean_Items() {
			return new DynamicBean(2, b -> 2L, id -> new EmptyBean());
		}
	}

	/** 字典序较小的家族（批2挑战者，跨批必须翻转接管）。 */
	public static final class HostAlpha {
		public static DynamicBean newDynamicBean_Managers() {
			return new DynamicBean(1, b -> 1L, id -> new EmptyBean());
		}
	}

	private static BVariable.Data mapVar(String name, String keyType) {
		var v = new BVariable.Data();
		v.setId(1);
		v.setName(name);
		v.setType("map");
		v.setKey(keyType);
		v.setValue("dynamic");
		return v;
	}

	private static BVariable.Data gtableVar(String name) {
		var v = new BVariable.Data();
		v.setId(1);
		v.setName(name);
		v.setType("gtable");
		v.setKey("binary,long");
		v.setValue("dynamic");
		return v;
	}

	// 批1（模拟启动期）先注册大 where 家族，批2（模拟增量开表 openDynamicTable）到达
	// 更小 where 家族：必须接管注册槽位。修复前 putIfAbsent 先到先得——新家族工厂被
	// 静默丢弃（红：after==before），后开表的 dynamic 集合日志回放继续用旧家族工厂解码。
	@Test
	public void testCrossBatchSmallerFamilyTakesOverLogSlot() throws Exception {
		var typeId = Map2Meta.createDynamic(Zeze.Net.Binary.class, b -> 0L, t -> null).logTypeId;
		assertNull(Log.getRegistered(typeId), "测试隔离前提：binary键dynamic map桶此前未注册");

		var batch1 = new Helper.DependsResult();
		Helper.dependsMap(HostZulu.class, mapVar("items", "binary"), "binary", "dynamic", batch1);
		Helper.applyRegistrations(batch1);
		var before = Log.getRegistered(typeId);
		assertNotNull(before, "批1注册后槽位必须被占");

		var batch2 = new Helper.DependsResult();
		Helper.dependsMap(HostAlpha.class, mapVar("managers", "binary"), "binary", "dynamic", batch2);
		Helper.applyRegistrations(batch2);

		assertNotSame(before, Log.getRegistered(typeId),
				"跨批次更小where家族必须接管注册槽位（先到先得会静默丢弃新家族工厂）");
	}

	// 批1先注册小 where 家族，批2到达更大 where 家族：落败但不再静默——槽位不得被扰动
	//（与启动期批内决胜同构：胜者恒为 argmin(where)，与到达批次顺序无关）。
	@Test
	public void testCrossBatchLargerFamilyLosesWithoutTakingSlot() throws Exception {
		var typeId = Map2Meta.createDynamic(BigDecimal.class, b -> 0L, t -> null).logTypeId;
		assertNull(Log.getRegistered(typeId), "测试隔离前提：decimal键dynamic map桶此前未注册");

		var batch1 = new Helper.DependsResult();
		Helper.dependsMap(HostAlpha.class, mapVar("managers", "decimal"), "decimal", "dynamic", batch1);
		Helper.applyRegistrations(batch1);
		var before = Log.getRegistered(typeId);
		assertNotNull(before);

		var batch2 = new Helper.DependsResult();
		Helper.dependsMap(HostZulu.class, mapVar("items", "decimal"), "decimal", "dynamic", batch2);
		Helper.applyRegistrations(batch2);

		assertSame(before, Log.getRegistered(typeId),
				"跨批次更大where家族必须落败，不得扰动既有胜者的注册槽位");
	}

	// gtable 外层 pmapMeta（hist-01外层收口的跨批次面）：同(row,col)的dynamic gtable
	// 外层 typeId 相同而 valueCtor 各异，跨批到达的更小家族同样必须接管外层槽位。
	// 列取 long：内层桶 (Long,Dynamic) 与本类 map 用例的 binary/decimal 桶互不重叠
	//（dependsGTable 对 dynamic 值同时登记内层家族）。
	@Test
	public void testCrossBatchOuterGTableMetaTakesOverLogSlot() throws Exception {
		var typeId = GTable2.getFactory(Zeze.Net.Binary.class, Long.class,
				b -> 0L, t -> null).getPmapMeta().logTypeId;
		assertNull(Log.getRegistered(typeId), "测试隔离前提：(binary,long)外层桶此前未注册");

		var batch1 = new Helper.DependsResult();
		Helper.dependsGTable(HostZulu.class, gtableVar("items"), "binary", "long", "dynamic", batch1);
		Helper.applyRegistrations(batch1);
		var before = Log.getRegistered(typeId);
		assertNotNull(before);

		var batch2 = new Helper.DependsResult();
		Helper.dependsGTable(HostAlpha.class, gtableVar("managers"), "binary", "long", "dynamic", batch2);
		Helper.applyRegistrations(batch2);

		assertNotSame(before, Log.getRegistered(typeId),
				"外层pmapMeta跨批次更小where家族必须接管注册槽位（外层行物化工厂不得由注册时机决定）");
	}
}
