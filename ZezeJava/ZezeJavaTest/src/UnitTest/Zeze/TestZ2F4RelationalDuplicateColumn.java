package UnitTest.Zeze;

import java.util.ArrayList;
import java.util.Map;

import Zeze.Schemas;
import Zeze.Transaction.Database;
import Zeze.Transaction.DatabaseRelationalMapping;
import org.jetbrains.annotations.NotNull;
import harness.Fast;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Z2-F4回归：列名以'_'拼接路径，不同变量路径可生成重复列名（如顶层int a_b与
 * Bean a{int b}），原先要到关系库执行CREATE TABLE/ADD COLUMN报Duplicate column
 * 才失败且无法定位撞名路径。修复：Table.buildRelationalColumns收集完列后按name
 * 查重，重复即抛带varIds路径的异常（fail-fast）。不改分隔符——列名格式变更会使
 * 既有部署的previous/current列名全部失配，触发大规模伪ALTER。
 */
@Fast
public class TestZ2F4RelationalDuplicateColumn {

	/** 最小关系映射：仅提供toSqlType所需的类型表。 */
	private static final DatabaseRelationalMapping MAPPING = new DatabaseRelationalMapping() {
		@Override
		public @NotNull Database.Table openRelationalTable(@NotNull String name) {
			throw new UnsupportedOperationException("not used in this test");
		}

		@Override
		public Map<String, String> getSqlTypeMap() {
			return Map.of("int", "INT", "long", "BIGINT");
		}
	};

	/** 顶层int a_b 与 Bean a{int b}：两条路径生成同名列a_b，必须fail-fast并给出varIds。 */
	@Test
	public void testDuplicateColumnNameFailFast() {
		var s = new Schemas();
		var beanA = new Schemas.Bean("Z2F4A1", false);
		beanA.addVariable(variable(1, "b", "int"));
		var beanV = new Schemas.Bean("Z2F4Value1", false);
		beanV.addVariable(variable(1, "a_b", "int"));
		beanV.addVariable(variable(2, "a", "Z2F4A1"));
		s.addBean(beanA);
		s.addBean(beanV);
		s.addTable(new Schemas.Table("Z2F4Table1", "long", "Z2F4Value1"));
		s.compile();

		var table = s.tables.get("Z2F4Table1");
		var columns = new ArrayList<Schemas.Column>();
		var ex = assertThrows(IllegalStateException.class,
				() -> table.buildRelationalColumns(columns, MAPPING));
		assertTrue(ex.getMessage().contains("duplicate"), "必须是查重错误: " + ex.getMessage());
		assertTrue(ex.getMessage().contains("a_b"), "报错须指明撞名列名: " + ex.getMessage());
		assertTrue(ex.getMessage().contains("[2, 1]") && ex.getMessage().contains("[2, 2, 1]"),
				"报错须给出两条变量路径的varIds（可定位撞名来源）: " + ex.getMessage());
	}

	/** 护栏：无撞名时正常构建（含嵌套Bean展开），不误伤既有路径。 */
	@Test
	public void testDistinctColumnsStillBuild() {
		var s = new Schemas();
		var beanA = new Schemas.Bean("Z2F4A2", false);
		beanA.addVariable(variable(1, "y", "int"));
		var beanV = new Schemas.Bean("Z2F4Value2", false);
		beanV.addVariable(variable(1, "x", "int"));
		beanV.addVariable(variable(2, "a", "Z2F4A2"));
		s.addBean(beanA);
		s.addBean(beanV);
		s.addTable(new Schemas.Table("Z2F4Table2", "long", "Z2F4Value2"));
		s.compile();

		var table = s.tables.get("Z2F4Table2");
		var columns = new ArrayList<Schemas.Column>();
		assertDoesNotThrow(() -> table.buildRelationalColumns(columns, MAPPING));
		// __key + x + a_y
		assertEquals(3, columns.size(), "无撞名时列数正确");
	}

	private static Schemas.Variable variable(int id, String name, String typeName) {
		var v = new Schemas.Variable();
		v.id = id;
		v.name = name;
		v.typeName = typeName;
		return v;
	}
}
