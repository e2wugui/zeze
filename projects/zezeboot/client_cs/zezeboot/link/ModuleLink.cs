using System.Threading.Tasks;
using Zeze.Net;
using zezeboot.login;

namespace zezeboot.link
{
    public class ModuleLink : AbstractModulelink
    {
        public static readonly ModuleLink Instance = new ModuleLink();

        public void OnHandshakeDone(AsyncSocket so)
        {
            var auth = new Auth
            {
                Argument = new BAuth
                {
                    Account = "TestAccountCs",
                    Token = "TestTokenCs",
                    Version = "0.1.0.0"
                }
            };
            var r = auth.Send(so, rpc =>
            {
                var rc = rpc.ResultCode;
                Log.Info($"Recv Auth resp({rc})");
                if (rc == 0)
                    ModuleLogin.Instance.OnAuthed(rpc.Sender);
                return Task.FromResult(0L);
            });
            Log.Info($"Send Auth = {r}");
        }
    }
}
