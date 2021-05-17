using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using Zeze.Net;

namespace Zeze.Raft
{
    public abstract class StateMachine
    {
        public Raft Raft { get; internal set; }

        public abstract Log LogFactory(int logTypeId);

        public abstract void Snapshot(AsyncSocket node);
    }
}
