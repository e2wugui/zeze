using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using Zeze.Serialize;

namespace Zeze.Transaction.Collections
{
	public class CollSet1<V> : CollSet<V>
	{
		public override bool Add(V item)
        {
			if (item == null)
				throw new ArgumentNullException();

			if (IsManaged)
			{
				var txn = Transaction.Current;
				txn.VerifyRecordAccessed(this);
				var setlog = (LogSet1<V>)txn.LogGetOrAdd(Parent.ObjectId + VariableId, CreateLogBean);
				return setlog.Add(item);
			}
			else
			{
				var newset = _set.Add(item);
				if (newset == _set)
					return false;
				_set = newset;
				return true;
			}
		}

        public override void ClearParameters()
        {
			Clear();
        }
        
		public override bool Remove(V item)
		{
			if (IsManaged)
			{
				var txn = Transaction.Current;
				txn.VerifyRecordAccessed(this);
				var setlog = (LogSet1<V>)txn.LogGetOrAdd(Parent.ObjectId + VariableId, CreateLogBean);
				return setlog.Remove(item);
			}
			else
			{
				var newset = _set.Remove(item);
				if (newset == _set)
					return false;
				_set = newset;
				return true;
			}
		}

		public override void Clear()
		{
			if (IsManaged)
			{
				var txn = Transaction.Current;
				txn.VerifyRecordAccessed(this);
				var setlog = (LogSet1<V>)txn.LogGetOrAdd(Parent.ObjectId + VariableId, CreateLogBean);
				setlog.Clear();
			}
			else
			{
				_set = _set.Clear();
			}
		}

		public override LogBean CreateLogBean()
		{
            var log = new LogSet1<V>
            {
                Belong = Parent,
                This = this,
                VariableId = VariableId,
                Value = _set
            };
            return log;
		}

		public override void FollowerApply(Log _log)
		{
			var log = (LogSet1<V>)_log;
			var tmp = _set;
			tmp = tmp.Union(log.Added);
			tmp = tmp.Except(log.Removed);
			_set = tmp;
		}

        public override CollSet1<V> Copy()
		{
            var copy = new CollSet1<V>
            {
                _set = _set
            };
            return copy;
		}

        public override void ExceptWith(IEnumerable<V> other)
        {
			if (this.IsManaged)
			{
				var txn = Transaction.Current;
				txn.VerifyRecordAccessed(this);
				var log = (LogSet1<V>)txn.LogGetOrAdd(Parent.ObjectId + VariableId, CreateLogBean);
				log.ExceptWith(other);
			}
			else
			{
				_set = _set.Except(other);
			}
		}

		public override void IntersectWith(IEnumerable<V> other)
        {
			if (this.IsManaged)
			{
				var txn = Transaction.Current;
				txn.VerifyRecordAccessed(this);
				var log = (LogSet1<V>)txn.LogGetOrAdd(Parent.ObjectId + VariableId, CreateLogBean);
				log.IntersectWith(other);
			}
			else
			{
				_set = _set.Intersect(other);
			}
		}

		public override void SymmetricExceptWith(IEnumerable<V> other)
        {
			if (this.IsManaged)
			{
				var txn = Transaction.Current;
				txn.VerifyRecordAccessed(this);
				var log = (LogSet1<V>)txn.LogGetOrAdd(Parent.ObjectId + VariableId, CreateLogBean);
				log.SymmetricExceptWith(other);
			}
			else
			{
				_set = _set.SymmetricExcept(other);
			}
		}

		public override void UnionWith(IEnumerable<V> other)
        {
			foreach (var v in other)
			{
				if (null == v)
					throw new ArgumentNullException();
			}

			if (this.IsManaged)
			{
				var txn = Transaction.Current;
				txn.VerifyRecordAccessed(this);
				var log = (LogSet1<V>)txn.LogGetOrAdd(Parent.ObjectId + VariableId, CreateLogBean);
				log.UnionWith(other);
			}
			else
			{
				_set = _set.Union(other);
			}
		}
	}
}
