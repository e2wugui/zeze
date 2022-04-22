// auto generate
namespace Zeze.Game
{
    public abstract class AbstractRank : Zeze.IModule 
    {
        public const int ModuleId = 11015;
        public override string FullName => "Zeze.Game.Rank";
        public override string Name => "Rank";
        public override int Id => ModuleId;
        public override bool IsBuiltin => true;

        internal Zeze.Builtin.Game.Rank.trank _trank = new Zeze.Builtin.Game.Rank.trank();

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
            zeze.AddTable(zeze.Config.GetTableConf(_trank.Name).DatabaseName, _trank);
        }

        public void UnRegisterZezeTables(Zeze.Application zeze)
        {
            zeze.RemoveTable(zeze.Config.GetTableConf(_trank.Name).DatabaseName, _trank);
        }

        public void RegisterRocksTables(Zeze.Raft.RocksRaft.Rocks rocks)
        {
        }

    }
}
