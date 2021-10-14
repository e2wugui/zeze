package Zezex.Provider;

import Zezex.*;

// auto-generated



public final class ModuleRedirect extends Zeze.Net.Rpc<Zezex.Provider.BModuleRedirectArgument, Zezex.Provider.BModuleRedirectResult> {
	public static final int ModuleId_ = 10001;
	public static final int ProtocolId_ = 30314;
	public static final int TypeId_ = ModuleId_ << 16 | ProtocolId_;

	@Override
	public int getModuleId() {
		return ModuleId_;
	}
	@Override
	public int getProtocolId() {
		return ProtocolId_;
	}
	public static final int ResultCodeSuccess = 0;
	public static final int ResultCodeMethodFullNameNotFound = 1;
	public static final int ResultCodeHandleException = 2;
	public static final int ResultCodeHandleError = 3;
	public static final int ResultCodeLinkdTimeout = 10;
	public static final int ResultCodeLinkdNoProvider = 11;
	public static final int ResultCodeRequestTimeout = 12;

}