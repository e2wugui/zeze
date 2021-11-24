package Zeze.Services.GlobalCacheManager;

public class LoginParam extends Zeze.Transaction.Bean {
    public int ServerId;

    // GlobalCacheManager 本身没有编号。
    // 启用多个进程，使用 GlobalTableKey.GetHashCode() 分配负载后，报告错误需要这个来识别哪个进程。
    // 当然识别还可以根据 ServerService 绑定的ip和port。
    // 给每个实例加配置不容易维护。
    public int GlobalCacheManagerHashIndex;

    @Override
    public void Decode(Zeze.Serialize.ByteBuffer bb) {
        ServerId = bb.ReadInt();
        GlobalCacheManagerHashIndex = bb.ReadInt();
    }

    @Override
    public void Encode(Zeze.Serialize.ByteBuffer bb) {
        bb.WriteInt(ServerId);
        bb.WriteInt(GlobalCacheManagerHashIndex);
    }

    @Override
    protected void InitChildrenRootInfo(Zeze.Transaction.Record.RootInfo root) {
        throw new UnsupportedOperationException();
    }
}
