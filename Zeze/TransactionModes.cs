using System;
using System.Collections.Generic;
using System.Text;

namespace Zeze
{
    public enum TransactionModes
    {
        ExecuteInTheCallerTransaction,
        ExecuteInNestedCall,
        ExecuteInAnotherThread,
    }
}
