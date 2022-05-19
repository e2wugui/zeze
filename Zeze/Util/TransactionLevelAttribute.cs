using System;
using Zeze.Transaction;

namespace Zeze.Util
{
    [AttributeUsage(AttributeTargets.Method)]
    public class TransactionLevelAttribute : Attribute
    {
        public TransactionLevel Level { get; set; } = TransactionLevel.Serializable;
    }
}
