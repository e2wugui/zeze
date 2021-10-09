package Zeze.Transaction;

import Zeze.*;
import java.util.*;

public final class ChangeVariableCollectorSet extends ChangeVariableCollector {
	private static final NLog.Logger logger = NLog.LogManager.GetCurrentClassLogger();
	private ChangeNote note;

	@Override
	public void CollectChanged(ArrayList<Util.KV<Bean, Integer>> path, ChangeNote note) {
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
				logger.Error(ex, "NotifyVariableChanged");
			}
		}
	}
}