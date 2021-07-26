using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace Zeze.Transaction
{
    public enum CheckpointMode
    {
        Period = 0,
        Immediately = 1,
        Table = 2,
    }
}
