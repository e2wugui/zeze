// auto-generated
package Game.Map;

public class CEnterWorld extends Zeze.Net.Protocol1<Zeze.Transaction.EmptyBean> {
    public final static int ModuleId_ = 8;
    public final static int ProtocolId_ = 22628;
    public final static int TypeId_ = ModuleId_ << 16 | ProtocolId_; 

    @Override
    public int getModuleId() {
        return ModuleId_;
    }

    @Override
    public int getProtocolId() {
        return ProtocolId_;
    }

    public CEnterWorld() {
        Argument = new Zeze.Transaction.EmptyBean();
    }

}
