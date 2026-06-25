// auto-generated

using System.Collections.Generic;

namespace Zege
{
    public sealed partial class App : Zeze.AppBase
    {

        public override Zeze.Application Zeze { get; set; }

        public Dictionary<string, Zeze.IModule> Modules { get; } = new Dictionary<string, Zeze.IModule>();

        public global::Zeze.Builtin.Online.ModuleOnline Zeze_Builtin_Online { get; set; }

        public global::Zeze.Builtin.LinkdBase.ModuleLinkdBase Zeze_Builtin_LinkdBase { get; set; }

        public global::Zege.Linkd.ModuleLinkd Zege_Linkd { get; set; }

        public global::Zege.Friend.ModuleFriend Zege_Friend { get; set; }

        public global::Zege.Message.ModuleMessage Zege_Message { get; set; }

        public global::Zege.User.ModuleUser Zege_User { get; set; }

        public global::Zege.Notify.ModuleNotify Zege_Notify { get; set; }

        public Zege.ClientService ClientService { get; set; }

        public void CreateZeze(Zeze.Config config = null)
        {
            lock(this)
            {
                if (Zeze != null)
                    throw new System.Exception("Zeze Has Created!");

                Zeze = new Zeze.Application("Zege", config);
            }
        }

        public void CreateService()
        {
            lock(this)
            {
                ClientService = new Zege.ClientService(Zeze);
            }
        }

        public void CreateModules()
        {
            lock(this)
            {
                Zeze_Builtin_Online = ReplaceModuleInstance(new global::Zeze.Builtin.Online.ModuleOnline(this));
                Zeze_Builtin_Online.Initialize();
                Zeze_Builtin_Online.Register();
                Modules.Add(Zeze_Builtin_Online.FullName, Zeze_Builtin_Online);
                Zeze_Builtin_LinkdBase = ReplaceModuleInstance(new global::Zeze.Builtin.LinkdBase.ModuleLinkdBase(this));
                Zeze_Builtin_LinkdBase.Initialize();
                Zeze_Builtin_LinkdBase.Register();
                Modules.Add(Zeze_Builtin_LinkdBase.FullName, Zeze_Builtin_LinkdBase);
                Zege_Linkd = ReplaceModuleInstance(new global::Zege.Linkd.ModuleLinkd(this));
                Zege_Linkd.Initialize();
                Zege_Linkd.Register();
                Modules.Add(Zege_Linkd.FullName, Zege_Linkd);
                Zege_Friend = ReplaceModuleInstance(new global::Zege.Friend.ModuleFriend(this));
                Zege_Friend.Initialize();
                Zege_Friend.Register();
                Modules.Add(Zege_Friend.FullName, Zege_Friend);
                Zege_Message = ReplaceModuleInstance(new global::Zege.Message.ModuleMessage(this));
                Zege_Message.Initialize();
                Zege_Message.Register();
                Modules.Add(Zege_Message.FullName, Zege_Message);
                Zege_User = ReplaceModuleInstance(new global::Zege.User.ModuleUser(this));
                Zege_User.Initialize();
                Zege_User.Register();
                Modules.Add(Zege_User.FullName, Zege_User);
                Zege_Notify = ReplaceModuleInstance(new global::Zege.Notify.ModuleNotify(this));
                Zege_Notify.Initialize();
                Zege_Notify.Register();
                Modules.Add(Zege_Notify.FullName, Zege_Notify);

            }
        }

        public void DestroyModules()
        {
            lock(this)
            {
                Zege_Notify = null;
                Zege_User = null;
                Zege_Message = null;
                Zege_Friend = null;
                Zege_Linkd = null;
                Zeze_Builtin_LinkdBase = null;
                Zeze_Builtin_Online = null;
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
                Zeze_Builtin_Online.Start(this);
                Zeze_Builtin_LinkdBase.Start(this);
                Zege_Linkd.Start(this);
                Zege_Friend.Start(this);
                Zege_Message.Start(this);
                Zege_User.Start(this);
                Zege_Notify.Start(this);

            }
        }

        public void StopModules()
        {
            lock(this)
            {
                Zege_Notify?.Stop(this);
                Zege_User?.Stop(this);
                Zege_Message?.Stop(this);
                Zege_Friend?.Stop(this);
                Zege_Linkd?.Stop(this);
                Zeze_Builtin_LinkdBase?.Stop(this);
                Zeze_Builtin_Online?.Stop(this);
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
