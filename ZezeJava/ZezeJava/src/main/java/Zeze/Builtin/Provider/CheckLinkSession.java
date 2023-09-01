// auto-generated @formatter:off
package Zeze.Builtin.Provider;

/*
			定义到这里，但是会通过Send包装发送，最终Link在处理Send时并不实际发送给客户端。
			问题：如果需要探测到客户端，需要真实的发给客户端，但是客户端怎么得到这个协议定义是个问题。【先不考虑支持】
*/
public class CheckLinkSession extends Zeze.Net.Protocol<Zeze.Transaction.EmptyBean.Data> {
    public static final int ModuleId_ = 11008;
    public static final int ProtocolId_ = 498078956;
    public static final long TypeId_ = Zeze.Net.Protocol.makeTypeId(ModuleId_, ProtocolId_); // 47279498073324
    static { register(TypeId_, CheckLinkSession.class); }

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

    public CheckLinkSession() {
        Argument = Zeze.Transaction.EmptyBean.Data.instance;
    }
}
