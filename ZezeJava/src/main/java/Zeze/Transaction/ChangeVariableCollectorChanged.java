package Zeze.Transaction;

import java.util.*;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

public final class ChangeVariableCollectorChanged extends ChangeVariableCollector {
	private static final Logger logger = LogManager.getLogger(ChangeVariableCollectorChanged.class);

	private boolean changed = false;

	@Override
	public void CollectChanged(ArrayList<Zeze.Util.KV<Bean, Integer>> path, ChangeNote note) {
		changed = true;
	}

	@Override
	public void NotifyVariableChanged(Object key, Bean value) {
		if (false == changed) {
			return;
		}

		for (var l : listeners) {
			try {
				l.OnChanged(key, value, null);
			}
			catch (Throwable ex) {
				logger.error("NotifyVariableChanged", ex);
			}
		}
	}
}