package UnitTest.Zeze.Transaction;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import Zeze.Builtin.HotDistribute.BVariable;
import Zeze.History.Helper;
import Zeze.Transaction.DynamicBean;
import demo.ModuleGTable.BValue;
import harness.Fast;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * FND8-33 A2钉板：Gen生成器对gtable+dynamic值的发射修复（生成产物端到端）。
 * 修复前gtable dynamic变量三处坏（solution.xml的BValue.gTable3/gTable4实证）：
 * ①factory走三参getFactory(row,col,DynamicBean.class)——A1的指名拒绝使宿主bean
 * 静态块抛IllegalArgumentException；②Construct发射new GTable2<>(meta2_X)——字段
 * 从未发射且GTable2无该构造器（编译炸）；③newDynamicBean_Xxx三件套不发射——
 * History.dependsGTable的newDynamicFamily反射NoSuchMethod。
 * 修复：dispatch补TypeGTable×TypeDynamic分支→GenDynamicSpecialMethod发四参
 * getFactory(row,col,Bean::getSpecialTypeIdFromBean_id,Bean::createBeanFromSpecialTypeId_id)；
 * Construct统一走factory构造器；三件套由通用段自动补齐。
 * 手写宿主路径的钉板见TestFnd833GTable2Dynamic；本类只钉生成产物。
 */
@Fast
public class TestGTableDynamicGen {

	@Test
	public void testGeneratedFactoryInstantiates() {
		// 修复前：静态factory初始化即抛（三参+DynamicBean指名拒绝），或编译炸（meta2_X未定义）
		var v = assertDoesNotThrow(BValue::new, "生成factory（四参getFactory）必须可初始化");
		assertNotNull(v.getGTable3(), "dynamic gtable变量必须可用");
		assertNotNull(v.getGTable4());
	}

	@Test
	public void testGeneratedGTablePutGetDynamic() {
		// 对齐TestFnd833手写宿主的put/get形态：经生成factory构建的GTable2可正常读写DynamicBean
		var v = new BValue();
		var dyn = new DynamicBean(3, BValue::getSpecialTypeIdFromBean_3, BValue::createBeanFromSpecialTypeId_3);
		dyn.setBean(new demo.ModuleGTable.Bean1());
		v.getGTable3().put(1, 2, dyn);

		var got = v.getGTable3().get(1, 2);
		assertNotNull(got, "生成factory路径put/get正常");
		assertNotNull(got.getBean(), "dynamic内层bean必须恢复");
		assertTrue(got.getBean() instanceof demo.ModuleGTable.Bean1, "实际bean类型必须正确");
	}

	@Test
	public void testDependsGTableOnGeneratedBean() {
		// History端到端：newDynamicFamily反射newDynamicBean_GTable3()（生成的三件套）
		// ——修复前该静态方法不存在，反射NoSuchMethod被包成RuntimeException
		var v = new BVariable.Data();
		v.setId(3);
		v.setName("gTable3");
		v.setType("gtable");
		v.setKey("int,int");
		v.setValue("dynamic");

		var result = new Helper.DependsResult();
		assertDoesNotThrow(() -> Helper.dependsGTable(BValue.class, v, "int", "int", "dynamic", result),
				"dependsGTable必须经生成三件套解析dynamic家族");
		assertTrue(result.map2Metas.size() >= 1, "外层pmapMeta必须收集（registerAllTableLogs注册原料）");
		assertEquals(1, result.map2Dynamic.size(), "dynamic家族按(keyClass,DynamicBean)登记");

		// 外层meta必须走coll-01根治路径：name含GTable2家族前缀与完整身份（int,int,DynamicBean），
		// 与三参版真实bean身份分流
		var outer = result.map2Metas.iterator().next();
		Assertions.assertTrue(outer.name.startsWith("GTable2:"),
				"外层name必须用GTable2专用家族头，实际=" + outer.name);
		Assertions.assertTrue(outer.name.endsWith("int, " + "Zeze.Transaction.DynamicBean"),
				"dynamic值身份固定DynamicBean（对齐Meta2.dynamic先例），实际=" + outer.name);
	}
}
