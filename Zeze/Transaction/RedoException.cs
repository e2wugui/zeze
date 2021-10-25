using System;
using System.Collections.Generic;
using System.Text;

namespace Zeze.Transaction
{
    public sealed class RedoException : Exception
    {
        internal RedoException()
        { 
        }

        internal RedoException(string msg) : base(msg)
        {

        }
    }
}
