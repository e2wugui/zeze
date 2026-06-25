using System.Threading.Tasks;

namespace Zeze
{
    public class Application
    {
        public readonly Config Config;
        public readonly string Name;

        public Application(string name, Config config)
        {
            Name = name;
            Config = config;
        }

        public Application(Config config)
        {
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
