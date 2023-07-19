// auto-generated

// ReSharper disable RedundantNameQualifier UnusedParameter.Global UnusedVariable
// ReSharper disable once CheckNamespace
namespace Zezex.Linkd
{
    public abstract class AbstractModule : Zeze.IModule
    {
        public override string FullName => "Zezex.Linkd";
        public override string Name => "Linkd";
        public override int Id => 10000;

        protected abstract System.Threading.Tasks.Task<long>  ProcessKeepAlive(Zeze.Net.Protocol p);
    }
}
