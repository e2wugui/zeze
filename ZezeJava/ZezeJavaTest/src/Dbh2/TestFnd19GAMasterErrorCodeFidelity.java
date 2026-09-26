package Dbh2;

import Zeze.Serialize.ByteBuffer;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * FND19 案外留档（GA-C06备注）回归：eVersionMismatch到客户端变形为eDefaultError → 已修（错误码保真）。
 * Master的SaveDataWithSameVersion/SetInUse handler是finally-SendResult模式（错误码预置+finally
 * 抢在派发层之前发送），早退路径的返回值不写入resultCode——派发层trySendResultCode输给finally的
 * 发送CAS。变形时客户端stale CAS收到eDefaultError，走客户端default分支抛RuntimeException，
 * 而不是eVersionMismatch的KV(0,false)CAS拒绝语义（SetInUse的重复localId等三个早退同型）。
 * 修复=早退路径预置resultCode再返回（返回值保持原值，直调断言不受影响）。
 * 集成拓扑（Dbh2TestEnv）：走真实rpc派发路径，钉住客户端视角的错误码契约。
 */
public class TestFnd19GAMasterErrorCodeFidelity {

	@Test
	public void testStaleVersionSaveReachesClientAsVersionMismatch() throws Exception {
		var env = new Dbh2TestEnv();
		env.prepareNewEnvironment();
		try {
			var operates = env.database.getDirectOperates();
			var key = ByteBuffer.Wrap("fnd19.errcode.key".getBytes());
			// 首次插入（version以参数0落盘）+ 正常CAS推进到version=1。
			Assertions.assertTrue(operates.saveDataWithSameVersion(key, ByteBuffer.Wrap(new byte[]{1}), 0).getValue());
			Assertions.assertTrue(operates.saveDataWithSameVersion(key, ByteBuffer.Wrap(new byte[]{2}), 0).getValue());

			// stale CAS：客户端必须收到eVersionMismatch（映射为KV(0,false)）。
			// 变形时收到eDefaultError走default分支抛RuntimeException。
			var stale = operates.saveDataWithSameVersion(key, ByteBuffer.Wrap(new byte[]{3}), 0);
			Assertions.assertFalse(stale.getValue(), "stale CAS必须以eVersionMismatch拒绝，而不是异常变形");

			// 数据未被stale覆盖。
			var loaded = operates.getDataWithVersion(key);
			Assertions.assertEquals(1, loaded.version);
			Assertions.assertEquals(1, loaded.data.size());
		} finally {
			env.stopAll();
		}
	}
}
