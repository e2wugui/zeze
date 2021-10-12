package Zeze.Transaction;

@FunctionalInterface
public interface DynamicBeanToId {
	long toId(Bean bean);
}
