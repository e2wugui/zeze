// auto-generated @formatter:off
package Game.Timer;

import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.TableX;
import Zeze.Transaction.TableReadOnly;
import org.jspecify.annotations.Nullable;

// key is serverid 每一台server拥有自己的链表。
@SuppressWarnings({"ConstantConditions", "ConstantValue", "DuplicateBranchesInSwitch", "RedundantSuppression"})
public final class tNodeRoot extends TableX<Integer, Game.Timer.BNodeRoot>
        implements TableReadOnly<Integer, Game.Timer.BNodeRoot, Game.Timer.BNodeRootReadOnly> {
    public tNodeRoot() {
        super(-514040774, "Game_Timer_tNodeRoot");
    }

    public tNodeRoot(String _s_) {
        super(-514040774, "Game_Timer_tNodeRoot", _s_);
    }

    @Override
    public Class<Integer> getKeyClass() {
        return Integer.class;
    }

    @Override
    public Class<Game.Timer.BNodeRoot> getValueClass() {
        return Game.Timer.BNodeRoot.class;
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
    public Game.Timer.BNodeRoot newValue() {
        return new Game.Timer.BNodeRoot();
    }

    @Override
    public @Nullable BNodeRootReadOnly getReadOnly(Integer _k_) {
        return get(_k_);
    }
}
