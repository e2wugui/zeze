using System;
using System.Collections.Generic;
using System.Linq;
using System.Reflection;
using System.Text;
using System.Threading.Tasks;
using Zeze.Transaction;

namespace Zeze.Util
{
    public class Reflect
    {
        public Dictionary<string, MethodInfo> Methods { get; } = new Dictionary<string, MethodInfo>();

        public Reflect(Type type)
        {
            foreach (var method in type.GetMethods(BindingFlags.Instance | BindingFlags.NonPublic | BindingFlags.Public))
            {
                Methods.Add(method.Name, method);
            }
        }

        public TransactionLevel GetTransactionLevel(string methodName, TransactionLevel def)
        {
            if (Methods.TryGetValue(methodName, out var method))
            {
                var attr = method.GetCustomAttribute<TransactionLevelAttribute>();
                if (attr != null)
                    return (TransactionLevel)TransactionLevel.Parse(typeof(TransactionLevel), attr.Level);
                // else def
            }
            return def;
        }
    }
}
