// auto-generated

using System.Collections.Generic;

namespace Zeze
{
    public sealed partial class App : Zeze.AppBase
    {

        public override Zeze.Application Zeze { get; set; }

        public Dictionary<string, Zeze.IModule> Modules { get; } = new Dictionary<string, Zeze.IModule>();

        public Zeze.ClientService ClientService { get; set; }

        public void CreateZeze(Zeze.Config config = null)
        {
            lock(this)
            {
                if (Zeze != null)
                    throw new System.Exception("Zeze Has Created!");

                Zeze = new Zeze.Application("WorldDemo", config);
            }
        }

        public void CreateService()
        {
            lock(this)
            {
                ClientService = new Zeze.ClientService(Zeze);
            }
        }

        public void CreateModules()
        {
            lock(this)
            {

            }
        }

        public void DestroyModules()
        {
            lock(this)
            {
                Modules.Clear();
            }
        }

        public void DestroyService()
        {
            lock(this)
            {
                ClientService = null;
            }
        }

        public void DestroyZeze()
        {
            lock(this)
            {
                Zeze = null;
            }
        }

        public void StartModules()
        {
            lock(this)
            {

            }
        }

        public void StopModules()
        {
            lock(this)
            {
            }
        }

        public void StartService()
        {
            lock(this)
            {
                ClientService.Start();
            }
        }

        public void StopService()
        {
            lock(this)
            {
                ClientService?.Stop();
            }
        }
    }
}
