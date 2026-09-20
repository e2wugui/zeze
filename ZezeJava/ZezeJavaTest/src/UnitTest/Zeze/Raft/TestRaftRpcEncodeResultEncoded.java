package UnitTest.Zeze.Raft;

import java.util.Arrays;

import Zeze.Net.Binary;
import Zeze.Raft.StartServerConnector;
import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.EmptyBean;
import Zeze.Transaction.Procedure;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * R3-F1 回归：RaftRpc.encode 的响应分支丢失基类 resultEncoded Append。
 * Leader对重试请求回RaftApplied应答时（Server.processRequest -> SendResultCode(RaftApplied,
 * state.getRpcResult())），Rpc.SendResult已把已应用结果设置到resultEncoded再编码发送；
 * RaftRpc.encode原实现响应分支固定Result.encode——发送侧实例的Result从未被填充
 * （RaftApplied携带的是保存的已编码结果，不是Result bean），发出的是空bean编码，
 * 客户端拿到静默错误数据。
 * 修复：对齐基类Rpc.encode，响应优先Append resultEncoded。
 */
@Fast
public class TestRaftRpcEncodeResultEncoded {

	/**
	 * RaftApplied重试应答的编码必须以resultEncoded原文收尾（修复前尾部是
	 * 未填充的Result=EmptyBean编码，仅1字节0）。
	 */
	@Test
	public void testRaftAppliedResponseCarriesEncodedResult() {
		var sender = new StartServerConnector();
		sender.setSessionId(42);
		var payload = new byte[] {1, 2, 3, 4, 5, 6, 7, 8, 9, 10};

		// 模拟Server.processRequest的RaftApplied应答：置码+携带已编码结果。
		// sender无socket，Rpc.SendResult对null sender安全返回false（仅告警日志），状态已就绪。
		sender.SendResultCode(Procedure.RaftApplied, new Binary(payload));
		Assertions.assertFalse(sender.isRequest(), "SendResultCode后必须是应答方向");

		var bb = ByteBuffer.Allocate();
		sender.encode(bb);

		Assertions.assertTrue(bb.WriteIndex > payload.length, "编码必须包含头部");
		var tail = Arrays.copyOfRange(bb.Bytes, bb.WriteIndex - payload.length, bb.WriteIndex);
		Assertions.assertArrayEquals(payload, tail, "响应尾部必须是resultEncoded的原文");
	}

	/**
	 * 解码侧闭环：应答头（码/sessionId/unique/createTime）可正常解析，剩余字节
	 * 正是已应用结果原文——接收方按RaftApplied语义用原始字节恢复结果。
	 */
	@Test
	public void testRaftAppliedResponseDecodeHeaderIntact() {
		var sender = new StartServerConnector();
		sender.setSessionId(4242);
		var payload = new byte[] {0, 9, 8, 7, 6, 5}; // 首字节0=空bean标签，EmptyBean.decode可安全消费
		sender.SendResultCode(Procedure.RaftApplied, new Binary(payload));

		var bb = ByteBuffer.Allocate();
		sender.encode(bb);

		var receiver = new StartServerConnector();
		receiver.decode(bb);
		Assertions.assertFalse(receiver.isRequest());
		Assertions.assertEquals(Procedure.RaftApplied, receiver.getResultCode());
		Assertions.assertEquals(4242, receiver.getSessionId());
		// EmptyBean.decode消费首字节0后，剩余必须是resultEncoded原文（修复前剩余为空）
		var rest = Arrays.copyOfRange(bb.Bytes, bb.ReadIndex, bb.WriteIndex);
		Assertions.assertArrayEquals(Arrays.copyOfRange(payload, 1, payload.length), rest);
	}

	/**
	 * 无resultEncoded的普通应答走Result.encode的既有路径不变（锁定另一半语义）。
	 */
	@Test
	public void testPlainResponseStillEncodesResultBean() {
		var sender = new StartServerConnector();
		sender.setSessionId(1);
		sender.setResultCode(Procedure.Success); // 直接置码，不设置resultEncoded
		sender.setRequest(false);
		Assertions.assertSame(EmptyBean.instance, sender.Result);

		var bb = ByteBuffer.Allocate();
		sender.encode(bb);

		var receiver = new StartServerConnector();
		receiver.decode(bb);
		Assertions.assertEquals(Procedure.Success, receiver.getResultCode());
		Assertions.assertEquals(1, receiver.getSessionId());
		Assertions.assertEquals(bb.WriteIndex, bb.ReadIndex, "普通应答编码必须被完整消费");
	}

	/**
	 * 请求方向编码不受影响。
	 */
	@Test
	public void testRequestDirectionUnchanged() {
		var request = new StartServerConnector();
		Assertions.assertSame(EmptyBean.instance, request.Argument);
		var bb = ByteBuffer.Allocate();
		request.encode(bb);
		Assertions.assertTrue(bb.WriteIndex > 0, "请求编码必须产生字节");
		Assertions.assertTrue(request.isRequest());
	}
}
