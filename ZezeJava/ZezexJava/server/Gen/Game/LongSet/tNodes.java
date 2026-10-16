// auto-generated @formatter:off
package Game.LongSet;

import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.TableX;
import Zeze.Transaction.TableReadOnly;
import org.jspecify.annotations.Nullable;

import Game.LongSet.BNodeReadOnly;
@SuppressWarnings({"ConstantConditions", "ConstantValue", "DuplicateBranchesInSwitch", "RedundantSuppression"})
public final class tNodes extends TableX<Long, Game.LongSet.BNode>
        implements TableReadOnly<Long, Game.LongSet.BNode, Game.LongSet.BNodeReadOnly> {
    public tNodes() {
        super(-391612437, "Game_LongSet_tNodes");
    }

    public tNodes(String _s_) {
        super(-391612437, "Game_LongSet_tNodes", _s_);
    }

    @Override
    public Class<Long> getKeyClass() {
        return Long.class;
    }

    @Override
    public Class<Game.LongSet.BNode> getValueClass() {
        return Game.LongSet.BNode.class;
    }

    public static final int VAR_PrevNodeId = 1;
    public static final int VAR_NextNodeId = 2;
    public static final int VAR_Set = 3;

    @Override
    public Long decodeKey(ByteBuffer _os_) {
        long _v_;
        _v_ = _os_.ReadLong();
        return _v_;
    }

    @Override
    public ByteBuffer encodeKey(Long _v_) {
        ByteBuffer _os_ = ByteBuffer.Allocate(ByteBuffer.WriteLongSize(_v_));
        _os_.WriteLong(_v_);
        return _os_;
    }

    @Override
    public Long decodeKeyResultSet(java.sql.ResultSet _s_) throws java.sql.SQLException {
        long _v_;
        _v_ = _s_.getLong("__key");
        return _v_;
    }

    @Override
    public void encodeKeySQLStatement(Zeze.Serialize.SQLStatement _s_, Long _v_) {
        _s_.appendLong("__key", _v_);
    }

    @Override
    public Game.LongSet.BNode newValue() {
        return new Game.LongSet.BNode();
    }

    @Override
    public @Nullable BNodeReadOnly getReadOnly(Long _k_) {
        return get(_k_);
    }
}
