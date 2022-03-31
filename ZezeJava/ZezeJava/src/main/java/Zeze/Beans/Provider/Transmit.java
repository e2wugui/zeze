// auto-generated @formatter:off
package Zeze.Beans.Provider;

public class Transmit extends Zeze.Net.Protocol1<Zeze.Beans.Provider.BTransmit> {
    public static final int ModuleId_ = 11008;
    public static final int ProtocolId_ = 381060094;
    public static final long TypeId_ = Zeze.Net.Protocol.MakeTypeId(ModuleId_, ProtocolId_);

    @Override
    public int getModuleId() {
        return ModuleId_;
    }

    @Override
    public int getProtocolId() {
        return ProtocolId_;
    }

    public Transmit() {
        Argument = new Zeze.Beans.Provider.BTransmit();
    }
}
