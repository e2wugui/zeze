// auto-generated @formatter:off
package Game.LongSet;

import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.TableX;
import Zeze.Transaction.TableReadOnly;
import org.jspecify.annotations.Nullable;

@SuppressWarnings({"ConstantConditions", "ConstantValue", "DuplicateBranchesInSwitch", "RedundantSuppression"})
public final class tNodeRoot extends TableX<String, Game.LongSet.BNodeRoot>
        implements TableReadOnly<String, Game.LongSet.BNodeRoot, Game.LongSet.BNodeRootReadOnly> {
    public tNodeRoot() {
        super(-281247304, "Game_LongSet_tNodeRoot");
    }

    public tNodeRoot(String _s_) {
        super(-281247304, "Game_LongSet_tNodeRoot", _s_);
    }

    @Override
    public Class<String> getKeyClass() {
        return String.class;
    }

    @Override
    public Class<Game.LongSet.BNodeRoot> getValueClass() {
        return Game.LongSet.BNodeRoot.class;
    }

    public static final int VAR_HeadNodeId = 1;
    public static final int VAR_TailNodeId = 2;

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
    public Game.LongSet.BNodeRoot newValue() {
        return new Game.LongSet.BNodeRoot();
    }

    @Override
    public @Nullable BNodeRootReadOnly getReadOnly(String _k_) {
        return get(_k_);
    }
}
