
using System;
using System.Collections.Generic;

namespace Zeze.Transaction.Collections
{
	public class CollList1<E> : CollList<E>
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
					var txn = Transaction.Current;
					txn.VerifyRecordAccessed(this);
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
				var txn = Transaction.Current;
				txn.VerifyRecordAccessed(this);
				var listLog = (LogList1<E>)txn.LogGetOrAdd(Parent.ObjectId + VariableId, CreateLogBean);
				listLog.Add(item);
			}
			else
            {
				_list = _list.Add(item);
			}
		}

		public override void AddRange(IEnumerable<E> items)
		{
			// XXX
			foreach (var v in items)
			{
				if (null == v)
					throw new ArgumentNullException();
			}
			if (IsManaged)
			{
				var txn = Transaction.Current;
				txn.VerifyRecordAccessed(this);
				var listLog = (LogList1<E>)txn.LogGetOrAdd(Parent.ObjectId + VariableId, CreateLogBean);
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
				var txn = Transaction.Current;
				txn.VerifyRecordAccessed(this);
				var listLog = (LogList1<E>)txn.LogGetOrAdd(Parent.ObjectId + VariableId, CreateLogBean);
				listLog.Clear();
			}
			else
            {
				_list = System.Collections.Immutable.ImmutableList<E>.Empty;
			}
		}

		public override void Insert(int index, E item)
		{
			if (item == null)
				throw new ArgumentNullException();

			if (IsManaged)
			{
				var txn = Transaction.Current;
				txn.VerifyRecordAccessed(this);
				var listLog = (LogList1<E>)txn.LogGetOrAdd(Parent.ObjectId + VariableId, CreateLogBean);
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
				var txn = Transaction.Current;
				txn.VerifyRecordAccessed(this);
				var listLog = (LogList1<E>)txn.LogGetOrAdd(Parent.ObjectId + VariableId, CreateLogBean);
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
				var txn = Transaction.Current;
				txn.VerifyRecordAccessed(this);
				var listLog = (LogList1<E>)txn.LogGetOrAdd(Parent.ObjectId + VariableId, CreateLogBean);
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
				var txn = Transaction.Current;
				txn.VerifyRecordAccessed(this);
				var listLog = (LogList1<E>)txn.LogGetOrAdd(Parent.ObjectId + VariableId, CreateLogBean);
				listLog.RemoveRange(index, count);
			}
			else
            {
				_list = _list.RemoveRange(index, count);
			}
		}

		public override LogBean CreateLogBean() {
            var log = new LogList1<E>
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
						_list = System.Collections.Immutable.ImmutableList<E>.Empty;
						break;
				}
			}
		}

		protected override void InitChildrenRootInfo(Record.RootInfo root)
		{
		}

		protected override void ResetChildrenRootInfo()
		{
		}

		public override CollList1<E> Copy()
		{
            var copy = new CollList1<E>
            {
                _list = _list
            };
            return copy;
		}
	}
}