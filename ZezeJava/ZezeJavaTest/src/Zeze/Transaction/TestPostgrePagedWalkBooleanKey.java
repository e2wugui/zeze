package Zeze.Transaction;

import org.jspecify.annotations.NullMarked;

import java.util.List;

import Zeze.Serialize.SQLStatement;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import static Zeze.Transaction.DatabasePostgreSQL.pagedKeyWhere;

/**
 * PostgreSQL 支持 boolean 标量和复合行值的大小比较。分页从首批返回的 boolean
 * 起始键继续时应构造排他谓词，不能仅因 SQLStatement.appendBoolean 产生 true/false 就拒绝。
 * 本类测试真实 SQL 装配函数；服务器能力由独立 PostgreSQL 17.6 实例执行相同谓词验证，
 * 此 Fast 测试自身不连接数据库。
 */
@Fast
@NullMarked
public class TestPostgrePagedWalkBooleanKey {

	@Test
	public void testBooleanSingleColumnForwardAndBackward() {
		var st = new SQLStatement();
		st.appendBoolean("flag", false);
		Assertions.assertEquals(" WHERE flag>false", pagedKeyWhere(st.getSql().toString(), true));
		st.clear();
		st.appendBoolean("flag", true);
		Assertions.assertEquals(" WHERE flag<true", pagedKeyWhere(st.getSql().toString(), false));
	}

	@Test
	public void testBooleanFirstInCompositeKey() {
		var st = new SQLStatement();
		st.appendBoolean("flag", false);
		st.appendInt("seq", 2);
		Assertions.assertEquals(" WHERE (flag, seq) > (false, 2)",
				pagedKeyWhere(st.getSql().toString(), true));
		Assertions.assertEquals(" WHERE (flag, seq) < (false, 2)",
				pagedKeyWhere(st.getSql().toString(), false));
	}

	@Test
	public void testBooleanLastInCompositeKey() {
		var st = new SQLStatement();
		st.appendInt("seq", 1);
		st.appendBoolean("flag", true);
		Assertions.assertEquals(" WHERE (seq, flag) > (1, true)",
				pagedKeyWhere(st.getSql().toString(), true));
		Assertions.assertEquals(" WHERE (seq, flag) < (1, true)",
				pagedKeyWhere(st.getSql().toString(), false));
	}

	@Test
	public void testBooleanAlongsideBoundString() {
		var st = new SQLStatement();
		st.appendString("tenant", "tenant_1");
		st.appendBoolean("flag", false);
		Assertions.assertEquals(" WHERE (tenant, flag) > (?, false)",
				pagedKeyWhere(st.getSql().toString(), true));
		Assertions.assertEquals(" WHERE (tenant, flag) < (?, false)",
				pagedKeyWhere(st.getSql().toString(), false));
		Assertions.assertEquals(List.of("tenant_1"), st.getParams());
	}

	@Test
	public void testNonBooleanPredicatesUnchanged() {
		// 单列：保持col>literal形态
		Assertions.assertEquals(" WHERE id>1", pagedKeyWhere("id=1", true));
		Assertions.assertEquals(" WHERE id<1", pagedKeyWhere("id=1", false));
		// 复合列：元组行值比较（游标语义），参数占位符相对顺序不变
		Assertions.assertEquals(" WHERE (id, seq) > (1, 5)", pagedKeyWhere("id=1, seq=5", true));
		Assertions.assertEquals(" WHERE (id, seq) < (1, 5)", pagedKeyWhere("id=1, seq=5", false));
		Assertions.assertEquals(" WHERE (name, seq) > (?, 5)", pagedKeyWhere("name=?, seq=5", true));
	}
}
