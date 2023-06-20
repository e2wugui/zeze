// auto-generated

namespace Zege.User
{
    public abstract class AbstractModule : Zeze.IModule
    {
        public override string FullName => "Zege.User";
        public override string Name => "User";
        public override int Id => 1;

        public const int eAccountHasUsed = 1;
        public const int eAccountHasPrepared = 2;
        public const int ePrepareExpired = 3;
        public const int ePrepareNotOwner = 4;
        public const int eAccountInvalid = 5;
    }
}
