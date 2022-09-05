
using Zeze.Net;
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

        protected override Task<long> ProcessKeepAlive(Zeze.Net.Protocol _p)
        {
            var p = _p as KeepAlive;
            p.SendResult();
            return Task.FromResult(ResultCode.Success);
        }

        protected override Task<long> ProcessChallengeResultRequest(Zeze.Net.Protocol _p)
        {
            var p = _p as ChallengeResult;
            ChallengeFuture.TrySetResult(p.ResultCode == 0);
            p.SendResult();
            return Task.FromResult(ResultCode.Success);
        }

        protected override async Task<long> ProcessChallengeRequest(Zeze.Net.Protocol _p)
        {
            var r = _p as Challenge;
            r.Result.Account = App.Zege_User.Account;
            var cert = await App.Zege_User.GetCertAsync();
            if (null == cert)
                return ErrorCode(eNobody);

            var signed = Cert.Sign(cert, r.Argument.RandomData.GetBytesUnsafe());
            r.Result.Signed = new Binary(signed);
            r.SendResult();
            return ResultCode.Success;
        }

        private TaskCompletionSource<bool> ChallengeFuture = new();

        public async Task ChallengeMeAsync()
        {
            if (ChallengeFuture.Task.IsCompletedSuccessfully)
                return;

            ChallengeFuture?.TrySetException(new Exception("Cancel"));
            ChallengeFuture = new(); // 每次挑战重新创建一个Future。
            new ChallengeMe().Send(App.Connector.TryGetReadySocket());
            await ChallengeFuture.Task.WaitAsync(TimeSpan.FromSeconds(5));
        }
    }
}
