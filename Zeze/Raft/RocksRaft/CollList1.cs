
using System;
using System.Collections.Generic;
using System.Collections.Immutable;

namespace Zeze.Raft.RocksRaft
{
	public class CollList1<E> : CollList<E>
	{
		public override E this[int index]
		{
			get => Data[index];
			set
			{
				if (value == null)
					throw new ArgumentNullException("value is null");

				if (this.IsManaged)
				{
					var txn = Transaction.Current;
					var log = (LogList1<E>)txn.LogGetOrAdd(Parent.ObjectId + VariableId, CreateLogBean);
					log.SetItem(index, value);
				}
				else
				{
					_list = _list.SetItem(index, value);
				}
			}
		}

		public override void Add(E item)
		{
			if (IsManaged)
			{
				var listLog = (LogList1<E>)Transaction.Current.LogGetOrAdd(Parent.ObjectId + VariableId, CreateLogBean);
				listLog.Add(item);
			}
			else
            {
				_list = _list.Add(item);
			}
		}

		public override void AddRange(IEnumerable<E> items)
		{
			if (IsManaged)
			{
				var listLog = (LogList1<E>)Transaction.Current.LogGetOrAdd(
						Parent.ObjectId + VariableId, CreateLogBean);
				listLog.AddRange(items);
			}
			else
            {
				_list = _list.AddRange(items);
			}
		}

		public override void Clear()
		{
			if (IsManaged)
			{
				var listLog = (LogList1<E>)Transaction.Current.LogGetOrAdd(
						Parent.ObjectId + VariableId, CreateLogBean);
				listLog.Clear();
			}
			else
            {
				_list = ImmutableList<E>.Empty;
			}
		}

		public override void Insert(int index, E item)
		{
			if (IsManaged)
			{
				var listLog = (LogList1<E>)Transaction.Current.LogGetOrAdd(
					Parent.ObjectId + VariableId, CreateLogBean);
				listLog.Insert(index, item);
			}
			else
            {
				_list = _list.Insert(index, item);
			}
		}

		public override bool Remove(E item)
		{
			if (IsManaged)
			{
				var listLog = (LogList1<E>)Transaction.Current.LogGetOrAdd(
						Parent.ObjectId + VariableId, CreateLogBean);
				return listLog.Remove(item);
			}
			else
            {
				var tmp = _list.Remove(item);
				if (tmp == _list)
					return false;
				_list = tmp;
				return true;
			}
		}

		public override void RemoveAt(int index)
		{
			if (IsManaged)
			{
				var listLog = (LogList1<E>)Transaction.Current.LogGetOrAdd(
						Parent.ObjectId + VariableId, CreateLogBean);
				listLog.RemoveAt(index);
			}
			else
            {
				_list = _list.RemoveAt(index);
			}
		}

		public override void RemoveRange(int index, int count)
		{
			if (IsManaged)
			{
				var listLog = (LogList1<E>)Transaction.Current.LogGetOrAdd(
						Parent.ObjectId + VariableId, CreateLogBean);
				listLog.RemoveRange(index, count);
			}
			else
            {
				_list = _list.RemoveRange(index, count);
			}
		}

		public override LogBean CreateLogBean() {
			var log = new LogList1<E>();
			log.Belong = Parent;
			log.This = this;
			log.VariableId = VariableId;
			log.Value = _list;
			return log;
		}

		public override void FollowerApply(Log _log)
		{
			var log = (LogList1<E>)_log;
			foreach (var opLog in log.OpLogs)
			{
				switch (opLog.op)
				{
					case LogList1<E>.OpLog.OP_MODIFY:
						_list = _list.SetItem(opLog.index, opLog.value);
						break;
					case LogList1<E>.OpLog.OP_ADD:
						_list = _list.Insert(opLog.index, opLog.value);
						break;
					case LogList1<E>.OpLog.OP_REMOVE:
						_list = _list.RemoveAt(opLog.index);
						break;
					case LogList1<E>.OpLog.OP_CLEAR:
						_list = ImmutableList<E>.Empty;
						break;
				}
			}
		}

		public override void LeaderApplyNoRecursive(Log _log)
		{
			_list = ((LogList1<E>)_log).Value;
		}

		protected override void InitChildrenRootInfo(Record.RootInfo root)
		{
		}

		public override Bean CopyBean()
		{
			var copy = new CollList1<E>();
			copy._list = _list;
			return copy;
		}
	}
}