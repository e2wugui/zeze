using System.Threading.Tasks;
using Zeze.Builtin.LinkdBase;
using Zeze.Net;

namespace zezeboot.link
{
    public class ModuleLinkdBase : AbstractModuleLinkdBase
    {
        public static readonly ModuleLinkdBase Instance = new ModuleLinkdBase();

        protected override Task<long> ProcessReportError(Protocol p)
        {
            Log.Info($"ReportError = {p.ArgumentBean}");
            return Task.FromResult(0L);
        }
    }
}
