package Zeze.Transaction;

import Zeze.*;
import java.util.*;

public final class ChangeRecordCollector {
	private final HashMap<Integer, ChangeVariableCollector> variables = new HashMap<Integer, ChangeVariableCollector>(); // key is VariableId
	private final Zeze.Transaction.RecordAccessed recordAccessed;
	private final Object key;

	public ChangeRecordCollector(TableKey tableKey, Table table, Zeze.Transaction.RecordAccessed recordAccessed) {
		this.recordAccessed = recordAccessed;
		key = tableKey.getKey();

		// 记录发生了覆盖或者删除，也需要把listener建立好，以便后面Notify。但是就不需要收集log和note了。参见下面的 CollectChanged.
		HashMap<Integer, HashSet<ChangeListener>> tmp = table.ChangeListenerMap.mapCopy;
		for (var e : tmp.entrySet()) {
			ChangeVariableCollector cvc = table.CreateChangeVariableCollector(e.getKey());
			if (null != cvc) { // 忽略掉不正确的 variableId，也许注册的时候加个检查更好，先这样了。
				variables.put(e.getKey(), cvc);
				cvc.listeners = e.getValue();
			}
		}
	}

	public void CollectChanged(ChangeCollector.Collect collect) {
		if (recordAccessed.getCommittedPutLog() != null) {
			return; // 记录发生了覆盖或者删除，此时整个记录都变了，不再收集变量的详细变化。
		}

		// 如果监听了整个记录的变化
		TValue beanCC;
		if (variables.containsKey(0) && (beanCC = variables.get(0)) == beanCC) {
			beanCC.CollectChanged(null, null); // bean 发生了变化仅仅记录trur|false，不需要 path and note。
			if (variables.size() == 1) {
				return; // 只有记录级别的监听者，已经完成收集，不再需要继续处理。
			}
		}

		// 收集具体变量变化，需要建立路径用来判断是否该变量的变化。
		java.util.ArrayList<Util.KV<Bean, Integer>> path;
		ChangeNote note;
//C# TO JAVA CONVERTER TODO TASK: The following method call contained an unresolved 'out' keyword - these cannot be converted using the 'OutObject' helper class unless the method is within the code being modified:
		collect.invoke(out path, out note);
		TValue varCC;
		if (variables.containsKey(path[path.Count - 1].Value) && (varCC = variables.get(path[path.Count - 1].Value)) == varCC) {
			path.RemoveAt(path.Count - 1); // 最后一个肯定是 Root-Bean 的变量。
			varCC.CollectChanged(path, note);
		}
	}

	public void Notify() {
		if (recordAccessed.getCommittedPutLog() != null) {
			if (recordAccessed.getCommittedPutLog().getValue() == null) {
				for (var cvc : variables.values()) {
					cvc.NotifyRecordRemoved(key);
				}
			}
			else {
				for (var cvc : variables.values()) {
					cvc.NotifyRecordChanged(key, recordAccessed.getOriginRecord().getValue());
				}
			}
		}
		else {
			for (var cvc : variables.values()) {
				cvc.NotifyVariableChanged(key, recordAccessed.getOriginRecord().getValue());
			}
		}
	}
}