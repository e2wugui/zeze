package Zeze.Transaction;

import org.junit.jupiter.api.Test;

import demo.ModuleGTable.BValue;
import harness.Fast;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 验证生成的 GTable 工厂和动态单元格。 */
@Fast
public class TestGTableDynamicGen {

	@Test
	public void testGeneratedFactoryInstantiates() {
		// 修复前：静态factory初始化即抛（三参+DynamicBean指名拒绝），或编译炸（meta2_X未定义）
		var v = assertDoesNotThrow(BValue::new, "生成factory必须可初始化");
		assertNotNull(v.getGTable3(), "dynamic gtable变量必须可用");
		assertNotNull(v.getGTable4());
	}

	@Test
	public void testGeneratedGTablePutGetDynamic() {
		// 对齐TestFnd833手写宿主的put/get形态：经生成factory构建的GTable2可正常读写DynamicBean
		var v = new BValue();
		var dyn = BValue.newDynamicBean_GTable3();
		dyn.setBean(new demo.ModuleGTable.Bean1());
		v.getGTable3().put(1, 2, dyn);

		var got = v.getGTable3().get(1, 2);
		assertNotNull(got, "生成factory路径put/get正常");
		assertNotNull(got.getBean(), "dynamic内层bean必须恢复");
		assertTrue(got.getBean() instanceof demo.ModuleGTable.Bean1, "实际bean类型必须正确");
	}

}
