using System;
using System.Collections.Generic;
using System.Text;

namespace Zeze.Util
{
    public class AtomicBool
    {
        private int _value;

        public AtomicBool(bool initialValue = false)
        {
            _value = initialValue ? 1 : 0;
        }

        public bool CompareAndExchange(bool expectedValue, bool newValue)
        {
            return System.Threading.Interlocked.CompareExchange(ref _value, newValue ? 1 : 0, expectedValue ? 1 : 0) != 0;
        }

        public bool Get()
        {
            return _value != 0;
        }

        public bool GetAndSet(bool newValue)
        {
            return System.Threading.Interlocked.Exchange(ref _value, newValue ? 1 : 0) != 0;
        }
    }
}
