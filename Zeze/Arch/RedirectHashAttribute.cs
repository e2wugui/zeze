using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace Zeze.Arch
{
    [System.AttributeUsage(System.AttributeTargets.Method)]
    public class RedirectHashAttribute : System.Attribute
    {
        public string GetConcurrentLevelSource { get; }

        public RedirectHashAttribute(string source = null)
        {
            GetConcurrentLevelSource = source;
        }
    }

}
