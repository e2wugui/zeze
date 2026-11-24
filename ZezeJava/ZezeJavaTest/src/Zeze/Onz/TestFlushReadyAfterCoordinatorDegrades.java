package Zeze.Onz;

import java.util.Map;
import Zeze.Builtin.Onz.FlushReady;
import Zeze.Transaction.EmptyBean;
import Zeze.Util.Task;
import Zeze.Util.TaskCompletionSource;
import harness.Fast;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@Fast
public class TestFlushReadyAfterCoordinatorDegrades {
	@Test
	@SuppressWarnings("unchecked")
	void readyArrivingDuringFallbackCheckpointIsAcknowledgedImmediately() throws Exception {
		Task.tryInitThreadPool();
		var server = mock(OnzServer.class);
		var txn = new OnzTransaction<EmptyBean.Data, EmptyBean.Data>() {
			@Override
			protected long perform() {
				return 0;
			}
		};
		txn.setOnzServer(server);
		txn.setFlushTimeout(1_000);
		var field = OnzTransaction.class.getDeclaredField("zezeProcedures");
		field.setAccessible(true);
		var participants = (Map<String, TaskCompletionSource<?>>)field.get(txn);
		participants.put("first", new TaskCompletionSource<>());
		participants.put("second", new TaskCompletionSource<>());
		var ready = new FlushReady();
		ready.Argument.setParticipant("first");
		doAnswer(call -> {
			txn.trySetFlushReady(ready);
			assertTrue(ready.isSendResultDone());
			throw new IllegalStateException("stop before sending a real checkpoint RPC");
		}).when(server).getZezeInstance(anyString());

		txn.waitFlushDone();

		verify(server, times(2)).getZezeInstance(anyString());
	}
}
