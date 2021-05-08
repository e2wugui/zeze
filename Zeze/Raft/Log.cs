using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace Zeze.Raft
{
    public class Log
    {
        public ulong Term { get; }
        public ulong Index { get; }
    }

    public class Logs
    { 
    }
}
