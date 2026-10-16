// auto-generated @formatter:off
package Game.LongSet;

import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.TableX;
import Zeze.Transaction.TableReadOnly;
import org.jspecify.annotations.Nullable;

// key is "{name}#{value}"
@SuppressWarnings({"ConstantConditions", "ConstantValue", "DuplicateBranchesInSwitch", "RedundantSuppression"})
public final class tIndexs extends TableX<String, Game.LongSet.BNodeId>
        implements TableReadOnly<String, Game.LongSet.BNodeId, Game.LongSet.BNodeIdReadOnly> {
    public tIndexs() {
        super(-262442665, "Game_LongSet_tIndexs");
    }

    public tIndexs(String _s_) {
        super(-262442665, "Game_LongSet_tIndexs", _s_);
    }

    @Override
    public Class<String> getKeyClass() {
        return String.class;
    }

    @Override
    public Class<Game.LongSet.BNodeId> getValueClass() {
        return Game.LongSet.BNodeId.class;
    }

    public static final int VAR_NodeId = 1;

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
    public Game.LongSet.BNodeId newValue() {
        return new Game.LongSet.BNodeId();
    }

    @Override
    public @Nullable BNodeIdReadOnly getReadOnly(String _k_) {
        return get(_k_);
    }
}
