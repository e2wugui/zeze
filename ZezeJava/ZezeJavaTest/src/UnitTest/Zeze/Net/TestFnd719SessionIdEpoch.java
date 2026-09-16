package UnitTest.Zeze.Net;

import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import Zeze.Net.Service;
import harness.Fast;

/**
 * FND7-19（R3迁移后）：默认sessionId发号流原从1起号，跨JVM/leader代必撞。修复：
 * 基址随机化（nanoTime ^ SecureRandom，取正63位）。R3发号下沉到Service实例后，
 * 默认流为全JVM共享的Service.staticSessionIdAtomicLong（共享保证同JVM多App不撞号，
 * 随机基址保证跨JVM不撞号），socket构造时经所属Service.nextSessionId()取号。
 * <p>
 * 验证：共享发号流当前值必须远离1起号的顺序段（远大于1e6）。
 */
@Fast
public class TestFnd719SessionIdEpoch {
	@Test
	public void testDefaultSessionIdBaseIsRandomized() throws Exception {
		var field = Service.class.getDeclaredField("staticSessionIdAtomicLong");
		field.setAccessible(true);
		var gen = (AtomicLong)field.get(null);
		Assertions.assertTrue(gen.get() > 1_000_000,
				"默认sessionId发号基址必须随机化（跨JVM唯一，FND7-19），当前值=" + gen.get()
						+ " 疑似仍为顺序起号");
	}
}
