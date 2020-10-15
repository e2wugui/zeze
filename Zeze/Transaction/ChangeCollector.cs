using System;
using System.Collections.Generic;
using System.Text;

namespace Zeze.Transaction
{
	public sealed class ChangeCollector
	{
		private readonly Dictionary<int, ChangeTableCollector> tables = new Dictionary<int, ChangeTableCollector>(); // key is Table.Id

		public delegate void Collect(out List<KeyValuePair<Bean, int>> path, out ChangeNote note);

		public void BuildCollect(TableKey tableKey, Transaction.RecordAccessed recordAccessed)
        {
			if (false == tables.TryGetValue(tableKey.TableId, out var tableCollector))
            {
				tableCollector = new ChangeTableCollector(tableKey);
				tables.Add(tableKey.TableId, tableCollector);
			}
			tableCollector.BuildCollect(tableKey, recordAccessed);
		}

		public void CollectChanged(TableKey tableKey, Collect collect)
        {
			if (tables.TryGetValue(tableKey.TableId, out var ctc))
			{
				ctc.CollectChanged(tableKey, collect);
			}
			// else skip error 只有测试代码可能会走到这个分支。
		}

		public void Notify()
		{
			foreach (var ctc in tables.Values)
				ctc.Notify();
		}
	}

	public sealed class ChangeTableCollector
	{
		private readonly Dictionary<object, ChangeRecordCollector> records = new Dictionary<object, ChangeRecordCollector>(); // key is Record.Key
		private readonly Table table;
		private readonly bool tableHasListener;

		public ChangeTableCollector(TableKey tableKey)
		{
			table = Table.GetTable(tableKey.TableId);
			tableHasListener = table.ChangeListenerMap.HasListener();
		}

		public void BuildCollect(TableKey tableKey, Transaction.RecordAccessed recordAccessed)
        {
			if (tableHasListener)
			{
				ChangeRecordCollector recordCollector = new ChangeRecordCollector(tableKey, table, recordAccessed);
				records.Add(tableKey.Key, recordCollector);
			}
		}

		public void CollectChanged(TableKey tableKey, ChangeCollector.Collect collect)
		{
			if (false == this.tableHasListener)
				return; // 优化，表格没有监听者时，不收集改变。

			if (records.TryGetValue(tableKey.Key, out var crc))
            {
				crc.CollectChanged(collect);
			}
			// else skip error . 只有测试代码可能会走到这个分支。
		}

		public void Notify()
        {
			if (false == this.tableHasListener)
				return;

			foreach (var crc in records.Values)
				crc.Notify();
        }
	}

	public sealed class ChangeRecordCollector
	{
		private readonly Dictionary<int, ChangeVariableCollector> variables = new Dictionary<int, ChangeVariableCollector>(); // key is VariableId
		private readonly Transaction.RecordAccessed recordAccessed;
		private readonly object key;

		public ChangeRecordCollector(TableKey tableKey, Table table, Transaction.RecordAccessed recordAccessed)
		{
			this.recordAccessed = recordAccessed;
			key = tableKey.Key;

			// 记录发生了覆盖或者删除，也需要把listener建立好，以便后面Notify。但是就不需要收集log和note了。参见下面的 CollectChanged.
			Dictionary<int, HashSet<ChangeListener>> tmp = table.ChangeListenerMap.mapCopy;
			foreach (var e in tmp)
			{
				ChangeVariableCollector cvc = table.CreateChangeVariableCollector(e.Key);
				if (null != cvc) // 忽略掉不正确的 variableId，也许注册的时候加个检查更好，先这样了。
				{
					variables.Add(e.Key, cvc);
					cvc.listeners = e.Value;
				}
			}
		}

		public void CollectChanged(ChangeCollector.Collect collect)
		{
			if (recordAccessed.CommittedPutLog != null)
				return; // 记录发生了覆盖或者删除，此时整个记录都变了，不再收集变量的详细变化。

			// 如果监听了整个记录的变化
			if (variables.TryGetValue(0, out var beanCC))
			{
				beanCC.CollectChanged(null, null); // bean 发生了变化仅仅记录trur|false，不需要 path and note。
				if (variables.Count == 1)
					return; // 只有记录级别的监听者，已经完成收集，不再需要继续处理。
			}

            // 收集具体变量变化，需要建立路径用来判断是否该变量的变化。
            collect(out List<KeyValuePair<Bean, int>> path, out ChangeNote note);
            if (variables.TryGetValue(path[^1].Value, out var varCC))
            {
				path.RemoveAt(path.Count - 1); // 最后一个肯定是 Root-Bean 的变量。
				varCC.CollectChanged(path, note);
            }
		}

		public void Notify()
        {
			if (recordAccessed.CommittedPutLog != null)
            {
				if (recordAccessed.CommittedPutLog.Value == null)
                {
					foreach (var cvc in variables.Values)
						cvc.NotifyRecordRemoved(key);
				}
				else
                {
					foreach (var cvc in variables.Values)
						cvc.NotifyRecordChanged(key, recordAccessed.OriginRecord.Value);
				}
			}
			else
            {
				foreach (var cvc in variables.Values)
					cvc.NotifyVariableChanged(key, recordAccessed.OriginRecord.Value);
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

		private readonly Func<ChangeNote> NoteFactory;

		public ChangeVariableCollectorMap(Func<ChangeNote> noteFactory)
        {
			NoteFactory = noteFactory;
		}

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
				note = NoteFactory(); // 动态创建的 note 是没有所属容器的引用，也就丢失了所在 Bean 的引用。
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
		private static readonly NLog.Logger logger = NLog.LogManager.GetCurrentClassLogger();
		private ChangeNote note;

		public override void CollectChanged(List<KeyValuePair<Bean, int>> path, ChangeNote note)
		{
			// 忽略错误检查
			this.note = note;
		}

		public override void NotifyVariableChanged(object key, Bean value)
        {
			if (null == note)
				return;

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
}
