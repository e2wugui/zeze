package Zeze.Transaction;

import org.jetbrains.annotations.NotNull;

@FunctionalInterface
public interface ChangeListener {
	void OnChanged(@NotNull Object key, @NotNull Changes.Record r);
}
