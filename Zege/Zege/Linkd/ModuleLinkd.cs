
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

        protected override Task<long> ProcessChallengeOkRequest(Zeze.Net.Protocol _p)
        {
            var p = _p as ChallengeOk;
            ChallengeFuture.SetResult(true);
            p.SendResult();
            return Task.FromResult(ResultCode.Success);
        }

        protected override Task<long> ProcessChallengeRequest(Zeze.Net.Protocol _p)
        {
            var r = _p as Challenge;
            r.Result.Account = App.Zege_User.Account;
            var file = App.Zege_User.Account + ".pkcs12";
            if (!File.Exists(file))
            {
                r.SendResultCode(1);
                return Task.FromResult(0L); // done
            }

            var passwd = "123";
            var cert = Cert.CreateFromPkcs12(file, passwd);
            var signed = Cert.Sign(cert, r.Argument.RandomData.GetBytesUnsafe());
            r.Result.Signed = new Binary(signed);
            r.SendResult();

            return Task.FromResult(ResultCode.Success);
        }

        public volatile TaskCompletionSource<bool> ChallengeFuture;

        public void SendChallengeMe()
        {
            new ChallengeMe().Send(App.Connector.TryGetReadySocket()); // skip rpc result
            ChallengeFuture?.TrySetException(new Exception("Cancel!"));
            ChallengeFuture = new();
        }
    }
}
