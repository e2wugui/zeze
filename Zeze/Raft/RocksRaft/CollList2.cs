using System;
using System.Collections.Generic;
using System.Collections.Immutable;

namespace Zeze.Raft.RocksRaft
{

	public class CollList2<E> : CollList<E>
			where E : Bean, new()
	{
		public override E this[int index]
		{
			get => Data[index];
			set
			{
				if (value == null)
                    throw new ArgumentNullException(nameof(value));

				if (this.IsManaged)
				{
					value.InitRootInfo(RootInfo, this);
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
				item.InitRootInfo(RootInfo, this);
				var listLog = (LogList2<E>)Transaction.Current.LogGetOrAdd(Parent.ObjectId + VariableId, CreateLogBean);
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
				foreach (var item in items)
					item.InitRootInfo(RootInfo, this);
				var listLog = (LogList2<E>)Transaction.Current.LogGetOrAdd(Parent.ObjectId + VariableId, CreateLogBean);
				listLog.AddRange(items);
			}
			else
			{
				_list = _list.AddRange(items);
			}
		}

		public override bool Remove(E item)
		{
			if (IsManaged)
			{
				var listLog = (LogList2<E>)Transaction.Current.LogGetOrAdd(Parent.ObjectId + VariableId, CreateLogBean);
				return listLog.Remove(item);
			}
			else
			{
				var newList = _list.Remove(item);
				if (newList == _list)
					return false;
				_list = newList;
				return true;
			}
		}

		public override void RemoveRange(int index, int count)
		{
			if (IsManaged)
			{
				var listLog = (LogList2<E>)Transaction.Current.LogGetOrAdd(Parent.ObjectId + VariableId, CreateLogBean);
				listLog.RemoveRange(index, count);
			}
			else
			{
				_list = _list.RemoveRange(index, count);
			}
		}

		public override void Clear()
		{
			if (IsManaged)
			{
				var listLog = (LogList2<E>)Transaction.Current.LogGetOrAdd(Parent.ObjectId + VariableId, CreateLogBean);
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
				item.InitRootInfo(RootInfo, this);
				var listLog = (LogList2<E>)Transaction.Current.LogGetOrAdd(Parent.ObjectId + VariableId, CreateLogBean);
				listLog.Insert(index, item);
			}
			else
			{
				_list = _list.Insert(index, item);
			}
		}

		public override void RemoveAt(int index)
		{
			if (IsManaged)
			{
				var listLog = (LogList2<E>)Transaction.Current.LogGetOrAdd(Parent.ObjectId + VariableId, CreateLogBean);
				listLog.RemoveAt(index);
			}
			else
			{
				_list = _list.RemoveAt(index);
			}
		}

		public override LogBean CreateLogBean()
		{
            var log = new LogList2<E>
            {
                Belong = Parent,
                This = this,
                VariableId = VariableId,
                Value = _list
            };
            return log;
		}

		public override void FollowerApply(Log _log)
		{
			var log = (LogList2<E>)_log;
			var tmp = _list;
			var newest = new HashSet<int>();
			foreach (var opLog in log.OpLogs)
			{
				switch (opLog.op)
				{
					case LogList1<E>.OpLog.OP_MODIFY:
						opLog.value.InitRootInfo(RootInfo, this);
						tmp = tmp.SetItem(opLog.index, opLog.value);
						newest.Add(opLog.index);
						break;
					case LogList1<E>.OpLog.OP_ADD:
						opLog.value.InitRootInfo(RootInfo, this);
						tmp = tmp.Insert(opLog.index, opLog.value);
						newest.Add(opLog.index);
						break;
					case LogList1<E>.OpLog.OP_REMOVE:
						tmp = tmp.RemoveAt(opLog.index);
						break;
					case LogList1<E>.OpLog.OP_CLEAR:
						tmp = ImmutableList<E>.Empty;
						break;
				}
			}
			_list = tmp;

			// apply changed
			foreach (var e in log.Changed)
			{
				if (newest.Contains(e.Value.Value))
					continue;
				_list[e.Value.Value].FollowerApply(e.Key);
			}
		}

		public override void LeaderApplyNoRecursive(Log _log)
		{
			_list = ((LogList2<E>)_log).Value;
		}

		protected override void InitChildrenRootInfo(Record.RootInfo root)
		{
			foreach (var v in _list)
				v.InitRootInfo(root, this);
		}

		public override CollList2<E> Copy()
		{
            var copy = new CollList2<E>
            {
                _list = _list
            };
            return copy;
		}
	}
}
