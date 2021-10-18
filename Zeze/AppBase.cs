using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace Zeze
{
    public abstract class AppBase
    {
        public virtual Zeze.IModule ReplaceModuleInstance(Zeze.IModule input)
        {
            return input;
        }
    }
}
