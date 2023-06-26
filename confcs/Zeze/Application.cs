using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace Zeze
{
    public class Application
    {
        public string Name { get; }
        public Config Config { get; set; }

        public Application(string name, Config config)
        {
            Name = name;
            Config = config;
        }

        public Task StartAsync()
        {
            return Task.CompletedTask;
        }

        public void Stop()
        {

        }
    }
}
