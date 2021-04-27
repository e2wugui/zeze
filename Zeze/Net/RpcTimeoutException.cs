using System;
using System.Collections.Generic;
using System.Text;

namespace Zeze.Net
{
    public sealed class RpcTimeoutException : Exception
    {
        public RpcTimeoutException()
        {

        }

        public RpcTimeoutException(string str) : base(str)
        {

        }
    }
}
