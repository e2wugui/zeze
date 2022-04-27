
namespace Zezex.Linkd
{
    public partial class ModuleLinkd : AbstractModule
    {
        public void Start(global::Game.App app)
        {
        }

        public void Stop(global::Game.App app)
        {
        }

        protected override async System.Threading.Tasks.Task<long> ProcessKeepAlive(Zeze.Net.Protocol _p)
        {
            var p = _p as KeepAlive;
            return Zeze.Transaction.Procedure.NotImplement;
        }

    }
}
