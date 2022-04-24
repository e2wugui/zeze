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
        private volatile List<T> read;
        private List<T> write = new();

        private List<T> PrepareRead()
        {
            var tmp = read;
            if (null != tmp)
                return tmp;

            lock (write)
            {
                if (null == read)
                {
                    read = tmp = new List<T>();
                    read.AddRange(write);
                }
                return tmp;
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
                lock (write)
                {
                    write[index] = value;
                    read = null;
                }
            }
        }

        public void Add(T item)
        {
            lock (write)
            {
                write.Add(item);
                read = null;
            }
        }

        public void Clear()
        {
            lock (write)
            {
                write.Clear();
                read = null;
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
            lock (write)
            {
                write.Insert(index, item);
                read = null;
            }
        }

        public bool Remove(T item)
        {
            lock (write)
            {
                if (write.Remove(item))
                {
                    read = null;
                    return true;
                }
                return false;
            }
        }

        public void RemoveAt(int index)
        {
            lock (write)
            {
                write.RemoveAt(index);
                read = null;
            }
        }

        IEnumerator IEnumerable.GetEnumerator()
        {
            return PrepareRead().GetEnumerator();
        }
    }
}
