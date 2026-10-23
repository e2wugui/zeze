package Zeze.Transaction;

import org.jspecify.annotations.Nullable;

/**
 * Local state owned by a savepoint. Collection implementations never enter the wire tree.
 * Atomic field logs may also be Log values, because their new value is already the complete payload.
 */
public interface TransactionLog {
	enum Category {eHistory, eUser, eSpecial}

	@Nullable Bean getBelong();

	int getVariableId();

	default long getLogKey() {
		var owner = getBelong();
		if (owner == null)
			throw new IllegalStateException("decoded log has no transaction target");
		return owner.objectId() + getVariableId();
	}

	Category category();

	default TransactionLog beginSavepoint() {
		return this;
	}

	default void endSavepoint(Savepoint parent) {
		parent.putLog(this);
	}

	void commit();

	void collectChanges(Changes changes);
}
