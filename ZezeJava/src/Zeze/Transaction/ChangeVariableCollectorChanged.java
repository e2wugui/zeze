package Zeze.Transaction;

import Zeze.*;
import java.util.*;

public final class ChangeVariableCollectorChanged extends ChangeVariableCollector {
	private static final NLog.Logger logger = NLog.LogManager.GetCurrentClassLogger();

	private boolean changed = false;

	@Override
	public void CollectChanged(ArrayList<Util.KV<Bean, Integer>> path, ChangeNote note) {
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
			catch (RuntimeException ex) {
				logger.Error(ex, "NotifyVariableChanged");
			}
		}
	}
}