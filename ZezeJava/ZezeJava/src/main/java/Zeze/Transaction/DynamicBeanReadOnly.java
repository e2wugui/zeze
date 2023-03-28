package Zeze.Transaction;

import org.jetbrains.annotations.NotNull;

public interface DynamicBeanReadOnly {
	long getTypeId();

	@NotNull Bean getBean();
}
