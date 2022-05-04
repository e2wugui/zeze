using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace Zeze.Arch
{
    public class LoginArgument : EventArgs
    {
        public string Account { get; set; }
        public string ClientId { get; set; }
    }
}
