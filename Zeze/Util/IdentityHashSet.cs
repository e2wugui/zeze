using System;
using System.Collections;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace Zeze.Util
{
    internal class IdentityHashSet<E> : IEnumerable<E>
    {
        private IdentityHashMap<E, E> Impl = new IdentityHashMap<E, E>();

        public bool Add(E e)
        {
            return Impl.TryAdd(e, e);
        }

        public int Count => Impl.Count;

        public void Clear()
        {
            Impl.Clear();
        }

        public bool Remove(E e)
        {
            if (Impl.TryRemove(e, out var rm))
                return true;
            return false;
        }

        public IEnumerator<E> GetEnumerator()
        {
            return Impl.Keys.GetEnumerator();
        }

        IEnumerator IEnumerable.GetEnumerator()
        {
            return GetEnumerator();
        }
    }
}
