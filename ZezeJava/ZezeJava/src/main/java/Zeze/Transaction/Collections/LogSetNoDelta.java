package Zeze.Transaction.Collections;

/** Final persistent state for commits without change consumers. */
final class LogSetNoDelta<V> extends LogSet<V> {
    LogSetNoDelta(PSet<V> source, org.pcollections.PSet<V> value) {
        super(source, value);
    }

    @Override
    public boolean hasDelta() {
        return false;
    }

    @SuppressWarnings("unchecked")
    @Override
    public LogSetNoDelta<V> beginSavepoint() {
        return new LogSetNoDelta<>((PSet<V>)source, getValue());
    }
}
