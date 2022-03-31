using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace Zeze.Arch
{
    [System.AttributeUsage(System.AttributeTargets.Method)]
    public class RedirectAttribute : System.Attribute
    {
        public string ChoiceHashCodeSource { get; }
        public RedirectAttribute(string source = null)
        {
            ChoiceHashCodeSource = source;
        }
    }

}
