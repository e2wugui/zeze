package Zeze.Transaction;

@FunctionalInterface
public interface ChangeListener {
	void OnChanged(Object key, Changes.Record r);
}
