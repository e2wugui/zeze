using System;
using System.Collections.Generic;
using System.Collections.Immutable;

namespace Zeze.Transaction.Collections
{
    public sealed class PSet1<E> : PSet<E>
    {
        public PSet1(long logKey, Func<ImmutableHashSet<E>, Log> logFactory) : base(logKey, logFactory)
        {
        }

        public override bool Add(E item)
        {
            if (item == null)
                throw new ArgumentNullException();

            if (this.IsManaged)
            {
                var txn = Transaction.Current;
                var oldv = txn.GetLog(LogKey) is LogV log ? log.Value : set;
                var newv = oldv.Add(item);
                if (newv != oldv)
                {
                    txn.PutLog(NewLog(newv));
                    return true;
                }
                else
                {
                    return false;
                }
            }
            else
            {
                set = set.Add(item);
                return true;
            }
        }

        public override void Clear()
        {
            if (this.IsManaged)
            {
                var txn = Transaction.Current;
                var oldv = txn.GetLog(LogKey) is LogV log ? log.Value : set;
                if (!oldv.IsEmpty)
                {
                    txn.PutLog(NewLog(ImmutableHashSet<E>.Empty));
                }
            }
            else
            {
                set = ImmutableHashSet<E>.Empty;
            }
        }



        public override void ExceptWith(IEnumerable<E> other)
        {
            if (this.IsManaged)
            {
                var txn = Transaction.Current;
                var oldv = txn.GetLog(LogKey) is LogV log ? log.Value : set;
                var newv = oldv.Except(other);
                if (newv != oldv)
                {
                    txn.PutLog(NewLog(newv));
                }
            }
            else
            {
                set = set.Except(other);
            }
        }


        public override void IntersectWith(IEnumerable<E> other)
        {
            if (this.IsManaged)
            {
                var txn = Transaction.Current;
                var oldv = txn.GetLog(LogKey) is LogV log ? log.Value : set;
                var newv = oldv.Intersect(other);
                if (newv != oldv)
                {
                    txn.PutLog(NewLog(newv));
                }
            }
            else
            {
                set = set.Intersect(other);
            }
        }

        public override bool Remove(E item)
        {
            if (this.IsManaged)
            {
                var txn = Transaction.Current;
                var oldv = txn.GetLog(LogKey) is LogV log ? log.Value : set;
                var newv = oldv.Remove(item);
                if (newv != oldv)
                {
                    txn.PutLog(NewLog(newv));
                    return true;
                }
                else
                {
                    return false;
                }
            }
            else
            {
                var old = set;
                set = set.Remove(item);
                return old != set;
            }
        }

        public override void SymmetricExceptWith(IEnumerable<E> other)
        {
            if (this.IsManaged)
            {
                var txn = Transaction.Current;
                var oldv = txn.GetLog(LogKey) is LogV log ? log.Value : set;
                var newv = oldv.SymmetricExcept(other);
                if (newv != oldv)
                {
                    txn.PutLog(NewLog(newv));
                }
            }
            else
            {
                set = set.SymmetricExcept(other);
            }
        }

        public override void UnionWith(IEnumerable<E> other)
        {
            foreach (E v in other)
            {
                if (null == v)
                    throw new ArgumentNullException();
            }

            if (this.IsManaged)
            {
                var txn = Transaction.Current;
                var oldv = txn.GetLog(LogKey) is LogV log ? log.Value : set;
                var newv = oldv.Union(other);
                if (newv != oldv)
                {
                    txn.PutLog(NewLog(newv));
                }
            }
            else
            {
                set = set.Union(other);
            }
        }

        protected override void InitChildrenTableKey(TableKey tableKey)
        {
        }
    }
}
