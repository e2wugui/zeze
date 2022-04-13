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
        public Func<long, int, Binary, Binary> RequestHandle { get; set; }
        public TransactionLevel RequestTransactionLevel { get; set; } = TransactionLevel.Serializable;
    }
}
