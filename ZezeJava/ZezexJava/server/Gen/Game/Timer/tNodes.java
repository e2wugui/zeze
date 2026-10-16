// auto-generated @formatter:off
package Game.Timer;

import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.TableX;
import Zeze.Transaction.TableReadOnly;
import org.jspecify.annotations.Nullable;

import Game.Timer.BNodeReadOnly;
// key is nodeId, alloc by AutoKey("Game.Timer.NodeIdGenerator")
@SuppressWarnings({"ConstantConditions", "ConstantValue", "DuplicateBranchesInSwitch", "RedundantSuppression"})
public final class tNodes extends TableX<Long, Game.Timer.BNode>
        implements TableReadOnly<Long, Game.Timer.BNode, Game.Timer.BNodeReadOnly> {
    public tNodes() {
        super(916100549, "Game_Timer_tNodes");
    }

    public tNodes(String _s_) {
        super(916100549, "Game_Timer_tNodes", _s_);
    }

    @Override
    public Class<Long> getKeyClass() {
        return Long.class;
    }

    @Override
    public Class<Game.Timer.BNode> getValueClass() {
        return Game.Timer.BNode.class;
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
    public Game.Timer.BNode newValue() {
        return new Game.Timer.BNode();
    }

    @Override
    public @Nullable BNodeReadOnly getReadOnly(Long _k_) {
        return get(_k_);
    }
}
