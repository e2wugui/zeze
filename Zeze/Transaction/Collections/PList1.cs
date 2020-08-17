using System;
using System.Collections.Immutable;

namespace Zeze.Transaction.Collections
{
    public sealed class PList1<E> : PList<E>
    {
        public PList1(Func<ImmutableList<E>, Log> logFactory) : base(logFactory)
        {
        }

        public override E this[int index]
        {
            get => Data[index];
            set
            {
                if (value == null)
                {
                    throw new ArgumentNullException("cant add null element");
                }

                if (this.IsManaged)
                {
                    var txn = Transaction.Current;
                    var oldv = txn.GetLog(this) is LogV log ? log.Value : list;
                    txn.PutLog(this, NewLog(oldv.SetItem(index, value)));
                }
                else
                {
                    list = list.SetItem(index, value);
                }
            }
        }

        public override void Add(E item)
        {
            if (item == null)
            {
                throw new ArgumentException("cant add null element");
            }

            if (this.IsManaged)
            {
                var txn = Transaction.Current;
                var oldv = txn.GetLog(this) is LogV log ? log.Value : list;
                txn.PutLog(this, NewLog(oldv.Add(item)));
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
                var oldv = txn.GetLog(this) is LogV log ? log.Value : list;
                if (!oldv.IsEmpty)
                {
                    txn.PutLog(this, NewLog(ImmutableList<E>.Empty));
                }
            }
            else
            {
                list = ImmutableList<E>.Empty;
            }
        }


        public override void Insert(int index, E item)
        {
            if (this.IsManaged)
            {
                if (item == null)
                {
                    throw new ArgumentException("cant add null element");
                }

                var txn = Transaction.Current;
                var oldv = txn.GetLog(this) is LogV log ? log.Value : list;
                txn.PutLog(this, NewLog(oldv.Insert(index, item)));
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
                var oldv = txn.GetLog(this) is LogV log ? log.Value : list;
                var newv = oldv.Remove(item);
                if (oldv != newv)
                {
                    txn.PutLog(this, NewLog(newv));
                    return true;
                }
                else
                {
                    return false;
                }
            }
            else
            {
                var newv = list.Remove(item);
                if (newv != list)
                {
                    list = newv;
                    return true;
                }
                else
                {
                    return false;
                }
            }
        }

        public override void RemoveAt(int index)
        {
            if (this.IsManaged)
            {
                var txn = Transaction.Current;
                var oldv = txn.GetLog(this) is LogV log ? log.Value : list;
                txn.PutLog(this, NewLog(oldv.RemoveAt(index)));
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
                var oldv = txn.GetLog(this) is LogV log ? log.Value : list;
                txn.PutLog(this, NewLog(oldv.RemoveRange(index, count)));
            }
            else
            {
                list = list.RemoveRange(index, count);
            }
        }

        protected override void InitChildrenTableKey(TableKey root)
        {

        }
    }
}
