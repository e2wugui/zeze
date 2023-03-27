// auto-generated @formatter:off
package Zeze.Builtin.Timer;

import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.TableX;
import Zeze.Transaction.TableReadOnly;

// key is serverid 每一台server拥有自己的链表。
@SuppressWarnings({"DuplicateBranchesInSwitch", "RedundantSuppression"})
public final class tNodeRoot extends TableX<Integer, Zeze.Builtin.Timer.BNodeRoot>
        implements TableReadOnly<Integer, Zeze.Builtin.Timer.BNodeRoot, Zeze.Builtin.Timer.BNodeRootReadOnly> {
    public tNodeRoot() {
        super("Zeze_Builtin_Timer_tNodeRoot");
    }

    @Override
    public int getId() {
        return 1952520306;
    }

    @Override
    public boolean isMemory() {
        return false;
    }

    @Override
    public boolean isAutoKey() {
        return false;
    }

    public static final int VAR_HeadNodeId = 1;
    public static final int VAR_TailNodeId = 2;
    public static final int VAR_LoadSerialNo = 3;

    @Override
    public Integer decodeKey(ByteBuffer _os_) {
        int _v_;
        _v_ = _os_.ReadInt();
        return _v_;
    }

    @Override
    public ByteBuffer encodeKey(Integer _v_) {
        ByteBuffer _os_ = ByteBuffer.Allocate(16);
        _os_.WriteInt(_v_);
        return _os_;
    }

    @Override
    public Integer decodeKeyResultSet(java.sql.ResultSet rs) throws java.sql.SQLException {
        int _v_;
        _v_ = rs.getInt("__key");
        return _v_;
    }

    @Override
    public void encodeKeySQLStatement(Zeze.Serialize.SQLStatement st, Integer _v_) {
        st.appendInt("__key", _v_);
    }

    @Override
    public Zeze.Builtin.Timer.BNodeRoot newValue() {
        return new Zeze.Builtin.Timer.BNodeRoot();
    }

    @Override
    public Zeze.Builtin.Timer.BNodeRootReadOnly getReadOnly(Integer key) {
        return get(key);
    }
}
