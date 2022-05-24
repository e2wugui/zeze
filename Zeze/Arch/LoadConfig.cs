using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Text.Json;
using System.Threading.Tasks;

namespace Zeze.Arch
{
    public class LoadConfig
    {
        public int MaxOnlineNew { get; set; } = 30;
        // 大致的Linkd数量。在Provider报告期间，用来估算负载均衡。
        public int ApproximatelyLinkdCount { get; set; } = 4;
        public int ReportDelaySeconds { get; set; } = 10;
        public int ProposeMaxOnline { get; set; } = 15000;
        public int DigestionDelayExSeconds { get; set; } = 2;

        public static LoadConfig Load(string jsonFile = "load.json")
        {
            try
            {
                string json = Encoding.UTF8.GetString(System.IO.File.ReadAllBytes(jsonFile));
                return JsonSerializer.Deserialize<LoadConfig>(json);
            }
            catch (Exception)
            {
                //Console.WriteLine(e);
                return new LoadConfig();
            }
        }
    }
}
