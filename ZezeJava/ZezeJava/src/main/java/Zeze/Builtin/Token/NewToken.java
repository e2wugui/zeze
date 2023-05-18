// auto-generated @formatter:off
package Zeze.Builtin.Token;

// 申请新token
public class NewToken extends Zeze.Net.Rpc<Zeze.Builtin.Token.BNewTokenArg.Data, Zeze.Builtin.Token.BNewTokenRes.Data> {
    public static final int ModuleId_ = 11029;
    public static final int ProtocolId_ = -633142956; // 3661824340
    public static final long TypeId_ = Zeze.Net.Protocol.makeTypeId(ModuleId_, ProtocolId_); // 47372856131924
    static { register(TypeId_, NewToken.class); }

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

    public NewToken() {
        Argument = new Zeze.Builtin.Token.BNewTokenArg.Data();
        Result = new Zeze.Builtin.Token.BNewTokenRes.Data();
    }

    public NewToken(Zeze.Builtin.Token.BNewTokenArg.Data arg) {
        Argument = arg;
        Result = new Zeze.Builtin.Token.BNewTokenRes.Data();
    }
}
