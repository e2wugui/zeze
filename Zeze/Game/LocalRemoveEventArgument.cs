using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using Zeze.Builtin.Game.Online;

namespace Zeze.Game
{
    public class LocalRemoveEventArgument : EventArgs
    {
        public long RoleId { get; set; }
        public BLocal LocalData { get; set; }
    }
}
