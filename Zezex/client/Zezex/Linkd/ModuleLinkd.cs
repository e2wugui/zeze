
using Zeze.Net;

namespace Zezex.Linkd
{
    public sealed partial class ModuleLinkd : AbstractModule
    {
        public void Start(Game.App app)
        {
        }

        public void Stop(Game.App app)
        {
        }

        public override int ProcessKeepAlive(Protocol _p)
        {
            var p = _p as KeepAlive;
            return Zeze.Transaction.Procedure.NotImplement;
        }

        public override int ProcessReportError(Protocol _p)
        {
            var p = _p as ReportError;
            return Zeze.Transaction.Procedure.NotImplement;
        }
    }
}
