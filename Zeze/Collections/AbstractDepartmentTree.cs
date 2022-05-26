// auto generate
namespace Zeze.Collections
{
    public abstract class AbstractDepartmentTree : Zeze.IModule 
    {
        public const int ModuleId = 11101;
        public override string FullName => "Zeze.Collections.DepartmentTree";
        public override string Name => "DepartmentTree";
        public override int Id => ModuleId;
        public override bool IsBuiltin => true;

        internal Zeze.Builtin.Collections.DepartmentTree.tDepartment _tDepartment = new();
        internal Zeze.Builtin.Collections.DepartmentTree.tDepartmentTree _tDepartmentTree = new();

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
            zeze.AddTable(zeze.Config.GetTableConf(_tDepartment.Name).DatabaseName, _tDepartment);
            zeze.AddTable(zeze.Config.GetTableConf(_tDepartmentTree.Name).DatabaseName, _tDepartmentTree);
        }

        public void UnRegisterZezeTables(Zeze.Application zeze)
        {
            zeze.RemoveTable(zeze.Config.GetTableConf(_tDepartment.Name).DatabaseName, _tDepartment);
            zeze.RemoveTable(zeze.Config.GetTableConf(_tDepartmentTree.Name).DatabaseName, _tDepartmentTree);
        }

        public void RegisterRocksTables(Zeze.Raft.RocksRaft.Rocks rocks)
        {
        }

    }
}
