// auto-generated @formatter:off
package Game.Buf;

import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.TableX;
import Zeze.Transaction.TableReadOnly;
import org.jspecify.annotations.Nullable;

@SuppressWarnings({"ConstantConditions", "ConstantValue", "DuplicateBranchesInSwitch", "RedundantSuppression"})
public final class tbufs extends TableX<Long, Game.Buf.BBufs>
        implements TableReadOnly<Long, Game.Buf.BBufs, Game.Buf.BBufsReadOnly> {
    public tbufs() {
        super(1844149833, "Game_Buf_tbufs");
    }

    public tbufs(String _s_) {
        super(1844149833, "Game_Buf_tbufs", _s_);
    }

    @Override
    public Class<Long> getKeyClass() {
        return Long.class;
    }

    @Override
    public Class<Game.Buf.BBufs> getValueClass() {
        return Game.Buf.BBufs.class;
    }

    public static final int VAR_Bufs = 1;

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
    public Game.Buf.BBufs newValue() {
        return new Game.Buf.BBufs();
    }

    @Override
    public @Nullable BBufsReadOnly getReadOnly(Long _k_) {
        return get(_k_);
    }
}
