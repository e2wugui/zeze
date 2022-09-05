
using System.Security.Cryptography.X509Certificates;
using Zeze.Net;
using Zeze.Util;

namespace Zege.User
{
    public partial class ModuleUser : AbstractModule
    {
        public string Account { get; private set; }

        public void Start(global::Zege.App app)
        {
        }

        public void Stop(global::Zege.App app)
        {
        }

        public async Task<X509Certificate2> GetCertAsync()
        {
            var certSaved = await SecureStorage.Default.GetAsync(Account + ".pkcs12");
            if (null == certSaved)
                return null;
            var pkcs12 = Convert.FromBase64String(certSaved);
            var passwd = await SecureStorage.Default.GetAsync(Account + ".password");
            if (null == passwd)
                passwd = "123"; // TODO 显示输入密码窗口。
            return Cert.CreateFromPkcs12(pkcs12, passwd);
        }

        public async Task TryCreateAsync(string account)
        {
            Account = account;

            var certSaved = await SecureStorage.Default.GetAsync(account);
            if (certSaved != null)
                return;

            var p = new Prepare();
            p.Argument.Account = account;
            await p.SendAsync(App.Connector.TryGetReadySocket());
            if (Mission.VerifySkipResultCode(p.ResultCode, eAccountHasUsed))
                return; // TODO 先简单忽略账号已经被使用的错误。完整的流程应该是把创建账号独立出来。

            var c = new Create();
            var rsa = Cert.GenerateRsa();
            var sign = Cert.Sign(rsa, p.Result.RandomData.GetBytesUnsafe());
            c.Argument.Account = account;
            c.Argument.RsaPublicKey = new Binary(rsa.ExportRSAPublicKey());
            c.Argument.Signed = new Binary(sign);
            await c.SendAsync(App.Connector.TryGetReadySocket());
            Mission.VerifySkipResultCode(c.ResultCode);

            var cert = Cert.CreateFromCertAndPrivateKey(c.Result.Cert.GetBytesUnsafe(), rsa);
            var passwd = "123"; // TODO 显示输入密码窗口。
            var pkcs12 = cert.Export(X509ContentType.Pkcs12, passwd);
            await SecureStorage.Default.SetAsync(account + ".pkcs12", Convert.ToBase64String(pkcs12));
        }
    }
}
