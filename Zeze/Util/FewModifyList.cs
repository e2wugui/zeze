using System;
using System.Collections;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace Zeze.Util
{
    public class FewModifyList<T> : IList<T>, IEnumerable<T>, IEnumerable, IReadOnlyCollection<T>, IReadOnlyList<T>
    {
        private volatile List<T> ListRead;
        private List<T> ListWrite = new();

        private List<T> PrepareRead()
        {
            if (null != ListRead)
                return ListRead;

            lock (ListWrite)
            {
                if (null == ListRead)
                {
                    ListRead = new List<T>();
                    ListRead.AddRange(ListWrite);
                }
                return ListRead;
            }
        }
        public T this[int index] => PrepareRead()[index];

        public int Count => PrepareRead().Count;

        public bool IsReadOnly => false;

        T IList<T>.this[int index]
        { 
            get => PrepareRead()[index];

            set
            {
                lock (ListWrite)
                {
                    ListWrite[index] = value;
                    ListRead = null;
                }
            }
        }

        public void Add(T item)
        {
            lock (ListWrite)
            {
                ListWrite.Add(item);
                ListRead = null;
            }
        }

        public void Clear()
        {
            lock (ListWrite)
            {
                ListWrite.Clear();
                ListRead = null;
            }
        }

        public bool Contains(T item)
        {
            return PrepareRead().Contains(item);
        }

        public void CopyTo(T[] array, int arrayIndex)
        {
            PrepareRead().CopyTo(array, arrayIndex);
        }

        public IEnumerator<T> GetEnumerator()
        {
            return PrepareRead().GetEnumerator();
        }

        public int IndexOf(T item)
        {
            return PrepareRead().IndexOf(item);
        }

        public void Insert(int index, T item)
        {
            lock (ListWrite)
            {
                ListWrite.Insert(index, item);
                ListRead = null;
            }
        }

        public bool Remove(T item)
        {
            lock (ListWrite)
            {
                var r = ListWrite.Remove(item);
                ListRead = null;
                return r;
            }
        }

        public void RemoveAt(int index)
        {
            lock (ListWrite)
            {
                ListWrite.RemoveAt(index);
                ListRead = null;
            }
        }

        IEnumerator IEnumerable.GetEnumerator()
        {
            return PrepareRead().GetEnumerator();
        }
    }
}
