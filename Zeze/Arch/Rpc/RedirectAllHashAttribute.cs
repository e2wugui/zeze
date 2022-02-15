using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace Zeze.Arch.Rpc
{
    [System.AttributeUsage(System.AttributeTargets.Method)]
    public class RedirectAllHashAttribute : System.Attribute
    {
        public string GetConcurrentLevelSource { get; }

        public RedirectAllHashAttribute(string source)
        {
            GetConcurrentLevelSource = source;
        }
    }
}
