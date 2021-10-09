package Zeze.Transaction;

import Zeze.*;
import java.util.*;

public abstract class ChangeVariableCollector {
	private static final NLog.Logger logger = NLog.LogManager.GetCurrentClassLogger();

	public HashSet<ChangeListener> listeners;

	public abstract void CollectChanged(ArrayList<Util.KV<Bean, Integer>> path, ChangeNote note);

	public final void NotifyRecordChanged(Object key, Bean value) {
		for (var l : listeners) {
			try {
				l.OnChanged(key, value);
			}
			catch (RuntimeException ex) {
				logger.Error(ex, "NotifyRecordChanged");
			}
		}
	}

	public final void NotifyRecordRemoved(Object key) {
		for (var l : listeners) {
			try {
				l.OnRemoved(key);
			}
			catch (RuntimeException ex) {
				logger.Error(ex, "NotifyRecordRemoved");
			}
		}
	}

	public abstract void NotifyVariableChanged(Object key, Bean value);
}