// auto-generated @formatter:off
package Zeze.Builtin.Game.Bag;

public class Move extends Zeze.Net.Rpc<Zeze.Builtin.Game.Bag.BMove, Zeze.Transaction.EmptyBean> {
    public static final int ModuleId_ = 11014;
    public static final int ProtocolId_ = -790071751; // 3504895545
    public static final long TypeId_ = Zeze.Net.Protocol.MakeTypeId(ModuleId_, ProtocolId_); // 47308274693689

    @Override
    public int getModuleId() {
        return ModuleId_;
    }

    @Override
    public int getProtocolId() {
        return ProtocolId_;
    }

    public Move() {
        Argument = new Zeze.Builtin.Game.Bag.BMove();
        Result = Zeze.Transaction.EmptyBean.instance;
    }

    public Move(Zeze.Builtin.Game.Bag.BMove arg) {
        Argument = arg;
        Result = Zeze.Transaction.EmptyBean.instance;
    }
}
