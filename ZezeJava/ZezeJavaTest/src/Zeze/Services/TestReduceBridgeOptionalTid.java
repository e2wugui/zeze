package Zeze.Services;

import Zeze.Builtin.GlobalCacheManagerWithRaft.Reduce;
import Zeze.Net.Binary;
import Zeze.Raft.ProxyRequest;
import Zeze.Serialize.ByteBuffer;
import Zeze.Util.Id128;
import harness.Fast;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

@Fast
public class TestReduceBridgeOptionalTid {
	private static final class CapturingProxy extends ProxyRequest {
		Binary reply = Binary.Empty;
		int sends;

		@Override
		public void SendResult(@Nullable Binary ignored) {
			reply = Result.getData();
			++sends;
		}
	}

	@Test
	public void absentTidIsEncodedAsZeroForEveryResponseEntry() {
		for (int mode = 0; mode < 4; ++mode) {
			var proxy = new CapturingProxy();
			var real = new Reduce();
			real.Argument.setGlobalKey(new Binary("key"));
			real.setProxyRequest(proxy);
			var bridge = new GlobalCacheManagerWithRaftAgent.ReduceBridge(real);
			bridge.Result.state = GlobalCacheManagerConst.StateInvalid;
			assertNull(bridge.Result.reducedTid);
			long code = mode == 0 ? 0 : GlobalCacheManagerConst.ReduceInvalidAlreadyIsInvalid;
			switch (mode) {
			case 0:
				bridge.SendResult();
				break;
			case 1:
				bridge.SendResultCode(code);
				break;
			case 2:
				assertTrue(bridge.trySendResultCode(code));
				break;
			default:
				bridge.SendResultCode(code, null);
			}
			assertEquals(1, proxy.sends);
			var bb = ByteBuffer.Wrap(proxy.reply);
			assertEquals(real.getModuleId(), bb.ReadInt4());
			assertEquals(real.getProtocolId(), bb.ReadInt4());
			assertEquals(bb.size() - 4, bb.ReadInt4());
			var received = new Reduce();
			received.decode(bb);
			assertTrue(bb.isEmpty());
			assertEquals(code, received.getResultCode());
			assertEquals(new Binary("key"), received.Result.getGlobalKey());
			assertEquals(GlobalCacheManagerConst.StateInvalid, received.Result.getState());
			assertEquals(Id128.Zero, received.Result.getReduceTid());
			assertFalse(bridge.trySendResultCode(999));
			assertEquals(1, proxy.sends);
		}
	}
}
