
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

        protected override System.Threading.Tasks.Task<long> ProcessReportError(Zeze.Net.Protocol _p)
        {
            //var p = _p as ReportError;
            return System.Threading.Tasks.Task.FromResult(Zeze.Transaction.Procedure.NotImplement);
        }

    }
}
