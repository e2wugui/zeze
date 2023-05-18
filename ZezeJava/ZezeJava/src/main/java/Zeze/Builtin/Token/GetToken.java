// auto-generated @formatter:off
package Zeze.Builtin.Token;

// 获取token状态(也可用于验证和删除token)
public class GetToken extends Zeze.Net.Rpc<Zeze.Builtin.Token.BGetTokenArg.Data, Zeze.Builtin.Token.BGetTokenRes.Data> {
    public static final int ModuleId_ = 11029;
    public static final int ProtocolId_ = -1570200909; // 2724766387
    public static final long TypeId_ = Zeze.Net.Protocol.makeTypeId(ModuleId_, ProtocolId_); // 47371919073971
    static { register(TypeId_, GetToken.class); }

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

    public GetToken() {
        Argument = new Zeze.Builtin.Token.BGetTokenArg.Data();
        Result = new Zeze.Builtin.Token.BGetTokenRes.Data();
    }

    public GetToken(Zeze.Builtin.Token.BGetTokenArg.Data arg) {
        Argument = arg;
        Result = new Zeze.Builtin.Token.BGetTokenRes.Data();
    }
}
