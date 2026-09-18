package Zeze.Raft;

import harness.Fast;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * FND8-38回归：Agent.resend 判死（createTime基准，门槛rpc.getTimeout()，分支在前）
 * 与重发（sendTime基准，间隔AppendEntriesTimeout）双基准竞速——显式超时<=间隔的
 * rpc判死先于首次重发成立，重发永不触发，(clientId,requestId)去重闭环失效
 * （应答丢失后无法按同号取回结果）。原一次性告警不改行为（注释自认残留缺口）。
 * 修复（方案一，间隔修正为 min(AET, 门槛-扫描周期)）：per-rpc间隔保证门槛大于1s
 * 扫描周期的rpc在判死前至少经历一次同号重发；判死门槛与时延不变；t<=扫描周期
 * 受tick粒度限制不保证（文档注明）。启动期对AET>=5000（Rpc默认超时）加一次性告警。
 * 纯单元：按resend的同构时序（判死分支在前、1s tick网格、双基准）做确定性模拟。
 */
@Fast
public class TestFnd838AgentResendInterval {

	private static final long AET = 2000; // RaftConfig默认AppendEntriesTimeout

	// 复刻resend的分支时序：1s网格tick（相位任意），判死在前（continue），重发在后。
	// 返回判死前是否至少重发一次。phase∈[0,1000)模拟rpc创建时刻与tick的相对相位。
	private static boolean resendFiresBeforeDeadline(int rpcTimeout, long interval, long phase) {
		long sendTime = 0; // createTime=0，首次重发前sendTime==createTime
		for (long elapsed = phase; elapsed <= rpcTimeout + 1000; elapsed += 1000) {
			if (rpcTimeout > 0 && elapsed > rpcTimeout)
				return false; // 判死在前（continue），此后不再重发
			if (elapsed - sendTime > interval) {
				if (elapsed > rpcTimeout)
					throw new IllegalStateException("unreachable: 判死分支应先命中");
				sendTime = elapsed; // 同号重发
				return true;
			}
		}
		return false; // timeout=0无判死，永远重发（此处不模拟）
	}

	// 门槛大于扫描周期的rpc，判死前必须至少经历一次同号重发（对tick相位任意成立）。
	@Test
	public void testGuaranteeAtLeastOneResendBeforeDeadline() {
		for (int rpcTimeout : new int[]{1500, 2000, 2500, 3000, 4999, 5000, 8000, 60_000}) {
			long interval = Agent.resendIntervalOf(AET, rpcTimeout);
			for (long phase = 0; phase < 1000; phase += 50) { // 任意tick相位
				assertTrue(resendFiresBeforeDeadline(rpcTimeout, interval, phase),
						"timeout=" + rpcTimeout + " phase=" + phase + " 必须判死前至少重发一次");
			}
		}
	}

	// 修复前形态（固定间隔=AET）对窗口内rpc不满足该性质（红基准，证明测试有区分度）。
	@Test
	public void testOldFixedIntervalFails() {
		int rpcTimeout = 1500;
		long oldInterval = AET;
		boolean anyFail = false;
		for (long phase = 0; phase < 1000; phase += 50)
			anyFail |= !resendFiresBeforeDeadline(rpcTimeout, oldInterval, phase);
		assertTrue(anyFail, "固定间隔=AppendEntriesTimeout 对窗口内rpc必然存在判死先于首发的相位");
	}

	// 间隔不超过AppendEntriesTimeout（正常rpc重发节奏不加密）；timeout=0维持AET。
	@Test
	public void testIntervalBounded() {
		assertTrue(Agent.resendIntervalOf(AET, 0) == AET, "timeout=0维持AET");
		assertTrue(Agent.resendIntervalOf(AET, 60_000) == AET, "大门槛维持AET节奏");
		assertTrue(Agent.resendIntervalOf(AET, 2500) <= AET);
		assertTrue(Agent.resendIntervalOf(AET, 1500) < 1500 - 1000 + 1, "间隔必须小于门槛-扫描周期（保证性）");
	}
}
