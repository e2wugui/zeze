package Zeze.Services.GlobalCacheManager;

public class AchillesHeel extends Zeze.Transaction.Bean {
    public int AutoKeyLocalId; // 必须的。

    public String SecureKey; // 安全验证
    public int GlobalCacheManagerHashIndex; // 安全验证

    @Override
    public void Decode(Zeze.Serialize.ByteBuffer bb) {
        AutoKeyLocalId = bb.ReadInt();
        SecureKey = bb.ReadString();
        GlobalCacheManagerHashIndex = bb.ReadInt();
    }

    @Override
    public void Encode(Zeze.Serialize.ByteBuffer bb) {
        bb.WriteInt(AutoKeyLocalId);
        bb.WriteString(SecureKey);
        bb.WriteInt(GlobalCacheManagerHashIndex);
    }

    @Override
    protected void InitChildrenRootInfo(Zeze.Transaction.Record.RootInfo root) {
        throw new UnsupportedOperationException();
    }
}

