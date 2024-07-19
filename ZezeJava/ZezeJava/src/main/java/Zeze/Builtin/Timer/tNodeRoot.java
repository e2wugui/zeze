// auto-generated @formatter:off
package Zeze.Builtin.Timer;

import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.TableX;
import Zeze.Transaction.TableReadOnly;

// key是serverId, 每个server维护处理自己的节点链表
@SuppressWarnings({"DuplicateBranchesInSwitch", "NullableProblems", "RedundantSuppression"})
public final class tNodeRoot extends TableX<Integer, Zeze.Builtin.Timer.BNodeRoot>
        implements TableReadOnly<Integer, Zeze.Builtin.Timer.BNodeRoot, Zeze.Builtin.Timer.BNodeRootReadOnly> {
    public tNodeRoot() {
        super(1952520306, "Zeze_Builtin_Timer_tNodeRoot");
    }

    public tNodeRoot(String _s_) {
        super(1952520306, "Zeze_Builtin_Timer_tNodeRoot", _s_);
    }

    @Override
    public Class<Integer> getKeyClass() {
        return Integer.class;
    }

    @Override
    public Class<Zeze.Builtin.Timer.BNodeRoot> getValueClass() {
        return Zeze.Builtin.Timer.BNodeRoot.class;
    }

    public static final int VAR_HeadNodeId = 1;
    public static final int VAR_TailNodeId = 2;
    public static final int VAR_LoadSerialNo = 3;
    public static final int VAR_Version = 4;

    @Override
    public Integer decodeKey(ByteBuffer _os_) {
        int _v_;
        _v_ = _os_.ReadInt();
        return _v_;
    }

    @Override
    public ByteBuffer encodeKey(Integer _v_) {
        ByteBuffer _os_ = ByteBuffer.Allocate(ByteBuffer.WriteLongSize(_v_));
        _os_.WriteInt(_v_);
        return _os_;
    }

    @Override
    public Integer decodeKeyResultSet(java.sql.ResultSet _s_) throws java.sql.SQLException {
        int _v_;
        _v_ = _s_.getInt("__key");
        return _v_;
    }

    @Override
    public void encodeKeySQLStatement(Zeze.Serialize.SQLStatement _s_, Integer _v_) {
        _s_.appendInt("__key", _v_);
    }

    @Override
    public Zeze.Builtin.Timer.BNodeRoot newValue() {
        return new Zeze.Builtin.Timer.BNodeRoot();
    }

    @Override
    public Zeze.Builtin.Timer.BNodeRootReadOnly getReadOnly(Integer _k_) {
        return get(_k_);
    }
}
