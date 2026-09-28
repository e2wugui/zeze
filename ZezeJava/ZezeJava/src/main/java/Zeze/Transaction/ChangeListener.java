package Zeze.Transaction;

import org.jetbrains.annotations.NotNull;

/** 表数据变更监听器：记录提交后按 key 与变更记录触发回调。 */
@FunctionalInterface
public interface ChangeListener {
	void OnChanged(@NotNull Object key, @NotNull Changes.Record r);
}
