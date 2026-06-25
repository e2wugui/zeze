// auto-generated

// ReSharper disable RedundantNameQualifier UnusedParameter.Global UnusedVariable
// ReSharper disable once CheckNamespace
namespace Zeze.Builtin.Online
{
    public abstract class AbstractModule : Zeze.IModule
    {
        public override string FullName => "Zeze.Builtin.Online";
        public override string Name => "Online";
        public override int Id => 11100;

        public const int ResultCodeSuccess = 0;
        public const int ResultCodeCreateRoleDuplicateRoleName = 1;
        public const int ResultCodeAccountNotExist = 2;
        public const int ResultCodeRoleNotExist = 3;
        public const int ResultCodeNotLastLoginRoleId = 4;
        public const int ResultCodeOnlineDataNotFound = 5;
        public const int ResultCodeReliableNotifyConfirmIndexOutOfRange = 6;
        public const int ResultCodeNotLogin = 7;
        public const int eOffline = 0;
        public const int eLinkBroken = 1;
        public const int eLogined = 2;

        protected abstract System.Threading.Tasks.Task<long>  ProcessSReliableNotify(Zeze.Net.Protocol p);
    }
}
