using System;
using System.Collections.Generic;
using System.Text;

namespace Zeze.Transaction
{
    public sealed class RedoAndReleaseLockException : Exception
    {
        public TableKey TableKey { get; }
        public long GlobalSerialId { get; }

        internal RedoAndReleaseLockException(TableKey tkey, long serialId, string msg)
            : base(msg)
        {
            TableKey = tkey;
            GlobalSerialId = serialId;
        }
    }
}
