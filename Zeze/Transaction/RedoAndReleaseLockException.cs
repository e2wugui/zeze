using System;
using System.Collections.Generic;
using System.Text;

namespace Zeze.Transaction
{
    public sealed class RedoAndReleaseLockException : Exception
    {
        public TableKey TableKey { get; }

        internal RedoAndReleaseLockException(TableKey tkey, string msg)
            : base(msg)
        {
            TableKey = tkey;
        }
    }
}
