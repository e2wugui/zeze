// auto-generated

using System.Collections.Generic;

namespace Zeze
{
    public sealed partial class App : Zeze.AppBase
    {

        public override Zeze.Application Zeze { get; set; }

        public Dictionary<string, Zeze.IModule> Modules { get; } = new Dictionary<string, Zeze.IModule>();

        public Zeze.Builtin.LinkdBase.ModuleLinkdBase Zeze_Builtin_LinkdBase { get; set; }

        public Zezex.Linkd.ModuleLinkd Zezex_Linkd { get; set; }

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
                Zeze_Builtin_LinkdBase = ReplaceModuleInstance(new Zeze.Builtin.LinkdBase.ModuleLinkdBase(this));
                Zeze_Builtin_LinkdBase.Initialize();
                Zeze_Builtin_LinkdBase.Register();
                Modules.Add(Zeze_Builtin_LinkdBase.FullName, Zeze_Builtin_LinkdBase);
                Zezex_Linkd = ReplaceModuleInstance(new Zezex.Linkd.ModuleLinkd(this));
                Zezex_Linkd.Initialize();
                Zezex_Linkd.Register();
                Modules.Add(Zezex_Linkd.FullName, Zezex_Linkd);

            }
        }

        public void DestroyModules()
        {
            lock(this)
            {
                Zezex_Linkd = null;
                Zeze_Builtin_LinkdBase = null;
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
                Zeze_Builtin_LinkdBase.Start(this);
                Zezex_Linkd.Start(this);

            }
        }

        public void StopModules()
        {
            lock(this)
            {
                Zezex_Linkd?.Stop(this);
                Zeze_Builtin_LinkdBase?.Stop(this);
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
