package UnitTest.Zeze.Net;

import harness.Fast;
import Zeze.Net.FamilyClass;
import Zeze.Raft.RaftRpc;
import Zeze.Serialize.ByteBuffer;
import demo.Module1.BValue;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * FND8-54回归：FamilyClass.isRaftRpc用宽松的>=RaftResponse判定，非法familyClass(5..31)
 * 被RaftRpc.decode当作合法Raft应答接受（setRequest恒false按Response静误解），对照
 * Protocol/Rpc路径的严格校验不对称——非法帧的排障信号从invalid-header退化为
 * lost-context/bean解码错。
 * 修复后：枚举相等判定（RaftResponse|RaftRequest），非法familyClass一律invalid-header异常。
 */
@Fast
public class TestFnd854RaftRpcFamilyClass {

	public static class TestRaftRpc extends RaftRpc<BValue, BValue> {
		public TestRaftRpc() {
			Argument = new BValue();
			Result = new BValue();
		}

		@Override
		public int getModuleId() {
			return 95;
		}

		@Override
		public int getProtocolId() {
			return 54;
		}
	}

	// 非法familyClass(5..31)必须在门处被拒——invalid header异常（修复前被当RaftResponse放行）
	@Test
	public void testIllegalFamilyClassRejected() {
		for (int fc = 5; fc <= FamilyClass.FamilyClassMask; fc++) {
			var bb = ByteBuffer.Allocate(8);
			bb.WriteUInt(fc); // 仅帧头：门失败必须先于任何body读取
			var rpc = new TestRaftRpc();
			var header = fc;
			var ex = Assertions.assertThrows(IllegalStateException.class, () -> rpc.decode(bb),
					"familyClass=" + fc + " 必须被拒绝");
			Assertions.assertTrue(ex.getMessage().contains("invalid header"),
					"familyClass=" + header + " 期望invalid-header诊断信号: " + ex.getMessage());
			Assertions.assertTrue(rpc.isRequest(), "门失败时不得改写isRequest（修复前被静默置为Response形态）");
		}
	}

	// 合法familyClass（3=RaftResponse、4=RaftRequest）不受影响：encode/decode往返形态正确
	@Test
	public void testLegalFamilyClassRoundtrip() {
		var req = new TestRaftRpc();
		req.Argument.setInt_1(7);
		var encoded = req.encode(); // 12字节协议头 + RaftRpc.encode体
		encoded.ReadIndex = 12; // decode从familyClass变长头开始
		var decoded = new TestRaftRpc();
		decoded.decode(encoded);
		Assertions.assertTrue(decoded.isRequest(), "RaftRequest(4)形态往返保持");
		Assertions.assertEquals(7, decoded.Argument.getInt_1());

		var resp = new TestRaftRpc();
		resp.setRequest(false);
		resp.Result.setInt_1(9);
		var encoded2 = resp.encode();
		encoded2.ReadIndex = 12;
		var decoded2 = new TestRaftRpc();
		decoded2.decode(encoded2);
		Assertions.assertFalse(decoded2.isRequest(), "RaftResponse(3)形态往返保持");
		Assertions.assertEquals(9, decoded2.Result.getInt_1());
	}
}
