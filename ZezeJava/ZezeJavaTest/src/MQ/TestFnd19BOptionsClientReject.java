package MQ;

import Zeze.Builtin.MQ.BOptions;
import Zeze.MQ.MQ;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * FND19 GB-D03 转修回归（客户端校验）：createMQ 传入未实现的 BOptions（DoubleWrite/Raft3 等）
 * 必须明确报错拒绝（fail-fast，错误信息含"未实现"），不再静默按 Single 跑。
 * <p>
 * 旧链路：Options 全程被存储、透传、从不校验——传 DoubleWrite 的用户得到 Single 语义
 * （单机无副本），协议层却回显创建成功 + Options 原样回显，静默降级。
 * <p>
 * 校验在 startAndWaitConnectionReady 之前抛出：本测试不需要任何 Master/Manager 在线。
 * Master 端有同款校验兜底（见 TestFnd19BOptionsServerReject），直连 MasterAgent 的调用方
 * 同样被拒绝。0/不传=默认 Single（编码省略形态），兼容既有 null 调用。
 */
@Fast
public class TestFnd19BOptionsClientReject {

	@Test
	public void testDoubleWriteRejected() {
		var ex = Assertions.assertThrows(IllegalArgumentException.class,
				() -> MQ.createMQ("topicBOptionsClient", 1, new BOptions.Data(BOptions.DoubleWrite)));
		Assertions.assertTrue(ex.getMessage().contains("未实现"), "错误信息须含【未实现】：message=" + ex.getMessage());
		Assertions.assertTrue(ex.getMessage().contains(String.valueOf(BOptions.DoubleWrite)),
				"错误信息须含传入值便于定位：message=" + ex.getMessage());
	}

	@Test
	public void testRaft3Rejected() {
		var ex = Assertions.assertThrows(IllegalArgumentException.class,
				() -> MQ.createMQ("topicBOptionsClient", 1, new BOptions.Data(BOptions.Raft3)));
		Assertions.assertTrue(ex.getMessage().contains("未实现"), "message=" + ex.getMessage());
	}

	@Test
	public void testOtherInvalidValuesRejected() {
		// 组合值（Single|Raft3）与任意未知值同样拒绝：只接受 Single 与"未指定"。
		Assertions.assertThrows(IllegalArgumentException.class,
				() -> MQ.createMQ("topicBOptionsClient", 1, new BOptions.Data(BOptions.Single | BOptions.Raft3)));
		Assertions.assertThrows(IllegalArgumentException.class,
				() -> MQ.createMQ("topicBOptionsClient", 1, new BOptions.Data(7)));
		Assertions.assertThrows(IllegalArgumentException.class,
				() -> MQ.createMQ("topicBOptionsClient", 1, new BOptions.Data(-1)));
	}
}
