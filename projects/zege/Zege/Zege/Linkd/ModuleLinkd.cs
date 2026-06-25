
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

        protected override Task<long> ProcessChallengeResultRequest(Zeze.Net.Protocol _p)
        {
            var p = _p as ChallengeResult;
            ChallengeFuture.TrySetResult(p.ResultCode == 0);
            p.SendResult();
            return Task.FromResult(ResultCode.Success);
        }

        protected override Task<long> ProcessChallengeRequest(Zeze.Net.Protocol _p)
        {
            var r = _p as Challenge;
            r.Result.Account = App.Zege_User.Account;
            if (null == App.Zege_User.Certificate)
                return Task.FromResult(ErrorCode(eNobody));

            var signed = Cert.Sign(App.Zege_User.Certificate, r.Argument.RandomData.GetBytesUnsafe());
            r.Result.Signed = new Binary(signed);
            r.SendResult();
            return Task.FromResult(ResultCode.Success);
        }

        public TaskCompletionSource<bool> ChallengeFuture = new();

        public async Task<bool> ChallengeMeAsync(string account, string passwd, bool save)
        {
            if (0 != await App.Zege_User.OpenAccountAsync(account, passwd, save))
                return false;

            ChallengeFuture?.TrySetException(new Exception("Cancel"));
            ChallengeFuture = new(); // 每次挑战重新创建一个Future。
            new ChallengeMe().Send(App.Connector.TryGetReadySocket());
            await ChallengeFuture.Task.WaitAsync(TimeSpan.FromSeconds(5));
            return true;
        }
    }
}
