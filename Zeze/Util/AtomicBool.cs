using System;
using System.Collections.Generic;
using System.Text;

namespace Zeze.Util
{
    public sealed class AtomicBool
    {
        private int _value;

        public AtomicBool(bool initialValue = false)
        {
            _value = initialValue ? 1 : 0;
        }

        public bool CompareAndExchange(bool expectedValue, bool newValue)
        {
            int n = newValue ? 1 : 0;
            int e = expectedValue ? 1 : 0;
            int r = System.Threading.Interlocked.CompareExchange(ref _value, n, e);
            return r != 0;
        }

        public bool Get()
        {
            return _value != 0;
        }

        /// <summary>
        /// ??? 对于 bool 来说，和 CompareAndExchange 差不多 ???
        /// </summary>
        /// <param name="newValue"></param>
        /// <returns></returns>
        public bool GetAndSet(bool newValue)
        {
            return System.Threading.Interlocked.Exchange(ref _value, newValue ? 1 : 0) != 0;
        }
    }
}
