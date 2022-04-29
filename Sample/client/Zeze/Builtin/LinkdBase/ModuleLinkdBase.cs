
namespace Zeze.Builtin.LinkdBase
{
    public partial class ModuleLinkdBase : AbstractModule
    {
        public void Start(global::ClientGame.App app)
        {
        }

        public void Stop(global::ClientGame.App app)
        {
        }

        protected override async System.Threading.Tasks.Task<long> ProcessReportError(Zeze.Net.Protocol _p)
        {
            var p = _p as ReportError;
            return Zeze.Transaction.Procedure.NotImplement;
        }

    }
}
