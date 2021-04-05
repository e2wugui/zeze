using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace Game
{
    public class Config
    {
        public int MaxOnlineNew { get; set; } = 30;
        public int ReportDelaySeconds { get; set; } = 10;
        public int ProposeMaxOnline { get; set; } = 15000;
    }
}
