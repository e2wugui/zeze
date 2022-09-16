// auto-generated @formatter:off
package Zeze.Builtin.ProviderDirect;

public class Transmit extends Zeze.Net.Protocol<Zeze.Builtin.ProviderDirect.BTransmit> {
    public static final int ModuleId_ = 11009;
    public static final int ProtocolId_ = 902147088;
    public static final long TypeId_ = Zeze.Net.Protocol.makeTypeId(ModuleId_, ProtocolId_); // 47284197108752

    @Override
    public int getModuleId() {
        return ModuleId_;
    }

    @Override
    public int getProtocolId() {
        return ProtocolId_;
    }

    public Transmit() {
        Argument = new Zeze.Builtin.ProviderDirect.BTransmit();
    }

    public Transmit(Zeze.Builtin.ProviderDirect.BTransmit arg) {
        Argument = arg;
    }
}
