package Zeze.Raft;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import Zeze.Builtin.GlobalCacheManagerWithRaft.BReduceParam;
import Zeze.Builtin.GlobalCacheManagerWithRaft.Reduce;
import Zeze.Net.Binary;
import Zeze.Net.Protocol;
import Zeze.Serialize.ByteBuffer;
import Zeze.Services.GlobalCacheManagerConst;
import Zeze.Services.GlobalCacheManagerWithRaftAgent;
import Zeze.Transaction.Procedure;
import Zeze.Util.Id128;
import harness.Fast;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/** 应答CAS必须覆盖result-code重载，并保留代理和Reduce桥接的实际编解码路由。 */
@Fast
public class TestRpcResultRouting {
	private static final class CapturingProxy extends ProxyRequest {
		final AtomicInteger sends = new AtomicInteger();
		volatile Binary reply = Binary.Empty;

		@Override
		public void SendResult(Binary ignored) {
			reply = Result.getData();
			sends.incrementAndGet();
		}
	}

	private static ByteBuffer decode(CapturingProxy proxy, Protocol<?> rpc) {
		Assertions.assertEquals(1, proxy.sends.get(), "内层没有sender时仍须通过外层proxy应答一次");
		var bb = ByteBuffer.Wrap(proxy.reply);
		Assertions.assertEquals(rpc.getModuleId(), bb.ReadInt4());
		Assertions.assertEquals(rpc.getProtocolId(), bb.ReadInt4());
		Assertions.assertEquals(bb.size() - 4, bb.ReadInt4());
		rpc.decode(bb);
		return bb;
	}

	@Test
	public void testAllResultCodeOverloadsPreserveProxyRoutingAndEncodedBody() {
		for (int mode = 0; mode < 4; ++mode) {
			var proxy = new CapturingProxy();
			var rpc = new StartServerConnector();
			rpc.setSessionId(413);
			rpc.getUnique().setClientId("proxy-result");
			rpc.getUnique().setRequestId(37);
			rpc.setCreateTime(1235);
			rpc.setProxyRequest(proxy);
			Assertions.assertNull(rpc.getSender());
			long code;
			switch (mode) {
			case 0:
				code = 0;
				rpc.SendResult();
				break;
			case 1:
				code = Procedure.RaftRetry;
				rpc.SendResultCode(code);
				break;
			case 2:
				code = Procedure.RaftApplied;
				rpc.SendResultCode(code, new Binary(new byte[]{0, 9, 8, 7}));
				break;
			default:
				code = Procedure.Unknown;
				Assertions.assertTrue(rpc.trySendResultCode(code));
				break;
			}
			var received = new StartServerConnector();
			var remaining = decode(proxy, received);
			Assertions.assertFalse(received.isRequest());
			Assertions.assertEquals(code, received.getResultCode());
			Assertions.assertEquals(413, received.getSessionId());
			Assertions.assertEquals("proxy-result", received.getUnique().getClientId());
			Assertions.assertEquals(37, received.getUnique().getRequestId());
			Assertions.assertEquals(1235, received.getCreateTime());
			Assertions.assertArrayEquals(mode == 2 ? new byte[]{9, 8, 7} : new byte[0], remaining.Copy());
			Assertions.assertFalse(rpc.trySendResultCode(999));
			rpc.SendResultCode(998, new Binary(new byte[]{0, 6}));
			Assertions.assertEquals(code, rpc.getResultCode(), "败者不可改胜者code");
			Assertions.assertEquals(1, proxy.sends.get());
		}
	}

	private static final class EncodeGateRpc extends StartServerConnector {
		final CountDownLatch encodeEntered = new CountDownLatch(1);
		final CountDownLatch continueEncode = new CountDownLatch(1);

		@Override
		public void encode(ByteBuffer bb) {
			encodeEntered.countDown();
			try {
				if (!continueEncode.await(5, TimeUnit.SECONDS))
					throw new IllegalStateException("winner encode was not released");
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new IllegalStateException(e);
			}
			super.encode(bb);
		}
	}

