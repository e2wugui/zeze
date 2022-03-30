package Zeze.Transaction;

@FunctionalInterface
public interface DynamicIdToBean {
	Bean toBean(long id);
}
