using System.Threading;

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
            return Interlocked.Increment(ref _value);
        }

        public long DecrementAndGet()
        {
            return Interlocked.Decrement(ref _value);
        }

        public long AddAndGet(long delta)
        {
            return Interlocked.Add(ref _value, delta);
        }

        public long CompareAndExchange(long expectedValue, long newValue)
        {
            return Interlocked.CompareExchange(ref _value, newValue, expectedValue);
        }

        public bool CompareAndSet(long expectedValue, long newValue)
        {
            return CompareAndExchange(expectedValue, newValue) == expectedValue;
        }

        public long Get()
        {
            return Interlocked.Read(ref _value);
        }

        public long GetAndSet(long newValue)
        {
            return Interlocked.Exchange(ref _value, newValue);
        }

        public override string ToString()
        {
            return Get().ToString();
        }
    }
}
