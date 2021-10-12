package Zeze.Transaction;

import java.util.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class ChangeVariableCollectorSet extends ChangeVariableCollector {
	private static final Logger logger = LogManager.getLogger(ChangeVariableCollectorSet.class);
	private ChangeNote note;

	@Override
	public void CollectChanged(ArrayList<Zeze.Util.KV<Bean, Integer>> path, ChangeNote note) {
		// 忽略错误检查
		this.note = note;
	}

	@Override
	public void NotifyVariableChanged(Object key, Bean value) {
		if (null == note) {
			return;
		}

		for (var l : listeners) {
			try {
				l.OnChanged(key, value, note);
			}
			catch (RuntimeException ex) {
				logger.error("NotifyVariableChanged", ex);
			}
		}
	}
}