// auto-generated

// ReSharper disable RedundantNameQualifier UnusedParameter.Global UnusedVariable
// ReSharper disable once CheckNamespace
namespace Zege.Linkd
{
    public abstract class AbstractModule : Zeze.IModule
    {
        public override string FullName => "Zege.Linkd";
        public override string Name => "Linkd";
        public override int Id => 10000;

        public const int eNobody = 1;

        protected abstract System.Threading.Tasks.Task<long>  ProcessChallengeRequest(Zeze.Net.Protocol p);

        protected abstract System.Threading.Tasks.Task<long>  ProcessChallengeResultRequest(Zeze.Net.Protocol p);
    }
}
