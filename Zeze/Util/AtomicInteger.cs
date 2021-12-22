using System;
using System.Collections.Generic;
using System.Text;

namespace Zeze.Util
{
    public sealed class AtomicInteger
    {
        private volatile int _value;

        public AtomicInteger(int initialValue = 0)
        {
            _value = initialValue;
        }

        public long IncrementAndGet()
        {
            return System.Threading.Interlocked.Increment(ref _value);
        }

        public int AddAndGet(int delta)
        {
            return System.Threading.Interlocked.Add(ref _value, delta);
        }

        public int CompareAndExchange(int expectedValue, int newValue)
        {
            return System.Threading.Interlocked.CompareExchange(ref _value, newValue, expectedValue);
        }

        public bool CompareAndSet(int expectedValue, int newValue)
        {
            return CompareAndExchange(expectedValue, newValue) == expectedValue;
        }

        public int Get()
        {
            return _value;
        }

        public int GetAndSet(int newValue)
        {
            return System.Threading.Interlocked.Exchange(ref _value, newValue);
        }
    }
}
