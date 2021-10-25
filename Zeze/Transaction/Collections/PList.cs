using System;
using System.Collections;
using System.Collections.Generic;
using System.Collections.Immutable;

namespace Zeze.Transaction.Collections
{
    public abstract class PList<E> : PCollection, IList<E>, IReadOnlyList<E>
    {
        private readonly Func<ImmutableList<E>, Log> _logFactory;

        protected ImmutableList<E> list;

        protected PList(long logKey, Func<ImmutableList<E>, Log> logFactory) : base(logKey)
        {
            this._logFactory = logFactory;
            list = ImmutableList<E>.Empty;
        }

        public Log NewLog(ImmutableList<E> value)
        {
            return _logFactory(value);
        }

        public abstract class LogV : Log
        {
            internal ImmutableList<E> Value;

            protected LogV(Bean bean, ImmutableList<E> last) : base(bean)
            {
                this.Value = last;
            }

            protected void Commit(PList<E> variable)
            {
                variable.list = Value;
            }
        }

        protected ImmutableList<E> Data
        {
            get
            {
                if (this.IsManaged)
                {
                    var txn = Transaction.Current;
                    if (txn == null)
                    {
                        return list;
                    }
                    txn.VerifyRecordAccessed(this, true);
                    var log = (LogV)txn.GetLog(LogKey);
                    return null != log ? log.Value : list;
                }
                return list;
            }
        }
        public int Count => Data.Count;

        public override string ToString()
        {
            return $"PList{Data}";
        }

        public abstract E this[int index] { get; set; }

        public bool IsReadOnly => false;

        public abstract void Add(E item);
        public abstract void AddRange(IEnumerable<E> items);
        public abstract void Clear();
        public abstract void Insert(int index, E item);
        public abstract bool Remove(E item);
        public abstract void RemoveAt(int index);
        public abstract void RemoveRange(int index, int count);

        public bool Contains(E item)
        {
            return Data.Contains(item);
        }

        public void CopyTo(E[] array, int arrayIndex)
        {
            Data.CopyTo(array, arrayIndex);
        }


        IEnumerator IEnumerable.GetEnumerator()
        {
            return Data.GetEnumerator();
        }

        IEnumerator<E> IEnumerable<E>.GetEnumerator()
        {
            return Data.GetEnumerator();
        }

        public ImmutableList<E>.Enumerator GetEnumerator()
        {
            return Data.GetEnumerator();
        }

        public int IndexOf(E item)
        {
            return Data.IndexOf(item);
        }
    }
}
