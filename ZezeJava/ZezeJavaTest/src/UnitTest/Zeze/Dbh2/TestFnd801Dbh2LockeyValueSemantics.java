package UnitTest.Zeze.Dbh2;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import Zeze.Net.Binary;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * FND8-01回归：Dbh2.Lockey未覆写hashCode/equals（身份语义），Locks按hashCode/equals
 * 去重并选segment，导致相同Binary键每次查询都注册新Lockey（各带全新Semaphore(1)），
 * Dbh2记录锁的2PC时间窗互斥完全失效。修复：值语义委托final字段key。
 * 测试钉住契约：相同Binary值必须命中同一注册实例（互斥的前提），
 * equals/hashCode/compareTo三者一致，Dbh2Transaction的HashMap<Lockey,Lockey>去重随之恢复。
 */
@Fast
public class TestFnd801Dbh2LockeyValueSemantics {

	@Test
	public void testLocksDedupByValue() {
		var locks = new Zeze.Dbh2.Locks();
		var key1 = new Binary("a1_fnd801_key".getBytes(StandardCharsets.UTF_8));
		var key1Copy = new Binary("a1_fnd801_key".getBytes(StandardCharsets.UTF_8));
		var key2 = new Binary("a1_fnd801_other".getBytes(StandardCharsets.UTF_8));
		Assertions.assertEquals(key1, key1Copy);

		// 修复前红：身份语义下两次查询各注册新Lockey（不同实例、不同信号量）
		var lock1 = locks.get(key1);
		var lock1Again = locks.get(key1Copy);
		Assertions.assertSame(lock1, lock1Again, "same key value must resolve to the same registered lockey");

		var lock2 = locks.get(key2);
		Assertions.assertNotSame(lock1, lock2);

		// contains以前因每次new且身份比较恒false，修复后才有意义
		Assertions.assertTrue(locks.contains(key1Copy));
		Assertions.assertFalse(locks.contains(new Binary("a1_fnd801_unknown".getBytes(StandardCharsets.UTF_8))));
	}

	@Test
	public void testEqualsHashCodeCompareToConsistent() {
		var key = new Binary("a1_fnd801_key".getBytes(StandardCharsets.UTF_8));
		var a = new Zeze.Dbh2.Lockey(key);
		var b = new Zeze.Dbh2.Lockey(new Binary("a1_fnd801_key".getBytes(StandardCharsets.UTF_8)));
		Assertions.assertEquals(a, b);
		Assertions.assertEquals(a.hashCode(), b.hashCode());
		Assertions.assertEquals(0, a.compareTo(b));
		Assertions.assertNotEquals(a, new Zeze.Dbh2.Lockey(new Binary("a1_fnd801_other".getBytes(StandardCharsets.UTF_8))));
	}

	@Test
	public void testTransactionLockMapDedup() {
		// Dbh2Transaction用HashMap<Lockey,Lockey>对同一batch内的重复键去重，依赖值语义
		var locks = new Zeze.Dbh2.Locks();
		var key = new Binary("a1_fnd801_key".getBytes(StandardCharsets.UTF_8));
		var map = new HashMap<Zeze.Dbh2.Lockey, Zeze.Dbh2.Lockey>();
		var first = locks.get(key);
		Assertions.assertNull(map.putIfAbsent(first, first));
		// puts与deletes含同一key时第二次putIfAbsent应命中既有条目，不重复加锁
		var second = locks.get(new Binary("a1_fnd801_key".getBytes(StandardCharsets.UTF_8)));
		Assertions.assertSame(first, map.putIfAbsent(second, second));
		Assertions.assertEquals(1, map.size());
	}
}
