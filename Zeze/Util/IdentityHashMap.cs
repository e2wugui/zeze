using System.Collections.Concurrent;
using System.Collections.Generic;
using System.Runtime.CompilerServices;

namespace Zeze.Util
{
    public sealed class IdentityHashMap<K, V> : ConcurrentDictionary<K, V>
    {
        private class IdentityEqualityComparer : IEqualityComparer<K>
        {
            bool IEqualityComparer<K>.Equals(K x, K y)
            {
                return object.ReferenceEquals(x, y);
            }

            int IEqualityComparer<K>.GetHashCode(K obj)
            {
                return RuntimeHelpers.GetHashCode(obj);
            }
        }
        private static readonly IdentityEqualityComparer comparer = new IdentityEqualityComparer();

        public IdentityHashMap() : base(comparer)
        { 
        }
    }
}
