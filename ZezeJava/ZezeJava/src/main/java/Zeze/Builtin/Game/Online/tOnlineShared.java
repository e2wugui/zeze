// auto-generated @formatter:off
package Zeze.Builtin.Game.Online;

import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.TableX;
import Zeze.Transaction.TableReadOnly;

// 登录相关状态的大版本共享的持久化表, 表名为"Zeze_Game_Online_tOnlineShared__{onlineSetName}", key是角色ID
@SuppressWarnings({"DuplicateBranchesInSwitch", "NullableProblems", "RedundantSuppression"})
public final class tOnlineShared extends TableX<Long, Zeze.Builtin.Game.Online.BOnlineShared>
        implements TableReadOnly<Long, Zeze.Builtin.Game.Online.BOnlineShared, Zeze.Builtin.Game.Online.BOnlineSharedReadOnly> {
    public tOnlineShared() {
        super(-447489428, "Zeze_Builtin_Game_Online_tOnlineShared");
    }

    public tOnlineShared(String _s_) {
        super(-447489428, "Zeze_Builtin_Game_Online_tOnlineShared", _s_);
    }

    @Override
    public Class<Long> getKeyClass() {
        return Long.class;
    }

    @Override
    public Class<Zeze.Builtin.Game.Online.BOnlineShared> getValueClass() {
        return Zeze.Builtin.Game.Online.BOnlineShared.class;
    }

    public static final int VAR_Account = 1;
    public static final int VAR_Link = 2;
    public static final int VAR_LoginVersion = 3;
    public static final int VAR_LogoutVersion = 4;
    public static final int VAR_UserData = 5;

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
    public Zeze.Builtin.Game.Online.BOnlineShared newValue() {
        return new Zeze.Builtin.Game.Online.BOnlineShared();
    }

    @Override
    public Zeze.Builtin.Game.Online.BOnlineSharedReadOnly getReadOnly(Long _k_) {
        return get(_k_);
    }
}
