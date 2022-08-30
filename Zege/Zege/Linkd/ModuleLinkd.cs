
using Zeze.Util;

namespace Zege.Linkd
{
    public partial class ModuleLinkd : AbstractModule
    {
        public void Start(global::Zege.App app)
        {
        }

        public void Stop(global::Zege.App app)
        {
        }

        protected override async System.Threading.Tasks.Task<long> ProcessKeepAlive(Zeze.Net.Protocol _p)
        {
            var p = _p as KeepAlive;
            return ResultCode.NotImplement;
        }

        protected override async Task<long> ProcessChallengeOkRequest(Zeze.Net.Protocol _p)
        {
            var p = _p as ChallengeOk;
            return ResultCode.NotImplement;
        }

        protected override async Task<long> ProcessChallengeRequest(Zeze.Net.Protocol _p)
        {
            var p = _p as Challenge;
            return ResultCode.NotImplement;
        }
    }
}
