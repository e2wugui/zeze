package UnitTest.Zeze.Transaction;

import static org.junit.jupiter.api.Assertions.assertEquals;

import Zeze.Config;
import Zeze.Transaction.DatabaseMySql;
import Zeze.Transaction.DatabaseRelationalMapping;
import harness.Fast;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * T2-F2 回归：MySQL 关系映射表 string key 列原先用未指定排序规则的 VARCHAR(256) 做
 * PRIMARY KEY，默认 *_ci 排序规则下大小写变体 key 被合并（REPLACE 静默删行、
 * find/remove 命中错误行）。修复：DatabaseMySql 覆写 getKeyStringType 返回
 * VARCHAR(256) COLLATE utf8mb4_bin，与 Zeze 缓存的字节比较语义一致。
 * DisableOperates 构造不触库（DruidDataSource 懒连接），建表 SQL 断言不依赖
 * 真实 MySQL；存量表 ALTER 见 docs（database/relational.md）。
 */
@Fast
public class TestMysqlKeyStringTypeCollate {
	private DatabaseMySql db;

	@BeforeEach
	public void setUp() {
		var conf = new Config.DatabaseConf();
		conf.setDatabaseType(Config.DbType.MySql);
		conf.setDatabaseUrl("jdbc:mysql://127.0.0.1:1/t2_f2_no_connect");
		conf.setDruidConf(new Config.DruidConf());
		conf.setDisableOperates(true); // 不创建 Operates（其构造会连库建表/过程）
		db = new DatabaseMySql(null, conf);
	}

	@AfterEach
	public void tearDown() {
		db.close();
	}

	@Test
	public void testKeyStringTypeHasBinaryCollation() {
		assertEquals("VARCHAR(256) COLLATE utf8mb4_bin", db.getKeyStringType(),
				"MySQL string key 列必须显式二进制排序规则");
	}

	@Test
	public void testDefaultKeyStringTypeUnchanged() {
		// 接口默认值保持不变（PostgreSQL 依赖它：text 等值恒为字节精确，无需覆写）。
		DatabaseRelationalMapping mapping = new DatabaseRelationalMapping() {
			@Override
			public Zeze.Transaction.Database.@NotNull Table openRelationalTable(@NotNull String name) {
				return null;
			}

			@Override
			public java.util.Map<String, String> getSqlTypeMap() {
				return null;
			}
		};
		assertEquals("VARCHAR(256)", mapping.getKeyStringType());
	}
}
