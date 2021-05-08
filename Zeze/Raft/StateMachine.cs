using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace Zeze.Raft
{
    public abstract class StateMachine
    {
        public abstract void Snapshot();
        public abstract void ApplyLog(Log log);
    }
}
