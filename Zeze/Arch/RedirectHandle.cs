using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using Zeze.Net;
using Zeze.Transaction;

namespace Zeze.Arch
{
    public class RedirectHandle
    {
        public Func<long, int, Binary, Task<Binary>> RequestHandle { get; set; } // 肯定是异步的，即使实际实现方法是同步的。
        public TransactionLevel RequestTransactionLevel { get; set; } = TransactionLevel.Serializable;
    }
}
