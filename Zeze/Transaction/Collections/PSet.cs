using System;
using System.Collections;
using System.Collections.Generic;
using System.Collections.Immutable;

namespace Zeze.Transaction.Collections
{
    public abstract class PSet<E> : PCollection, ISet<E>
    {
        private readonly Func<ImmutableHashSet<E>, Log> _logFactory;

        protected ImmutableHashSet<E> set;

        protected PSet(long logKey, Func<ImmutableHashSet<E>, Log> logFactory) : base(logKey)
        {
            this._logFactory = logFactory;
            set = ImmutableHashSet<E>.Empty;
        }

        public Log NewLog(ImmutableHashSet<E> value)
        {
            return _logFactory(value);
        }

        public abstract class LogV : Log
        {
            internal ImmutableHashSet<E> Value;

            protected LogV(Bean bean, ImmutableHashSet<E> value) : base(bean)
            {
                Value = value;
            }

            protected void Commit(PSet<E> variable)
            {
                variable.set = Value;
            }
        }

        protected ImmutableHashSet<E> Data
        {
            get
            {
                if (this.IsManaged)
                {
                    var txn = Transaction.Current;
                    if (txn == null)
                    {
                        return set;
                    }
                    txn.VerifyRecordAccessed(this, true);
                    return txn.GetLog(LogKey) is LogV log ? log.Value : set;
                }
                else
                {
                    return set;
                }
            }
        }

        public override string ToString()
        {
            return $"PSet{Data}";
        }

        public int Count => Data.Count;
        public bool IsReadOnly => false;

        public void AddAll(IEnumerable<E> items)
        {
            foreach (var item in items)
                Add(item);
        }

        public abstract bool Add(E item);
        public abstract void Clear();
        public abstract void ExceptWith(IEnumerable<E> other);
        public abstract void IntersectWith(IEnumerable<E> other);
        public abstract bool Remove(E item);
        public abstract void SymmetricExceptWith(IEnumerable<E> other);
        public abstract void UnionWith(IEnumerable<E> other);

        void ICollection<E>.Add(E item)
        {
            this.Add(item);
        }

        public bool Contains(E item)
        {
            return Data.Contains(item);
        }

        public void CopyTo(E[] array, int arrayIndex)
        {
            int index = arrayIndex;
            foreach (var e in Data)
            {
                array[index++] = e;
            }
        }

        public bool IsProperSubsetOf(IEnumerable<E> other)
        {
            return Data.IsProperSubsetOf(other);
        }

        public bool IsProperSupersetOf(IEnumerable<E> other)
        {
            return Data.IsProperSupersetOf(other);
        }

        public bool IsSubsetOf(IEnumerable<E> other)
        {
            return Data.IsSubsetOf(other);
        }

        public bool IsSupersetOf(IEnumerable<E> other)
        {
            return Data.IsSupersetOf(other);
        }

        public bool SetEquals(IEnumerable<E> other)
        {
            return Data.SetEquals(other);
        }

        public bool Overlaps(IEnumerable<E> other)
        {
            return Data.Overlaps(other);
        }

        IEnumerator IEnumerable.GetEnumerator()
        {
            return Data.GetEnumerator();
        }
        IEnumerator<E> IEnumerable<E>.GetEnumerator()
        {
            return Data.GetEnumerator();
        }

        public ImmutableHashSet<E>.Enumerator GetEnumerator()
        {
            return Data.GetEnumerator();
        }
    }
}
