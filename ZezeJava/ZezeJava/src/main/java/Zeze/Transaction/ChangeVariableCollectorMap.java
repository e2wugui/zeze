package Zeze.Transaction;

import java.util.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class ChangeVariableCollectorMap extends ChangeVariableCollector {
	private static final Logger logger = LogManager.getLogger(ChangeVariableCollectorMap.class);

	private ChangeNote note;
	private IdentityHashMap<Bean, Bean> changedValue;

	private final Zeze.Util.Factory<ChangeNote> NoteFactory;

	public ChangeVariableCollectorMap(Zeze.Util.Factory<ChangeNote> noteFactory) {
		NoteFactory = noteFactory;
	}

	@Override
	public void CollectChanged(ArrayList<Zeze.Util.KV<Bean, Integer>> path, ChangeNote note) {
		if (path.isEmpty()) {
			this.note = note; // 肯定只有一个。这里就不检查了。
		}
		else {
			if (null == changedValue) {
				changedValue = new IdentityHashMap<>();
			}
			// Value 不是 Bean 的 Map 不会走到这里。
			Bean value = path.get(path.size() - 1).getKey();
			changedValue.putIfAbsent(value, value);
		}
	}

	@Override
	public void NotifyVariableChanged(Object key, Bean value) {
		if (null == note && null == changedValue) {
			return;
		}

		if (null == note) {
			note = NoteFactory.create(); // 动态创建的 note 是没有所属容器的引用，也就丢失了所在 Bean 的引用。
		}
		note.SetChangedValue(changedValue);

		for (var l : listeners) {
			try {
				l.OnChanged(key, value, note);
			}
			catch (Throwable ex) {
				logger.error("NotifyVariableChanged", ex);
			}
		}
	}
}
