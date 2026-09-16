package UnitTest.Zeze.Net;

import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import Zeze.Net.AsyncSocket;
import harness.Fast;

/**
 * FND7-19：AsyncSocket默认sessionId发号基址为1，每个JVM内独立从1起号。Raft复制状态
 * 用sessionId判活（ServiceManagerWithRaft.reconcileSessions用GetSocket(sessionId)判
 * 死会话）时跨JVM/leader代必然碰撞：死agent的会话行被现任leader上同号活连接误判存活，
 * 幽灵服务地址持续分发。修复：默认基址随机化（nanoTime ^ SecureRandom，取正63位）。
 * <p>
 * 验证：默认发号器的当前值必须远离1起号的顺序段（远大于1e6）——修复前恒为
 * 1+本JVM已建socket数（个位到百位量级），断言红；修复后为随机63位基址（值&lt;1e6的
 * 概率约1e-13，可忽略）。跨JVM行为无法在同进程内直接复现（静态发号器全JVM一份），
 * 基址随机化正是使"两个独立JVM的起号序列重叠"概率可忽略的根因修复。
 */
@Fast
public class TestFnd719SessionIdEpoch {
	@Test
	public void testDefaultSessionIdBaseIsRandomized() throws Exception {
		var field = AsyncSocket.class.getDeclaredField("sessionIdGen");
		field.setAccessible(true);
		var gen = (AtomicLong)field.get(null);
		Assertions.assertTrue(gen.get() > 1_000_000,
				"默认sessionId发号基址必须随机化（跨JVM唯一，FND7-19），当前值=" + gen.get()
						+ " 疑似仍为顺序起号");
	}
}
