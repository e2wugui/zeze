
namespace Zege.Message
{
    public partial class ModuleMessage : AbstractModule
    {
        public void Start(global::Zege.App app)
        {
        }

        public void Stop(global::Zege.App app)
        {
        }

        protected override async System.Threading.Tasks.Task<long> ProcessNotifyMessageRequest(Zeze.Net.Protocol _p)
        {
            var p = _p as NotifyMessage;
            return Zeze.Transaction.Procedure.NotImplement;
        }

    }
}
