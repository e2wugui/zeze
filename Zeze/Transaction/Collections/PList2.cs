using System;
using System.Collections.Immutable;
using System.Diagnostics;

namespace Zeze.Transaction.Collections
{
    public sealed class PList2<E> : PList<E> where E : Bean
    {
        public PList2(Func<ImmutableList<E>, Log> logFactory) : base(logFactory)
        {
        }

        public override E this[int index]
        {
            get => Data[index];
            set
            {
                if (value == null)
                {
                    throw new ArgumentException("cant add null element");
                }

                if (this.IsManaged)
                {
                    var txn = Transaction.Current;
                    var oldv = txn.GetField(this) is LogV log ? log.Value : list;
                    txn.PutField(this, NewLog(oldv.SetItem(index, value)));
                    value.InitTableKey(TableKey);
                }
                else
                {
                    list = list.SetItem(index, value);
                }
            }
        }

        public override void Add(E item)
        {
            if (this.IsManaged)
            {
                var txn = Transaction.Current;
                var oldv = txn.GetField(this) is LogV log ? log.Value : list;
                txn.PutField(this, NewLog(oldv.Add(item)));
                item.InitTableKey(TableKey);
            }
            else
            {
                list = list.Add(item);
            }
        }

        public override void Clear()
        {
            if (this.IsManaged)
            {
                var txn = Transaction.Current;
                var oldv = txn.GetField(this) is LogV log ? log.Value : list;
                if (!oldv.IsEmpty)
                {
                    txn.PutField(this, NewLog(ImmutableList<E>.Empty));
                }
            }
            else
            {
                this.list = ImmutableList<E>.Empty;
            }
        }

        public override void Insert(int index, E item)
        {
            if (this.IsManaged)
            {
                var txn = Transaction.Current;
                var oldv = txn.GetField(this) is LogV log ? log.Value : list;
                txn.PutField(this, NewLog(oldv.Insert(index, item)));
                item.InitTableKey(TableKey);
            }
            else
            {
                list = list.Insert(index, item);
            }
        }

        public override bool Remove(E item)
        {
            if (this.IsManaged)
            {
                var txn = Transaction.Current;
                var oldv = txn.GetField(this) is LogV log ? log.Value : list;
                var newv = oldv.Remove(item);
                if (oldv != newv)
                {
                    txn.PutField(this, NewLog(newv));
                    return true;
                }
                else
                {
                    return false;
                }
            }
            else
            {
                var oldv = list;
                list = list.Remove(item);
                return oldv != list;
            }
        }

        public override void RemoveAt(int index)
        {
            if (this.IsManaged)
            {
                var txn = Transaction.Current;
                var oldv = txn.GetField(this) is LogV log ? log.Value : list;
                txn.PutField(this, NewLog(oldv.RemoveAt(index)));
            }
            else
            {
                list = list.RemoveAt(index);
            }
        }

        public override void RemoveRange(int index, int count)
        {
            if (this.IsManaged)
            {
                var txn = Transaction.Current;
                var oldv = txn.GetField(this) is LogV log ? log.Value : list;
                txn.PutField(this, NewLog(oldv.RemoveRange(index, count)));
            }
            else
            {
                list = list.RemoveRange(index, count);
            }
        }

        protected override void InitChildrenTableKey(TableKey tableKey)
        {
            foreach (var e in list)
            {
                e.InitTableKey(tableKey);
            }
        }
    }
}
