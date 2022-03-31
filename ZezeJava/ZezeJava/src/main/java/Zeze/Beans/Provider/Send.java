// auto-generated @formatter:off
package Zeze.Beans.Provider;

public class Send extends Zeze.Net.Protocol1<Zeze.Beans.Provider.BSend> {
    public static final int ModuleId_ = 11008;
    public static final int ProtocolId_ = 1423658047;
    public static final long TypeId_ = Zeze.Net.Protocol.MakeTypeId(ModuleId_, ProtocolId_);

    @Override
    public int getModuleId() {
        return ModuleId_;
    }

    @Override
    public int getProtocolId() {
        return ProtocolId_;
    }

    public Send() {
        Argument = new Zeze.Beans.Provider.BSend();
    }
}
