// auto-generated

namespace Zeze.Beans.ProviderDirect
{
    public sealed class ModuleRedirect : Zeze.Net.Rpc<Zeze.Beans.ProviderDirect.BModuleRedirectArgument, Zeze.Beans.ProviderDirect.BModuleRedirectResult>
    {
        public const int ModuleId_ = 11009;
        public const int ProtocolId_ = -881551061;
        public const long TypeId_ = (long)ModuleId_ << 32 | (ProtocolId_ & 0xffff_ffff);

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
