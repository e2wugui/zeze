package Zeze.Transaction.Collections;

import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import Zeze.Transaction.Bean;
import org.jspecify.annotations.Nullable;

/** 源提交回调期间的完整值投影；容器不可写，Bean 仍受同步监听生命周期约束。 */
public final class Map2ListenerView<K, V extends Bean> {
	private final Map<K, V> replaced;
	private final Map<K, LogBean> changed;
	private final Set<K> removed;
	private final @Nullable Comparator<? super K> upsertOrder;

	private Map2ListenerView(Map<K, V> replaced, Map<K, LogBean> changed, Set<K> removed,
			@Nullable Comparator<? super K> upsertOrder) {
		this.replaced = replaced;
		this.changed = changed;
		this.removed = removed;
		this.upsertOrder = upsertOrder;
	}

	/** 由源 Changes.Record 创建；解码日志没有原位修改对应的完整源 Bean。 */
	public static <K, V extends Bean> Map2ListenerView<K, V> of(LogMap2<K, V> log) {
		requireSource(log);
		return new Map2ListenerView<>(log.replacedForRead(), log.changedForRead(), log.removedForRead(), null);
	}

	public static <K extends Comparable<K>, V extends Bean> Map2ListenerView<K, V> of(LogSortedMap2<K, V> log) {
		requireSource(log);
		var order = log.comparator();
		return new Map2ListenerView<>(log.replacedForRead(), log.changedForRead(), log.removedForRead(),
				order != null ? order : Comparator.naturalOrder());
	}

	public void forEachUpsert(BiConsumer<? super K, ? super V> action) {
		if (upsertOrder != null)
			forEachSortedUpsert(replaced, changed, upsertOrder, action);
		else
			forEachUpsert(replaced, changed, action);
	}

	public void forEachRemoved(Consumer<? super K> action) {
		removed.forEach(action);
	}

	public Set<K> getRemoved() {
		return Collections.unmodifiableSet(removed);
	}

	/** 直接投影入口不创建 view，也不物化 replaced+changed 联合集合。 */
	public static <K, V extends Bean> void forEachUpsert(LogMap2<K, V> log,
			BiConsumer<? super K, ? super V> action) {
		requireSource(log);
		forEachUpsert(log.replacedForRead(), log.changedForRead(), action);
	}

	public static <K extends Comparable<K>, V extends Bean> void forEachUpsert(LogSortedMap2<K, V> log,
			BiConsumer<? super K, ? super V> action) {
		requireSource(log);
		var order = log.comparator();
		forEachSortedUpsert(log.replacedForRead(), log.changedForRead(),
				order != null ? order : Comparator.naturalOrder(), action);
	}

	private static void requireSource(LogBean log) {
		if (log.getThis() == null)
			throw new IllegalStateException("listener projection requires a collected source log");
	}

	private static void validateEdits(Map<?, LogBean> changed) {
		// 在任何业务 action 前拒绝缺少源 Bean 的增量，避免半次投影产生副作用。
		changed.forEach((key, edit) -> {
			if (edit.getThis() == null)
				throw new IllegalStateException("listener projection requires source Beans");
		});
	}

	@SuppressWarnings("unchecked")
	private static <K, V extends Bean> void forEachUpsert(Map<K, V> replaced, Map<K, LogBean> changed,
			BiConsumer<? super K, ? super V> action) {
		if (changed.isEmpty()) {
			// HashMap/TreeMap 的 forEach 可直接遍历节点，不为纯结构变更建立条目迭代器。
			replaced.forEach(action);
			return;
		}
		validateEdits(changed);
		replaced.forEach(action);
		for (var entry : changed.entrySet()) {
			V value = (V)Objects.requireNonNull(entry.getValue().getThis(),
					"listener projection requires a source Bean");
			action.accept(entry.getKey(), value);
		}
	}

	@SuppressWarnings("unchecked")
	private static <K, V extends Bean> void forEachSortedUpsert(Map<K, V> replaced, Map<K, LogBean> changed,
			Comparator<? super K> order, BiConsumer<? super K, ? super V> action) {
		if (replaced.isEmpty() || changed.isEmpty()) {
			forEachUpsert(replaced, changed, action);
			return;
		}
		Objects.requireNonNull(action);
		validateEdits(changed);
		var replacements = replaced.entrySet().iterator();
		var edits = changed.entrySet().iterator();
		var replacement = replacements.hasNext() ? replacements.next() : null;
		var edit = edits.hasNext() ? edits.next() : null;
		while (replacement != null || edit != null) {
			int comparison = replacement != null && edit != null
					? order.compare(replacement.getKey(), edit.getKey()) : 0;
			if (edit == null || replacement != null && comparison <= 0) {
				action.accept(replacement.getKey(), replacement.getValue());
				replacement = replacements.hasNext() ? replacements.next() : null;
				if (edit != null && comparison == 0)
					edit = edits.hasNext() ? edits.next() : null;
			} else {
				action.accept(edit.getKey(), (V)Objects.requireNonNull(edit.getValue().getThis()));
				edit = edits.hasNext() ? edits.next() : null;
			}
		}
	}
}
