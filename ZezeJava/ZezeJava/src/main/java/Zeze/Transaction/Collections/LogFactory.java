package Zeze.Transaction.Collections;

@FunctionalInterface
public interface LogFactory<T> {
	Zeze.Transaction.Log create(T collection);
}
