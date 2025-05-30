
namespace Zezex.Linkd
{
    public partial class ModuleLinkd : AbstractModule
    {
        public void Start(global::Zezex.App app)
        {
        }

        public void Stop(global::Zezex.App app)
        {
        }

        protected override async System.Threading.Tasks.Task<long> ProcessSc(Zeze.Net.Protocol _p)
        {
            var p = _p as Sc;
            return Zeze.Util.ResultCode.NotImplement;
        }

    }
}
