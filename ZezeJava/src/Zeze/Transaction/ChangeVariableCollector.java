package Zeze.Transaction;

import java.util.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public abstract class ChangeVariableCollector {
	private static final Logger logger = LogManager.getLogger(ChangeVariableCollector.class);

	public HashSet<ChangeListener> listeners;

	public abstract void CollectChanged(ArrayList<Zeze.Util.KV<Bean, Integer>> path, ChangeNote note);

	public final void NotifyRecordChanged(Object key, Bean value) {
		for (var l : listeners) {
			try {
				l.OnChanged(key, value);
			}
			catch (RuntimeException ex) {
				logger.error("NotifyRecordChanged", ex);
			}
		}
	}

	public final void NotifyRecordRemoved(Object key) {
		for (var l : listeners) {
			try {
				l.OnRemoved(key);
			}
			catch (RuntimeException ex) {
				logger.error("NotifyRecordRemoved", ex);
			}
		}
	}

	public abstract void NotifyVariableChanged(Object key, Bean value);
}