	@Test
	public void testLosingProxyResponseDoesNotChangeWinnerDuringEncode() throws Exception {
		var proxy = new CapturingProxy();
		var rpc = new EncodeGateRpc();
		rpc.setProxyRequest(proxy);
		var failure = new AtomicReference<Throwable>();
		var winner = new Thread(() -> {
			try {
				rpc.SendResultCode(123, new Binary(new byte[]{0, 11, 22}));
			} catch (Throwable e) {
				failure.set(e);
			}
		}, "proxy-result-winner");
		winner.setDaemon(true);
		winner.start();
		try {
			Assertions.assertTrue(rpc.encodeEntered.await(5, TimeUnit.SECONDS));
			Assertions.assertFalse(rpc.trySendResultCode(900));
			rpc.SendResultCode(901, new Binary(new byte[]{0, 99}));
			rpc.SendResult(new Binary(new byte[]{0, 100}));
		} finally {
			rpc.continueEncode.countDown();
			winner.join(5_000);
		}
		Assertions.assertFalse(winner.isAlive());
		Assertions.assertNull(failure.get());
		var received = new StartServerConnector();
		var remaining = decode(proxy, received);
		Assertions.assertEquals(123, received.getResultCode());
		Assertions.assertArrayEquals(new byte[]{11, 22}, remaining.Copy());
	}

	@Test
	public void testReduceBridgeResultCodeUsesRealProxyAndPreservesMappedFields() {
		for (int mode = 0; mode < 4; ++mode) {
			var proxy = new CapturingProxy();
			var real = new Reduce();
			real.Argument.setGlobalKey(new Binary("key"));
			real.Argument.setState(GlobalCacheManagerConst.StateInvalid);
			real.setProxyRequest(proxy);
			var bridge = new GlobalCacheManagerWithRaftAgent.ReduceBridge(real);
			bridge.Result.state = GlobalCacheManagerConst.StateInvalid;
			bridge.Result.reducedTid = new Id128(8, 9);
			long code = mode < 2 ? 0 : Procedure.Unknown;
			if (mode == 0)
				bridge.SendResult();
			else if (mode == 1)
				bridge.SendResultCode(code);
			else if (mode == 2)
				Assertions.assertTrue(bridge.trySendResultCode(code));
			else {
				var encodedResult = new BReduceParam();
				encodedResult.setGlobalKey(new Binary("encoded"));
				encodedResult.setState(GlobalCacheManagerConst.StateShare);
				encodedResult.setReduceTid(new Id128(15, 16));
				var body = ByteBuffer.Allocate();
				encodedResult.encode(body);
				bridge.SendResultCode(code, new Binary(body));
			}
			var received = new Reduce();
			Assertions.assertTrue(decode(proxy, received).isEmpty());
			Assertions.assertEquals(code, received.getResultCode());
			Assertions.assertFalse(received.isRequest());
			Assertions.assertEquals(new Binary(mode == 3 ? "encoded" : "key"), received.Result.getGlobalKey());
			Assertions.assertEquals(mode == 3 ? GlobalCacheManagerConst.StateShare : GlobalCacheManagerConst.StateInvalid,
					received.Result.getState());
			Assertions.assertEquals(mode == 3 ? new Id128(15, 16) : new Id128(8, 9), received.Result.getReduceTid());
			Assertions.assertTrue(bridge.isSendResultDone());
			Assertions.assertFalse(bridge.isRequest());
			// 迟到的同一bridge应答不可再映射字段到已获胜的real。
			bridge.Result.state = GlobalCacheManagerConst.StateModify;
			bridge.Result.reducedTid = new Id128(99, 100);
			Assertions.assertFalse(bridge.trySendResultCode(998));
			bridge.SendResult();
			Assertions.assertEquals(GlobalCacheManagerConst.StateInvalid, real.Result.getState());
			Assertions.assertEquals(new Id128(8, 9), real.Result.getReduceTid());
			Assertions.assertEquals(code, real.getResultCode());
			Assertions.assertEquals(1, proxy.sends.get());
		}
	}
}
