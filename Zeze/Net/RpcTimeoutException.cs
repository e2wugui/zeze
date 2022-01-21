using System;

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
