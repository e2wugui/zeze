using System;
using System.Collections.Generic;
using System.Text;

namespace Zeze.Transaction
{
    public sealed class RedoAndReleaseLockException : Exception
    {
        internal RedoAndReleaseLockException()
        { 
        }

        internal RedoAndReleaseLockException(string msg) : base(msg)
        {

        }
    }
}
