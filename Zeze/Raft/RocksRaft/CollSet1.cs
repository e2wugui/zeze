using System;
using System.Collections.Generic;
using System.Collections.Immutable;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using Zeze.Serialize;

namespace Zeze.Raft.RocksRaft
{
	public class CollSet1<V> : CollSet<V>
	{
		public override bool Add(V item)
        {
			if (IsManaged)
			{
				var setlog = (LogSet1<V>)Transaction.Current.LogGetOrAdd(Parent.ObjectId + VariableId, CreateLogBean);
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

		public override bool Remove(V item)
		{
			if (IsManaged)
			{
				var setlog = (LogSet1<V>)Transaction.Current.LogGetOrAdd(Parent.ObjectId + VariableId, CreateLogBean);
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
				var setlog = (LogSet1<V>)Transaction.Current.LogGetOrAdd(Parent.ObjectId + VariableId, CreateLogBean);
				setlog.Clear();
			}
			else
			{
				_set = _set.Clear();
			}
		}

		public override LogBean CreateLogBean()
		{
			var log = new LogSet1<V>();
			log.Belong = Parent;
			log.This = this;
			log.VariableId = VariableId;
			log.Value = _set;
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

		public override void LeaderApplyNoRecursive(Log _log)
		{
			var log = (LogSet1<V>)_log;
			_set = log.Value;
		}

        protected override void InitChildrenRootInfo(Record.RootInfo root)
        {
        }

		public override Bean CopyBean()
		{
			var copy = new CollSet1<V>();
			copy._set = _set;
			return copy;
		}
	}
}
