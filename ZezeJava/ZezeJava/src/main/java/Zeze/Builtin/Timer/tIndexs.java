// auto-generated @formatter:off
package Zeze.Builtin.Timer;

import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.TableX;
import Zeze.Transaction.TableReadOnly;

// timer索引, key是timerId(用户指定的,或"@"+Base64编码的自动分配ID)
@SuppressWarnings({"DuplicateBranchesInSwitch", "NullableProblems", "RedundantSuppression"})
public final class tIndexs extends TableX<String, Zeze.Builtin.Timer.BIndex>
        implements TableReadOnly<String, Zeze.Builtin.Timer.BIndex, Zeze.Builtin.Timer.BIndexReadOnly> {
    public tIndexs() {
        super(833718, "Zeze_Builtin_Timer_tIndexs");
    }

    public tIndexs(String _s_) {
        super(833718, "Zeze_Builtin_Timer_tIndexs", _s_);
    }

    @Override
    public Class<String> getKeyClass() {
        return String.class;
    }

    @Override
    public Class<Zeze.Builtin.Timer.BIndex> getValueClass() {
        return Zeze.Builtin.Timer.BIndex.class;
    }

    public static final int VAR_ServerId = 1;
    public static final int VAR_NodeId = 2;
    public static final int VAR_SerialId = 3;
    public static final int VAR_Version = 4;

    @Override
    public String decodeKey(ByteBuffer _os_) {
        String _v_;
        _v_ = _os_.ReadString();
        return _v_;
    }

    @Override
    public ByteBuffer encodeKey(String _v_) {
        ByteBuffer _os_ = ByteBuffer.Allocate(16);
        _os_.WriteString(_v_);
        return _os_;
    }

    @Override
    public String decodeKeyResultSet(java.sql.ResultSet _s_) throws java.sql.SQLException {
        String _v_;
        _v_ = _s_.getString("__key");
        if (_v_ == null)
            _v_ = "";
        return _v_;
    }

    @Override
    public void encodeKeySQLStatement(Zeze.Serialize.SQLStatement _s_, String _v_) {
        _s_.appendString("__key", _v_);
    }

    @Override
    public Zeze.Builtin.Timer.BIndex newValue() {
        return new Zeze.Builtin.Timer.BIndex();
    }

    @Override
    public Zeze.Builtin.Timer.BIndexReadOnly getReadOnly(String _k_) {
        return get(_k_);
    }
}
