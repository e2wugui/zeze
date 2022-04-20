// auto-generated @formatter:off
package Zeze.Beans.Game.Bag;

public class Sort extends Zeze.Net.Rpc<Zeze.Transaction.EmptyBean, Zeze.Transaction.EmptyBean> {
    public static final int ModuleId_ = 11014;
    public static final int ProtocolId_ = 1950429894;
    public static final long TypeId_ = Zeze.Net.Protocol.MakeTypeId(ModuleId_, ProtocolId_);

    @Override
    public int getModuleId() {
        return ModuleId_;
    }

    @Override
    public int getProtocolId() {
        return ProtocolId_;
    }

    public Sort() {
        Argument = new Zeze.Transaction.EmptyBean();
        Result = new Zeze.Transaction.EmptyBean();
    }
}
