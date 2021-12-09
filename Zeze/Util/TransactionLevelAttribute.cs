
using System;

namespace Zeze.Util
{
    [AttributeUsage(AttributeTargets.Method)]
    public class TransactionLevelAttribute : Attribute
    {
        public string Level { get; set; } = "Serializable";
        
        public TransactionLevelAttribute()
        {

        }
    }
}
