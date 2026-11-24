package Zeze.Onz;

import java.util.concurrent.ConcurrentHashMap;
import Zeze.Builtin.Onz.BSavedCommits;
import Zeze.Transaction.EmptyBean;
import harness.Fast;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@Fast
public class TestCommitDecisionSurvivesDeliveryFailure {
	@Test
	void errorAfterSavingCommitDecisionDoesNotInitiateRollback() throws Exception {
		var server = mock(OnzServer.class);
		setField(server, "onzAgent", new OnzAgent());
		setField(server, "hangWarnedTids", ConcurrentHashMap.newKeySet());
		doCallRealMethod().when(server).perform(any());
		doThrow(new AssertionError("delivery failed")).when(server).removeCommitRecord(any());
		var transaction = new OnzTransaction<EmptyBean.Data, EmptyBean.Data>() {
			@Override
			protected long perform() {
				return 0;
			}

			@Override
			boolean rollback() {
				throw new AssertionError("rollback after saving the commit decision");
			}
		};
		transaction.setOnzServer(server);

		assertEquals(0, server.perform(transaction));
		var saves = inOrder(server);
		saves.verify(server).saveCommitPoint(any(byte[].class), any(BSavedCommits.Data.class), eq(AbstractOnz.ePreparing));
		saves.verify(server).saveCommitPoint(any(byte[].class), any(BSavedCommits.Data.class), eq(AbstractOnz.eCommitting));
		verify(server, times(1)).saveCommitPoint(any(), any(), eq(AbstractOnz.ePreparing));
	}

	private static void setField(OnzServer server, String name, Object value) throws Exception {
		var field = OnzServer.class.getDeclaredField(name);
		field.setAccessible(true);
		field.set(server, value);
	}
}
