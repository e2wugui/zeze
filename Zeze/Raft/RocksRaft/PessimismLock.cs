using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace Zeze.Raft.RocksRaft
{
    public interface PessimismLock
    {
        public void Lock();
        public void Unlock();
    }
}
