package Zeze.Transaction.Collections;

import java.util.Objects;

import Zeze.Transaction.Bean;
import Zeze.Transaction.Changes;
import Zeze.Transaction.Savepoint;
import Zeze.Transaction.TransactionLog;
import org.jspecify.annotations.Nullable;

/** Transaction-only persistent collection state; change notes are optional. */
public abstract class LogCollection implements TransactionLog {
	protected final Collection source;

	protected LogCollection(Collection source) {
		this.source = Objects.requireNonNull(source);
	}

	public final Collection getSourceBean() {
		return source;
	}

	@Override
	public final @Nullable Bean getBelong() {
		return source.parent();
	}

	@Override
	public final int getVariableId() {
		return source.variableId();
	}

	@Override
	public final Category category() {
		return Category.eHistory;
	}

	public abstract boolean hasDelta();

	@Override
	public abstract LogCollection beginSavepoint();

	protected abstract void mergeFrom(LogCollection child);

	@Override
	public final void endSavepoint(Savepoint parent) {
		var existing = parent.getLog(getLogKey());
		if (existing == null)
			parent.putLog(this);
		else {
			if (existing.getClass() != getClass())
				throw new IllegalStateException("collection capture mode changed inside savepoint");
			((LogCollection)existing).mergeFrom(this);
		}
	}

	@Override
	public final void collectChanges(Changes changes) {
		changes.collectContainer(source);
	}
}
