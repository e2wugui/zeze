
namespace gnet.Linkd
{
    public sealed partial class ModuleLinkd : AbstractModule
    {
        public void Start(Game.App app)
        {
        }

        public void Stop(Game.App app)
        {
        }

        public override int ProcessKeepAlive(KeepAlive protocol)
        {
            return Zeze.Transaction.Procedure.NotImplement;
        }

        public override int ProcessReportError(ReportError protocol)
        {
            return Zeze.Transaction.Procedure.NotImplement;
        }
    }
}
