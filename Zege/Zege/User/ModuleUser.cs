
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
                passwd = await Mission.AppShell.GetPromptAsync("Seccurity", "Private Key Password");

            if (null == passwd)
                throw new Exception("Open Private Key Fail.");

            return Cert.CreateFromPkcs12(pkcs12, passwd);
        }

        public async Task<bool> TryCreateAsync(string account, string passwd, bool savedPasswd)
        {
            Account = account;

            var certSaved = await SecureStorage.Default.GetAsync(account + ".pkcs12");
            if (certSaved != null)
                return false;

            var p = new Prepare();
            p.Argument.Account = account;
            await p.SendAsync(App.Connector.TryGetReadySocket());
            if (Mission.VerifySkipResultCode(p.ResultCode, eAccountHasUsed))
                return false; // TODO 先简单忽略账号已经被使用的错误。完整的流程应该是把创建账号独立出来。

            var c = new Create();
            var rsa = Cert.GenerateRsa();
            var sign = Cert.Sign(rsa, p.Result.RandomData.GetBytesUnsafe());
            c.Argument.Account = account;
            c.Argument.RsaPublicKey = new Binary(rsa.ExportRSAPublicKey());
            c.Argument.Signed = new Binary(sign);
            await c.SendAsync(App.Connector.TryGetReadySocket());
            Mission.VerifySkipResultCode(c.ResultCode);

            var cert = Cert.CreateFromCertAndPrivateKey(c.Result.Cert.GetBytesUnsafe(), rsa);
            if (savedPasswd)
                await SecureStorage.Default.SetAsync(account + ".password", passwd);
            var pkcs12 = cert.Export(X509ContentType.Pkcs12, passwd);
            await SecureStorage.Default.SetAsync(account + ".pkcs12", Convert.ToBase64String(pkcs12));
            return true;
        }
    }
}
