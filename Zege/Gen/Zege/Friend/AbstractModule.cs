// auto-generated

// ReSharper disable RedundantNameQualifier UnusedParameter.Global UnusedVariable
// ReSharper disable once CheckNamespace
namespace Zege.Friend
{
    public abstract class AbstractModule : Zeze.IModule
    {
        public override string FullName => "Zege.Friend";
        public override string Name => "Friend";
        public override int Id => 2;

        public const int eDepartmentNotFound = 1;
        public const int eFriendNodeNotFound = 2;
        public const int eMemberNodeNotFound = 3;
        public const int eDeparmentMemberNotInGroup = 4;
        public const int eUserNotFound = 5;
        public const int eManagePermission = 6;
        public const int eNotGroupMember = 7;
        public const int eToomanyBelongDepartments = 8;
        public const int eParameterError = 9;
        public const int eUserExists = 10;
        public const int eNotFriend = 11;
        public const int eGroupNotExist = 12;
        public const int eNotTopmost = 13;
        public const int eAlreadyIsFriend = 14;
        public const int eTooManyTopmost = 15;
        public const int eAcceptFromNotExist = 16;
        public const int eVerifySigned = 17;
        public const int eGroupInviteMax = 18;

        protected abstract System.Threading.Tasks.Task<long>  ProcessFriendNodeLogBeanNotify(Zeze.Net.Protocol p);
    }
}
