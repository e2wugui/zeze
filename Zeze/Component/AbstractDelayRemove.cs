// auto generate
namespace Zeze.Component
{
    public abstract class AbstractDelayRemove : Zeze.IModule 
    {
        public const int ModuleId = 11007;
        public override string FullName => "Zeze.Beans.DelayRemove";
        public override string Name => "DelayRemove";
        public override int Id => ModuleId;


        public void RegisterProtocols(Zeze.Net.Service service)
        {
            // register protocol factory and handles
            var _reflect = new Zeze.Util.Reflect(this.GetType());
        }

        public void UnRegisterProtocols(Zeze.Net.Service service)
        {
        }

        public void RegisterZezeTables(Zeze.Application zeze)
        {
            // register table
        }

        public void UnRegisterZezeTables(Zeze.Application zeze)
        {
        }

        public void RegisterRocksTables(Zeze.Raft.RocksRaft.Rocks rocks)
        {
        }

    }
}
