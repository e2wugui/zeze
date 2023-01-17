// auto-generated @formatter:off
package Zeze.Builtin.ProviderDirect;

// 默认不启用事务，由协议实现自己控制。
public class TransmitAccount extends Zeze.Net.Protocol<Zeze.Builtin.ProviderDirect.BTransmitAccount> {
    public static final int ModuleId_ = 11009;
    public static final int ProtocolId_ = 952255342;
    public static final long TypeId_ = Zeze.Net.Protocol.makeTypeId(ModuleId_, ProtocolId_); // 47284247217006

    @Override
    public int getModuleId() {
        return ModuleId_;
    }

    @Override
    public int getProtocolId() {
        return ProtocolId_;
    }

    @Override
    public long getTypeId() {
        return TypeId_;
    }

    public TransmitAccount() {
        Argument = new Zeze.Builtin.ProviderDirect.BTransmitAccount();
    }

    public TransmitAccount(Zeze.Builtin.ProviderDirect.BTransmitAccount arg) {
        Argument = arg;
    }
}
