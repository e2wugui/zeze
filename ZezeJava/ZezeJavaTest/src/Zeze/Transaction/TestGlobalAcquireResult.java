package Zeze.Transaction;

import Zeze.Services.GlobalCacheManagerConst;
import harness.Fast;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Fast
public class TestGlobalAcquireResult implements GlobalCacheManagerConst {
	@Test
	public void recognizesExistingModifyGrant() {
		assertTrue(new IGlobalAgent.AcquireResult(AcquireShareAlreadyIsModify, StateModify, null).isSuccess());
		assertTrue(new IGlobalAgent.AcquireResult(AcquireModifyAlreadyIsModify, StateModify, null).isSuccess());
	}

	@Test
	public void rejectsFailureResponsesEvenWhenTheyCarryAState() {
		assertFalse(new IGlobalAgent.AcquireResult(AcquireModifyDeadLockFound, StateModify, null).isSuccess());
		assertFalse(new IGlobalAgent.AcquireResult(AcquireShareAlreadyIsModify, StateShare, null).isSuccess());
	}
}
