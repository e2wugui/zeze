package Zeze.Transaction;

import Zeze.*;
import java.util.*;

public final class ChangeVariableCollectorMap extends ChangeVariableCollector {
	private static final NLog.Logger logger = NLog.LogManager.GetCurrentClassLogger();

	private ChangeNote note;
	private Util.IdentityHashMap<Bean, Bean> changedValue;

	private final tangible.Func0Param<ChangeNote> NoteFactory;

	public ChangeVariableCollectorMap(tangible.Func0Param<ChangeNote> noteFactory) {
		NoteFactory = ::noteFactory;
	}

	@Override
	public void CollectChanged(ArrayList<Util.KV<Bean, Integer>> path, ChangeNote note) {
		if (path.isEmpty()) {
			this.note = note; // 肯定只有一个。这里就不检查了。
		}
		else {
			if (null == changedValue) {
				changedValue = new Util.IdentityHashMap<Bean, Bean>();
			}
			// Value 不是 Bean 的 Map 不会走到这里。
			Bean value = path.get(path.size() - 1).getKey();
			if (!changedValue.containsKey(value)) {
//C# TO JAVA CONVERTER TODO TASK: There is no Java ConcurrentHashMap equivalent to this .NET ConcurrentDictionary method:
				changedValue.TryAdd(value, value);
			}
		}
	}

	@Override
	public void NotifyVariableChanged(Object key, Bean value) {
		if (null == note && null == changedValue) {
			return;
		}

		if (null == note) {
			note = NoteFactory.invoke(); // 动态创建的 note 是没有所属容器的引用，也就丢失了所在 Bean 的引用。
		}
		note.SetChangedValue(changedValue);

		for (var l : listeners) {
			try {
				l.OnChanged(key, value, note);
			}
			catch (RuntimeException ex) {
				logger.Error(ex, "NotifyVariableChanged");
			}
		}
	}
}