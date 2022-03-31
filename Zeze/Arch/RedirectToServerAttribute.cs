using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace Zeze.Arch
{

    [System.AttributeUsage(System.AttributeTargets.Method)]
    public class RedirectToServerAttribute : System.Attribute
    {
        public RedirectToServerAttribute()
        {

        }
    }
}
