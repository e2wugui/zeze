
using System;

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

        public void sendCs()
        {
            var p = new Cs();
            p.Argument.Account = "sendCs";
            p.Send(App.ClientService.GetSocket());
        }

        protected override System.Threading.Tasks.Task<long> ProcessSc(Zeze.Net.Protocol _p)
        {
            var p = _p as Sc;
            Console.WriteLine("ProcessSc: " + p.Argument.Account);
            return System.Threading.Tasks.Task.FromResult<long>(0);
        }

    }
}
