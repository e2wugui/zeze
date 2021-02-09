using System;
using System.Collections.Generic;
using System.Text;

namespace Zeze.Transaction
{
    public sealed class AbortException : Exception
    {
        public AbortException()
        {
        }

        public AbortException(string msg) : base(msg)
        {

        }
    }
}
