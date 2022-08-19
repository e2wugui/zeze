
namespace Zeze.Builtin.Online
{
    public partial class ModuleOnline : AbstractModule
    {
        public void Start(global::Zege.App app)
        {
        }

        public void Stop(global::Zege.App app)
        {
        }

        protected override async System.Threading.Tasks.Task<long> ProcessSReliableNotify(Zeze.Net.Protocol _p)
        {
            var p = _p as SReliableNotify;
            return Zeze.Transaction.Procedure.NotImplement;
        }

    }
}
