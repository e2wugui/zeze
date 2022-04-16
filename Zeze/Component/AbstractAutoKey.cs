// auto generate
namespace Zeze.Component
{
    public abstract class AbstractAutoKey : Zeze.IModule 
    {
        public const int ModuleId = 11003;
        public override string FullName => "Zeze.Beans.AutoKey";
        public override string Name => "AutoKey";
        public override int Id => ModuleId;

        internal Zeze.Beans.AutoKey.tAutoKeys _tAutoKeys = new Zeze.Beans.AutoKey.tAutoKeys();

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
            zeze.AddTable(zeze.Config.GetTableConf(_tAutoKeys.Name).DatabaseName, _tAutoKeys);
        }

        public void UnRegisterZezeTables(Zeze.Application zeze)
        {
            zeze.RemoveTable(zeze.Config.GetTableConf(_tAutoKeys.Name).DatabaseName, _tAutoKeys);
        }

        public void RegisterRocksTables(Zeze.Raft.RocksRaft.Rocks rocks)
        {
        }

    }
}
