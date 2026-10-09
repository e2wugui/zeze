package Zeze.Trans;

import java.lang.reflect.InvocationTargetException;

import Zeze.Transaction.DatabasePostgreSQL;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * PG 后端分页 walk 的排他起始键谓词用行值比较 (col1,col2)&gt;(v1,v2)：bool 列在
 * PG 没有 &gt;/&lt; 运算符，bool 作复合 key 首列时拼出的谓词直接报
 * "operator does not exist"，报错远离根因。MySQL（bool=tinyint，TRUE=1）不受影响，
 * 仅 PG 路径显式拒绝。
 *
 * "=true"/"=false" 字面量只能来自 SQLStatement.appendBoolean（数值内联数字、
 * 字符串/二进制用 ? 参数），按内容检测可靠。
 */
@Fast
public class TestPostgrePagedWalkBooleanKey {

	private static String pagedKeyWhere(String table, String sql, boolean asc) throws Exception {
		var m = DatabasePostgreSQL.class.getDeclaredMethod("pagedKeyWhere", String.class, String.class, boolean.class);
		m.setAccessible(true);
		return (String)m.invoke(null, table, sql, asc);
	}

	@Test
	public void testBooleanKeyRejectedWithTableName() throws Exception {
		var ex = Assertions.assertThrows(InvocationTargetException.class,
				() -> pagedKeyWhere("t_bool", "flag=true, seq=1", true));
		var cause = ex.getCause();
		Assertions.assertInstanceOf(IllegalStateException.class, cause);
		Assertions.assertTrue(cause.getMessage().contains("boolean"), cause.getMessage());
		Assertions.assertTrue(cause.getMessage().contains("t_bool"), "拒绝消息应带表名");
		// bool在非首列同样拒绝（行值比较整组无'>'运算符即报错）
		var ex2 = Assertions.assertThrows(InvocationTargetException.class,
				() -> pagedKeyWhere("t_bool2", "seq=1, flag=false", false));
		Assertions.assertInstanceOf(IllegalStateException.class, ex2.getCause());
	}

	@Test
	public void testNonBooleanPredicatesUnchanged() throws Exception {
		// 单列：保持col>literal形态
		Assertions.assertEquals(" WHERE id>1", pagedKeyWhere("t", "id=1", true));
		Assertions.assertEquals(" WHERE id<1", pagedKeyWhere("t", "id=1", false));
		// 复合列：元组行值比较（游标语义），参数占位符相对顺序不变
		Assertions.assertEquals(" WHERE (id, seq) > (1, 5)", pagedKeyWhere("t", "id=1, seq=5", true));
		Assertions.assertEquals(" WHERE (id, seq) < (1, 5)", pagedKeyWhere("t", "id=1, seq=5", false));
		Assertions.assertEquals(" WHERE (name, seq) > (?, 5)", pagedKeyWhere("t", "name=?, seq=5", true));
	}
}
