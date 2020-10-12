using System;
using System.Collections.Generic;
using System.Text;

namespace Zeze.Util
{
    public sealed class AtomicLong
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

        public long CompareAndExchange(long expectedValue, long newValue)
        {
            return System.Threading.Interlocked.CompareExchange(ref _value, newValue, expectedValue);
        }

        public long Get()
        {
            return System.Threading.Interlocked.Read(ref _value);
        }

        public long GetAndSet(long newValue)
        {
            return System.Threading.Interlocked.Exchange(ref _value, newValue);
        }
    }
}
