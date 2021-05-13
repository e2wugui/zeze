using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace Zeze.Raft
{
    public abstract class StateMachine
    {
        public Raft Raft { get; internal set; }

        public abstract void Snapshot();
    }
}
