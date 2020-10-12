using System;
using System.Collections;
using System.Collections.Generic;
using System.Runtime.CompilerServices;
using System.Text;

namespace Zeze.Util
{
    public sealed class IdentityHashMap<K, V> : Dictionary<K, V>
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
        private static IdentityEqualityComparer comparer = new IdentityEqualityComparer();

        public IdentityHashMap() : base(comparer)
        { 
        }
    }
}
