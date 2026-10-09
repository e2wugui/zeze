package Zeze;

import Zeze.Application;
import Zeze.Config;
import Zeze.Schemas;
import harness.Fast;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 兼容检查的去重状态只能属于一次比较：失败的检查之后，对同一 current
 重新检查（调用方重试、热更失败重试、CAS 重读拿到不同 previous）必须重新执行判定。
 * 修复前 Bean 实例上的检查标记跨比较保留，失败也置位，第二次检查直接按
 * "已检查通过"放行，string→long 等禁止变更被静默接受。
 */
@Fast
public class TestSchemasCompatibleCheckRetryKeepsRejecting {

	@Test
	public void failedCompatibleCheckIsRejectedAgainOnRetry() throws Exception {
		var conf = new Config();
		conf.setNoDatabase(true);
		conf.setServiceManager("disable");
		var app = new Application("TestSchemasCompatibleCheckRetry", conf);

		var current = schemas("long");
		var ex1 = assertThrows(IllegalStateException.class, () -> current.checkCompatible(schemas("string"), app));
		assertTrue(ex1.getMessage().contains("Incompatible"), "第一次必须因不兼容被拒绝: " + ex1.getMessage());

		// 修复前：current 的 Bean 实例带着失败置位的检查标记，第二次直接放行（不抛异常）。
		var ex2 = assertThrows(IllegalStateException.class, () -> current.checkCompatible(schemas("string"), app));
		assertTrue(ex2.getMessage().contains("Incompatible"), "重试必须重新执行判定并再次拒绝: " + ex2.getMessage());
	}

	private static Schemas schemas(String beanVarType) {
		var s = new Schemas();
		var bean = new Schemas.Bean("CompatRetryBean", false);
		var variable = new Schemas.Variable();
		variable.id = 1;
		variable.name = "x";
		variable.typeName = beanVarType;
		bean.addVariable(variable);
		s.addBean(bean);
		s.addTable(new Schemas.Table("CompatRetryTable", "long", "CompatRetryBean"));
		s.compile();
		return s;
	}
}
