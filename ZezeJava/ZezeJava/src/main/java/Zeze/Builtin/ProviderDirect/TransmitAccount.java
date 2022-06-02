// auto-generated @formatter:off
package Zeze.Builtin.ProviderDirect;

public class TransmitAccount extends Zeze.Net.Protocol<Zeze.Builtin.ProviderDirect.BTransmitAccount> {
    public static final int ModuleId_ = 11009;
    public static final int ProtocolId_ = 952255342;
    public static final long TypeId_ = Zeze.Net.Protocol.MakeTypeId(ModuleId_, ProtocolId_);

    @Override
    public int getModuleId() {
        return ModuleId_;
    }

    @Override
    public int getProtocolId() {
        return ProtocolId_;
    }

    public TransmitAccount() {
        Argument = new Zeze.Builtin.ProviderDirect.BTransmitAccount();
    }
}
