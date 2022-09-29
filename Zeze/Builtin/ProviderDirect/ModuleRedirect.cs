// auto-generated

// 转发只定义一个rpc，以后可能需要实现server之间的直连，不再通过转发
namespace Zeze.Builtin.ProviderDirect
{
    public sealed class ModuleRedirect : Zeze.Net.Rpc<Zeze.Builtin.ProviderDirect.BModuleRedirectArgument, Zeze.Builtin.ProviderDirect.BModuleRedirectResult>
    {
        public const int ModuleId_ = 11009;
        public const int ProtocolId_ = 1107993902;
        public const long TypeId_ = (long)ModuleId_ << 32 | unchecked((uint)ProtocolId_); // 47284402955566

        public override int ModuleId => ModuleId_;
        public override int ProtocolId => ProtocolId_;
        public const int RedirectTypeWithHash = 0;
        public const int RedirectTypeToServer = 1;
        public const int ResultCodeSuccess = 0;
        public const int ResultCodeMethodFullNameNotFound = 1;
        public const int ResultCodeHandleException = 2;
        public const int ResultCodeHandleError = 3;
        public const int ResultCodeLinkdTimeout = 10;
        public const int ResultCodeLinkdNoProvider = 11;
        public const int ResultCodeRequestTimeout = 12;

    }
}
