// auto-generated @formatter:off
package Zeze.Builtin.Token;

// 获取token服务器的全局状态
public class TokenStatus extends Zeze.Net.Rpc<Zeze.Transaction.EmptyBean.Data, Zeze.Builtin.Token.BTokenStatus.Data> {
    public static final int ModuleId_ = 11029;
    public static final int ProtocolId_ = -365098350; // 3929868946
    public static final long TypeId_ = Zeze.Net.Protocol.makeTypeId(ModuleId_, ProtocolId_); // 47373124176530
    static { register(TypeId_, TokenStatus.class); }

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

    public TokenStatus() {
        Argument = Zeze.Transaction.EmptyBean.Data.instance;
        Result = new Zeze.Builtin.Token.BTokenStatus.Data();
    }
}
