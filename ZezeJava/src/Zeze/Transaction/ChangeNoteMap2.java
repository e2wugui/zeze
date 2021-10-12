package Zeze.Transaction;

import java.util.*;

public final class ChangeNoteMap2<K, V extends Bean> extends ChangeNoteMap1<K, V> {
	// 记录 map 中的 value 发生了改变。需要查找原 Map 才能映射到 Replaced 中。
	// Notify 的时候由 Collector 设置。
	private IdentityHashMap<Bean, Bean> ChangedValue;

	public ChangeNoteMap2(Zeze.Transaction.Collections.PMap<K, V> map) {
		super(map);
	}

	/** 
	 使用 Replaced 之前调用这个方法把 Map 中不是增删，而是直接改变 value 的数据合并到 Replaced 之中。
	*/
	public void MergeChangedToReplaced(Zeze.Transaction.Collections.PMap2<K, V> map) {
		if (null == ChangedValue || ChangedValue.isEmpty()) {
			return;
		}

		for (var e : map.entrySet()) {
			if (ChangedValue.containsKey(e.getValue())) {
				if (!getReplaced().containsKey(e.getKey())) {
				   getReplaced().put(e.getKey(), e.getValue());
				}
			}
		}

		ChangedValue.clear();
	}

	@Override
	public void SetChangedValue(IdentityHashMap<Bean, Bean> values) {
		ChangedValue = values;
	}
}