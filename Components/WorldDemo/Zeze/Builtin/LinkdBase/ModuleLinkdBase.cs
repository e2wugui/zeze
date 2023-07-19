
namespace Zeze.Builtin.LinkdBase
{
    public partial class ModuleLinkdBase : AbstractModule
    {
        public void Start(global::Zeze.App app)
        {
        }

        public void Stop(global::Zeze.App app)
        {
        }

        protected override async System.Threading.Tasks.Task<long> ProcessReportError(Zeze.Net.Protocol _p)
        {
            var p = _p as ReportError;
            return Zeze.Util.ResultCode.NotImplement;
        }

    }
}
