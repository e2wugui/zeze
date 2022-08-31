using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace Zeze
{
    public abstract class AppBase
    {
        public virtual Application Zeze { get; set; }

        public virtual T ReplaceModuleInstance<T>(T input)
            where T : IModule
        {
            return input;
        }
    }
}
