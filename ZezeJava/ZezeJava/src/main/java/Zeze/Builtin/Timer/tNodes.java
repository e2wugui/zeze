// auto-generated @formatter:off
package Zeze.Builtin.Timer;

import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.TableX;
import Zeze.Transaction.TableReadOnly;

@SuppressWarnings({"DuplicateBranchesInSwitch", "RedundantSuppression"})
public final class tNodes extends TableX<Long, Zeze.Builtin.Timer.BNode>
        implements TableReadOnly<Long, Zeze.Builtin.Timer.BNode, Zeze.Builtin.Timer.BNodeReadOnly> {
    public tNodes() {
        super("Zeze_Builtin_Timer_tNodes");
    }

    public tNodes(String suffix) {
        super("Zeze_Builtin_Timer_tNodes" + suffix);
    }

    public String getOriginName() {
        return "Zeze_Builtin_Timer_tNodes";
    }

    @Override
    public int getId() {
        return 453698467;
    }

    public static final int VAR_PrevNodeId = 1;
    public static final int VAR_NextNodeId = 2;
    public static final int VAR_Timers = 3;

    @Override
    public Long decodeKey(ByteBuffer _os_) {
        long _v_;
        _v_ = _os_.ReadLong();
        return _v_;
    }

    @Override
    public ByteBuffer encodeKey(Long _v_) {
        ByteBuffer _os_ = ByteBuffer.Allocate(16);
        _os_.WriteLong(_v_);
        return _os_;
    }

    @Override
    public Long decodeKeyResultSet(java.sql.ResultSet rs) throws java.sql.SQLException {
        long _v_;
        _v_ = rs.getLong("__key");
        return _v_;
    }

    @Override
    public void encodeKeySQLStatement(Zeze.Serialize.SQLStatement st, Long _v_) {
        st.appendLong("__key", _v_);
    }

    @Override
    public Zeze.Builtin.Timer.BNode newValue() {
        return new Zeze.Builtin.Timer.BNode();
    }

    @Override
    public Zeze.Builtin.Timer.BNodeReadOnly getReadOnly(Long key) {
        return get(key);
    }
}
