package Zeze.Transaction;

import org.jspecify.annotations.Nullable;

/**
 * Local state owned by a savepoint. Collection implementations never enter the wire tree.
 * Atomic field logs may also be Log values, because their new value is already the complete payload.
 * Changes collects Log payloads and collection state; other implementations remain local to the transaction.
 */
public interface TransactionLog {
	@Nullable Bean getBelong();

	int getVariableId();

	default long getLogKey() {
		var owner = getBelong();
		if (owner == null)
			throw new IllegalStateException("decoded log has no transaction target");
		return owner.objectId() + getVariableId();
	}

	default TransactionLog beginSavepoint() {
		return this;
	}

	default void endSavepoint(Savepoint parent) {
		parent.putLog(this);
	}

	void commit();
}
