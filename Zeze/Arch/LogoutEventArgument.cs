using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using Zeze.Builtin.Online;

namespace Zeze.Arch
{
    public class LogoutEventArgument : EventArgs
    {
        public string Account { get; set; }
        public string ClientId { get; set; }
        public BOnline OnlineData { get; set; }
    }
}
