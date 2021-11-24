package Zeze.Services.GlobalCacheManager;

public class Param2 extends Zeze.Transaction.Bean {
    public GlobalTableKey GlobalTableKey; // 没有初始化，使用时注意
    public int State;
    public long GlobalSerialId;

    @Override
    public void Decode(Zeze.Serialize.ByteBuffer bb) {
        if (null == GlobalTableKey)
            GlobalTableKey = new GlobalTableKey();
        GlobalTableKey.Decode(bb);
        State = bb.ReadInt();
        GlobalSerialId = bb.ReadLong();
    }

    @Override
    public void Encode(Zeze.Serialize.ByteBuffer bb) {
        GlobalTableKey.Encode(bb);
        bb.WriteInt(State);
        bb.WriteLong(GlobalSerialId);
    }

    @Override
    protected void InitChildrenRootInfo(Zeze.Transaction.Record.RootInfo root) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String toString() {
        return GlobalTableKey + ":" + State;
    }
}
