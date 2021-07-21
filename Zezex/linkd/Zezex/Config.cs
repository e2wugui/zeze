using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace Zezex
{
    public class Config
    {
        public int MaxOnlineNew { get; set; } = 30;
        // 大致的Linkd数量。在Provider报告期间，用来估算负载均衡。
        public int ApproximatelyLinkdCount { get; set; } = 4;
    }
}
