using System;
using System.Collections.Generic;
using System.Text;

namespace Zeze.Transaction
{
    public abstract class Record
    {
        public long Timestamp { get; set; }
    }

    public class Record<K, V> : Record
    {
        public V Value { get; }
    }
}
