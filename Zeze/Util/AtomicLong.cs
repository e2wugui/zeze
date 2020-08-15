using System;
using System.Collections.Generic;
using System.Text;

namespace Zeze.Util
{
    public class AtomicLong
    {
        private long _value;

        public AtomicLong(long initialValue = 0)
        {
            _value = initialValue;
        }

        public long IncrementAndGet()
        {
            return System.Threading.Interlocked.Increment(ref _value);
        }

        public long AddAndGet(long delta)
        {
            return System.Threading.Interlocked.Add(ref _value, delta);
        }
    }
}
