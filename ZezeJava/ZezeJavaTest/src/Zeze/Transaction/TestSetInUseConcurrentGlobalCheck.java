package Zeze.Transaction;

import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * setInUse的并发首启动窗口：A/B同时首启动且global行不存在时，双方快照读都
 * 看不到对方未提交的行，都走插入分支；B的插入撞A未提交主键阻塞，A提交后
 * B的INSERT IGNORE/ON CONFLICT DO NOTHING被静默丢弃——若不校验插入行数并
 * 重读比较，cur_global<>in_global的一致性校验整体旁路，不同global的两个
 * 实例双双启动成功（ret=4防护恰在需要它的窗口失效）。
 * 过程体必须：插入后校验行数，为0即重读global并比较；MySQL为REPEATABLE READ，
 * 重读必须是FOR UPDATE锁定读（普通读仍是旧快照）。真实MySQL/PG服务不可用，
 * 这里对启动时下发的过程体文本做契约断言（行为确认待真实环境）。
 */
@Fast
public class TestSetInUseConcurrentGlobalCheck {

	private static int countOccurrences(String text, String token) {
		int count = 0;
		for (int i = text.indexOf(token); i >= 0; i = text.indexOf(token, i + token.length()))
			count++;
		return count;
	}

	@Test
	public void pgReReadsGlobalWhenInsertNoOps() {
		var sql = DatabasePostgreSQL.PROC_SET_IN_USE_SQL;
		assertEquals(2, countOccurrences(sql, "SELECT data INTO cur_global"),
				"ON CONFLICT被静默忽略后必须重读已提交global并比较（READ COMMITTED新语句取新快照）");
	}

	@Test
	public void mySqlReReadsGlobalWithLockingRead() {
		var sql = DatabaseMySql.PROC_SET_IN_USE_SQL;
		assertEquals(2, countOccurrences(sql, "SELECT data INTO cur_global"),
				"INSERT IGNORE被静默忽略后必须重读global并比较");
		assertTrue(sql.contains("FOR UPDATE"),
				"REPEATABLE READ下重读必须是锁定读：普通读仍是事务旧快照，检不出对方已提交的global");
	}
}
