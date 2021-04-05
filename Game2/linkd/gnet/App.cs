
using System;
using System.Text;
using System.Text.Json;

namespace gnet
{
    public sealed partial class App
    {
        public Zeze.Util.Scheduler Scheduler { get; } = new Zeze.Util.Scheduler();

        public Zeze.IModule ReplaceModuleInstance(Zeze.IModule module)
        {
            return module;
        }


        public Config Config { get; private set; }

        private void LoadConfig()
        {
            try
            {
                string json = Encoding.UTF8.GetString(System.IO.File.ReadAllBytes("linkd.json"));
                Config = JsonSerializer.Deserialize<Config>(json);
            }
            catch (Exception)
            {
                //MessageBox.Show(ex.ToString());
            }
            if (null == Config)
                Config = new Config();
        }

        public void Start()
        {
            LoadConfig();
            Create();
            StartModules(); // 启动模块，装载配置什么的。
            Zeze.Start(); // 启动数据库
            StartService(); // 启动网络
        }

        public void Stop()
        {
            StopService(); // 关闭网络
            Zeze.Stop(); // 关闭数据库
            StopModules(); // 关闭模块,，卸载配置什么的。
            Destroy();
        }
    }
}
