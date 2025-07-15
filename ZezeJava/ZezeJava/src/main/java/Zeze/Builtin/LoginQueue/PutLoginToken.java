// auto-generated @formatter:off
package Zeze.Builtin.LoginQueue;

// 			客户端收到这条协议表示排队成功。此后可以连接link继续登录。
public class PutLoginToken extends Zeze.Net.Protocol<Zeze.Builtin.LoginQueue.BLoginToken.Data> {
    public static final int ModuleId_ = 11043;
    public static final int ProtocolId_ = 1893050735;
    public static final long TypeId_ = Zeze.Net.Protocol.makeTypeId(ModuleId_, ProtocolId_); // 47431216900463
    static { register(TypeId_, PutLoginToken.class); }

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

    public PutLoginToken() {
        Argument = new Zeze.Builtin.LoginQueue.BLoginToken.Data();
    }

    public PutLoginToken(Zeze.Builtin.LoginQueue.BLoginToken.Data arg) {
        Argument = arg;
    }
}
