// auto-generated

// ReSharper disable RedundantNameQualifier UnusedParameter.Global UnusedVariable
// ReSharper disable once CheckNamespace
namespace Zeze.Builtin.LinkdBase
{
    public abstract class AbstractModule : Zeze.IModule
    {
        public override string FullName => "Zeze.Builtin.LinkdBase";
        public override string Name => "LinkdBase";
        public override int Id => 11011;

        protected abstract System.Threading.Tasks.Task<long>  ProcessReportError(Zeze.Net.Protocol p);
    }
}
