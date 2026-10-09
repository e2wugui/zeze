package Zeze.Transaction;

import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * PG存储过程的块级EXCEPTION不得吞错：RAISE EXCEPTION 'ROLLBACK'是控制流
 * （借子事务回滚撤销函数内已写行并保留ret_value），但 WHEN OTHERS 无条件收尾
 * 会把函数体内的一切真实错误（死锁40P01、约束违反、磁盘错误）静默吞掉，
 * 函数以ret_value=1正常返回——Java侧只抛"Unknown Error"，依赖
 * "deadlock detected"消息的64次重试循环永远不可达，可重试故障退化为硬失败
 * 且掩盖根因。三个过程体的WHEN OTHERS必须条件重抛（仅放过ROLLBACK控制流）。
 * 真实PG服务不可用于测试环境，这里对启动时CREATE OR REPLACE下发的过程体
 * 文本做契约断言（PG行为验证待真实环境）。
 */
@Fast
public class TestPostgreSqlProceduresReraiseErrors {

	private static void assertReraiseControlFlow(String name, String sql) {
		Assertions.assertTrue(sql.contains("EXCEPTION WHEN OTHERS THEN"), name + "：保留块级EXCEPTION控制流（回滚已写行）");
		var handler = sql.substring(sql.indexOf("EXCEPTION WHEN OTHERS THEN"));
		Assertions.assertTrue(handler.contains("IF SQLERRM <> 'ROLLBACK' THEN"),
				name + "：WHEN OTHERS必须区分控制流与真实错误");
		Assertions.assertTrue(handler.contains("RAISE;"),
				name + "：真实错误必须重抛到JDBC（死锁重试依赖'deadlock detected'消息到达Java侧）");
		// 吞错形态：EXCEPTION WHEN OTHERS 直接 END，无任何重抛分支
		Assertions.assertFalse(handler.lines().map(String::trim).anyMatch(l -> l.equals("END;") && handler.indexOf("RAISE;") > handler.indexOf(l)),
				name + "：不得存在无条件吞错的收尾");
	}

	@Test
	public void saveDataWithSameVersionReraisesRealErrors() {
		assertReraiseControlFlow("saveDataWithSameVersion", DatabasePostgreSQL.PROC_SAVE_DATA_WITH_SAME_VERSION_SQL);
	}

	@Test
	public void setInUseReraisesRealErrors() {
		assertReraiseControlFlow("setInUse", DatabasePostgreSQL.PROC_SET_IN_USE_SQL);
	}

	@Test
	public void clearInUseReraisesRealErrors() {
		assertReraiseControlFlow("clearInUse", DatabasePostgreSQL.PROC_CLEAR_IN_USE_SQL);
	}
}
