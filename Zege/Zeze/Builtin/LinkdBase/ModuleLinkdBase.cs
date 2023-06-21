
using Zeze.Util;

namespace Zeze.Builtin.LinkdBase
{
    public partial class ModuleLinkdBase : AbstractModule
    {
        public void Start(global::Zege.App app)
        {
        }

        public void Stop(global::Zege.App app)
        {
        }

        protected override System.Threading.Tasks.Task<long> ProcessReportError(Zeze.Net.Protocol _p)
        {
            var p = _p as ReportError;
            return Task.FromResult(ResultCode.NotImplement);
        }

    }
}
