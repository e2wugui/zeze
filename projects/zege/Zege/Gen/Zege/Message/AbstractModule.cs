// auto-generated

// ReSharper disable RedundantNameQualifier UnusedParameter.Global UnusedVariable
// ReSharper disable once CheckNamespace
namespace Zege.Message
{
    public abstract class AbstractModule : Zeze.IModule
    {
        public override string FullName => "Zege.Message";
        public override string Name => "Message";
        public override int Id => 3;

        public const int eNotYourFriend = 1;
        public const int eGroupNotExist = 2;
        public const int eDepartmentNotExist = 3;
        public const int eMessageRange = 4;
        public const int eTooManyMembers = 4;
        public const long eGetMessageFromAboutRead = -1; // 从最后已读消息前面一点开始读取消息历史
        public const long eGetMessageFromAboutLast = -2; // 从最后收到的消息前面一点开始读取消息历史
        public const long eGetMessageToAuto = -1; // 读取的最后一条消息自动控制。一般为From+20

        protected abstract System.Threading.Tasks.Task<long>  ProcessNotifyMessageRequest(Zeze.Net.Protocol p);
    }
}
