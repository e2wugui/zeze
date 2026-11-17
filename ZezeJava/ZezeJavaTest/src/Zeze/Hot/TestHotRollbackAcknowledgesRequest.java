package Zeze.Hot;

import Zeze.Builtin.HotDistribute.TryRollback;
import harness.Fast;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

@Fast
public class TestHotRollbackAcknowledgesRequest {
	@Test
	void acceptedRollbackCompletesItsOwnRpc() throws Exception {
		var service = waitingForCommit();
		var request = new TryRollback();
		request.Argument.setDistributeId(71);
		assertEquals(0, service.ProcessTryRollbackRequest(request));
		assertTrue(request.isSendResultDone(), "返回0不触发派发层应答，handler必须自行发送成功结果");
		assertFalse(request.isRequest());
		var state = HotDistribute.class.getDeclaredField("state");
		state.setAccessible(true);
		assertEquals(HotDistribute.eTryRollback, state.getInt(service));
	}

	@Test
	void mismatchedSessionRemainsAnErrorForDispatcher() throws Exception {
		var service = waitingForCommit();
		var request = new TryRollback();
		request.Argument.setDistributeId(72);
		assertEquals(service.errorCode(HotDistribute.eDistributeIdMismatch),
				service.ProcessTryRollbackRequest(request));
		assertFalse(request.isSendResultDone());
	}

	private static HotDistribute waitingForCommit() throws Exception {
		var service = new HotDistribute(null);
		var state = HotDistribute.class.getDeclaredField("state");
		state.setAccessible(true);
		state.setInt(service, HotDistribute.eTryDistribute);
		var id = HotDistribute.class.getDeclaredField("distributeId");
		id.setAccessible(true);
		id.setLong(service, 71);
		return service;
	}
}
