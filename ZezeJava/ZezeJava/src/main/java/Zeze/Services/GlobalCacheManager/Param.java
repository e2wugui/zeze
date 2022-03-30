package Zeze.Services.GlobalCacheManager;

import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.Bean;
import Zeze.Transaction.Record;
import Zeze.Beans.GlobalCacheManagerWithRaft.GlobalTableKey;

public class Param extends Bean {
    public GlobalTableKey GlobalTableKey; // 没有初始化，使用时注意
    public int State;

    @Override
    public void Decode(ByteBuffer bb) {
        if (null == GlobalTableKey)
            GlobalTableKey = new GlobalTableKey();
        GlobalTableKey.Decode(bb);
        State = bb.ReadInt();
    }

    @Override
    public void Encode(ByteBuffer bb) {
        GlobalTableKey.Encode(bb);
        bb.WriteInt(State);
    }

    @Override
    protected void InitChildrenRootInfo(Record.RootInfo root) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String toString() {
        return GlobalTableKey + ":" + State;
    }

    private static int _PRE_ALLOC_SIZE_ = 16;

    @Override
    public int getPreAllocSize() {
        return _PRE_ALLOC_SIZE_;
    }

    @Override
    public void setPreAllocSize(int size) {
        _PRE_ALLOC_SIZE_ = size;
    }
}
