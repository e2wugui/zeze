using System;
using System.Collections.Generic;
using System.Text;

namespace Zeze.Transaction
{
	public sealed class ChangeCollector
	{
		private Dictionary<int, ChangeTableCollector> tables = new Dictionary<int, ChangeTableCollector>(); // key is Table.Id

		public void CollectChanged(TableKey tableKey, Transaction.RecordAccessed recordAccessed, List<KeyValuePair<Bean, int>> path, ChangeNote note)
        {
			if (tables.TryGetValue(tableKey.TableId, out var ctc))
			{
				ctc.CollectChanged(recordAccessed, path, note);
				return;
			}
			Table table = Table.GetTable(tableKey.TableId);
			ChangeTableCollector ctcNew = new ChangeTableCollector(table);
			tables.Add(tableKey.TableId, ctcNew);
			ctcNew.CollectChanged(recordAccessed, path, note);
		}
	}

	public sealed class ChangeTableCollector
	{
		private Dictionary<object, ChangeRecordCollector> records = new Dictionary<object, ChangeRecordCollector>(); // key is Record.Key
		private Table table;
		private bool tableHasListener;

		public ChangeTableCollector(Table table)
		{
			this.table = table;
			this.tableHasListener = table.ChangeListenerMap.HasListener();
		}

		public void CollectChanged(Transaction.RecordAccessed recordAccessed, List<KeyValuePair<Bean, int>> path, ChangeNote note)
		{
			if (false == this.tableHasListener)
				return; // 优化，表格没有监听者时，不收集改变。

			ChangeRecordCollector crc;
			if (false == records.TryGetValue(recordAccessed.OriginRecord.KeyObject, out crc))
            {
				crc = new ChangeRecordCollector(table, recordAccessed);
				records.Add(recordAccessed.OriginRecord.KeyObject, crc);
            }
			crc.CollectChanged(path, note);
		}
	}

	public sealed class ChangeRecordCollector
	{
		private Dictionary<int, ChangeVariableCollector> variables = new Dictionary<int, ChangeVariableCollector>(); // key is VariableId
		private Transaction.RecordAccessed recordAccessed;

		public ChangeRecordCollector(Table table, Transaction.RecordAccessed recordAccessed)
		{
			this.recordAccessed = recordAccessed;
			Dictionary<int, HashSet<ChangeListener>> tmp = table.ChangeListenerMap.mapCopy;
			foreach (var e in tmp)
            {
				ChangeVariableCollector cvc = (e.Key == 0) ? new ChangeVariableCollectorChanged() : table.CreateChangeVariableCollector(e.Key);
				if (null != cvc) // 忽略掉不正确的 variableId，也许注册的时候加个检查更好，先这样了。
				{
					variables.Add(e.Key, cvc);
					cvc.listeners = e.Value;
				}
            }
		}

		public void CollectChanged(List<KeyValuePair<Bean, int>> path, ChangeNote note)
		{
			if (recordAccessed.CommittedPutLog != null)
				return; // 记录发生了覆盖或者删除，此时整个记录都变了，不再收集变量的详细变化。

			// 如果监听了整个记录的变化
			if (variables.TryGetValue(0, out var beanCC))
			{
				beanCC.CollectChanged(path, note); // bean 发生了变化仅仅记录trur|false，不需要更新path。
			}
			// 收集具体变量变化
			if (variables.TryGetValue(path[^1].Value, out var varCC))
            {
				path.RemoveAt(path.Count - 1); // 最后一个肯定是 Root-Bean 的变量。
				varCC.CollectChanged(path, note);
            }
		}
	}

	public abstract class ChangeVariableCollector
	{
		private static readonly NLog.Logger logger = NLog.LogManager.GetCurrentClassLogger();

		internal HashSet<ChangeListener> listeners;

		public abstract void CollectChanged(List<KeyValuePair<Bean, int>> path, ChangeNote note);

		public void NotifyRecordChanged(object key, Bean value)
        {
			foreach (var l in listeners)
            {
				try
                {
					l.OnChanged(key, value);
				}
				catch (Exception ex)
                {
					logger.Error(ex, "NotifyRecordChanged");
                }
			}
        }

		public void NotifyRecordRemoved(object key)
        {
			foreach (var l in listeners)
			{
				try
                {
					l.OnRemoved(key);
				}
				catch (Exception ex)
				{
					logger.Error(ex, "NotifyRecordRemoved");
				}
			}
		}

		public abstract void NotifyVariableChanged(object key, Bean value);
	}

	public sealed class ChangeVariableCollectorChanged : ChangeVariableCollector
	{
		private static readonly NLog.Logger logger = NLog.LogManager.GetCurrentClassLogger();

		private bool changed = false;

		public override void CollectChanged(List<KeyValuePair<Bean, int>> path, ChangeNote note)
		{
			changed = true;
		}

		public override void NotifyVariableChanged(object key, Bean value)
        {
			if (false == changed)
				return;

			foreach (var l in listeners)
			{
				try
                {
					l.OnChanged(key, value, null);
				}
				catch (Exception ex)
                {
					logger.Error(ex, "NotifyVariableChanged");
                }
			}
		}
	}

	public sealed class ChangeVariableCollectorMap : ChangeVariableCollector
	{
		private static readonly NLog.Logger logger = NLog.LogManager.GetCurrentClassLogger();

		private ChangeNote note;
		private Util.IdentityHashMap<Bean, Bean> changedValue;

		public override void CollectChanged(List<KeyValuePair<Bean, int>> path, ChangeNote note)
		{
			if (path.Count == 0)
            {
				this.note = note; // 肯定只有一个。这里就不检查了。
            }
			else
            {
				if (null == changedValue)
					changedValue = new Util.IdentityHashMap<Bean, Bean>();
				// Value 不是 Bean 的 Map 不会走到这里。
				Bean value = path[^1].Key;
				changedValue.TryAdd(value, value);
            }
		}

		public override void NotifyVariableChanged(object key, Bean value)
        {
			if (null == note && null == changedValue)
				return;

			if (null == note)
				note = null;
			note.SetChangedValue(changedValue);

			foreach (var l in listeners)
			{
				try
				{
					l.OnChanged(key, value, note);
				}
				catch (Exception ex)
				{
					logger.Error(ex, "NotifyVariableChanged");
				}
			}
		}
	}

	public sealed class ChangeVariableCollectorSet : ChangeVariableCollector
	{
		public override void CollectChanged(List<KeyValuePair<Bean, int>> path, ChangeNote note)
		{
			// TODO
		}

		public override void NotifyVariableChanged(object key, Bean value)
        {
			// TODO 
        }
	}
}
