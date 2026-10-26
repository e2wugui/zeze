package Zeze.Transaction.Collections;

import java.util.Objects;

import Zeze.Transaction.Bean;
import Zeze.Transaction.Savepoint;
import Zeze.Transaction.TransactionLog;
import org.jspecify.annotations.Nullable;

/** Transaction-only persistent collection state; change notes are optional. */
public abstract class LogCollection<S extends Collection> implements TransactionLog {
	protected final S source;

	protected LogCollection(S source) {
		this.source = Objects.requireNonNull(source);
	}

	public final S getSourceBean() {
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

	public abstract boolean hasDelta();

	@Override
	public abstract LogCollection<S> beginSavepoint();

	protected abstract void mergeFrom(LogCollection<?> child);

	@Override
	public final void endSavepoint(Savepoint parent) {
		var existing = parent.getLog(getLogKey());
		if (existing == null)
			parent.putLog(this);
		else {
			if (existing.getClass() != getClass())
				throw new IllegalStateException("collection capture mode changed inside savepoint");
			((LogCollection<?>)existing).mergeFrom(this);
		}
	}
}
