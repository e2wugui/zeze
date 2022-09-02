
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

        public async Task OpenAsync(string account)
        {
            Account = account;

            var fileName = account + ".pkcs12";
            if (File.Exists(fileName))
            {
                App.Zege_Linkd.SendChallengeMe();
                return; // 已经注册。先简单用文件是否存在判断一下。
            }

            var p = new Prepare();
            p.Argument.Account = account;
            await p.SendAsync(App.Connector.TryGetReadySocket());
            if (GetErrorCode(p.ResultCode) == RCAccountExist)
                return; // 账号已经创建，结束流程。done

            if (p.ResultCode != 0)
                throw new Exception($"Prepare Error! Module={GetModuleId(p.ResultCode)} Code={GetErrorCode(p.ResultCode)}");

            var c = new Create();

            var rsa = Cert.GenerateRsa();
            var sign = Cert.Sign(rsa, p.Result.RandomData.GetBytesUnsafe());

            c.Argument.Account = account;
            c.Argument.RsaPublicKey = new Binary(rsa.ExportRSAPublicKey());
            c.Argument.Signed = new Binary(sign);

            await c.SendAsync(App.Connector.TryGetReadySocket());
            if (c.ResultCode != 0 && GetErrorCode(c.ResultCode) != RCAccountExist)
                throw new Exception($"Create Error! Module={GetModuleId(p.ResultCode)} Code={GetErrorCode(p.ResultCode)}");

            var cert = Cert.CreateFromCertAndPrivateKey(c.Result.Cert.GetBytesUnsafe(), rsa);
            var passwd = "123";
            var pkcs12 = cert.Export(System.Security.Cryptography.X509Certificates.X509ContentType.Pkcs12, passwd);
            File.WriteAllBytes(fileName, pkcs12);

            App.Zege_Linkd.SendChallengeMe();
        }
    }
}
