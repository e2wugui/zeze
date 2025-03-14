// auto-generated @formatter:off
package Zeze.Builtin.Game.Online;

import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.TableX;
import Zeze.Transaction.TableReadOnly;

// 每个server各自保存不同表的模板表, 表名为"Zeze_Game_Online_tlocal__{onlineSetName}__{serverId}", key是角色ID
@SuppressWarnings({"DuplicateBranchesInSwitch", "NullableProblems", "RedundantSuppression"})
public final class tlocal extends TableX<Long, Zeze.Builtin.Game.Online.BLocal>
        implements TableReadOnly<Long, Zeze.Builtin.Game.Online.BLocal, Zeze.Builtin.Game.Online.BLocalReadOnly> {
    public tlocal() {
        super(-1657900798, "Zeze_Builtin_Game_Online_tlocal");
    }

    public tlocal(String _s_) {
        super(-1657900798, "Zeze_Builtin_Game_Online_tlocal", _s_);
    }

    @Override
    public Class<Long> getKeyClass() {
        return Long.class;
    }

    @Override
    public Class<Zeze.Builtin.Game.Online.BLocal> getValueClass() {
        return Zeze.Builtin.Game.Online.BLocal.class;
    }

    public static final int VAR_LoginVersion = 1;
    public static final int VAR_Datas = 2;
    public static final int VAR_Link = 3;

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
    public Zeze.Builtin.Game.Online.BLocal newValue() {
        return new Zeze.Builtin.Game.Online.BLocal();
    }

    @Override
    public Zeze.Builtin.Game.Online.BLocalReadOnly getReadOnly(Long _k_) {
        return get(_k_);
    }
}
