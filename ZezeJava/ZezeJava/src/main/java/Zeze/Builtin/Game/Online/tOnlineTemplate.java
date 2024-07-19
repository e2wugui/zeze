// auto-generated @formatter:off
package Zeze.Builtin.Game.Online;

import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.TableX;
import Zeze.Transaction.TableReadOnly;

// 登录相关状态的大版本隔离的持久化模板表, key是角色ID
@SuppressWarnings({"DuplicateBranchesInSwitch", "NullableProblems", "RedundantSuppression"})
public final class tOnlineTemplate extends TableX<Long, Zeze.Builtin.Game.Online.BOnline>
        implements TableReadOnly<Long, Zeze.Builtin.Game.Online.BOnline, Zeze.Builtin.Game.Online.BOnlineReadOnly> {
    public tOnlineTemplate() {
        super(-175272172, "Zeze_Builtin_Game_Online_tOnlineTemplate");
    }

    public tOnlineTemplate(String _s_) {
        super(-175272172, "Zeze_Builtin_Game_Online_tOnlineTemplate", _s_);
    }

    @Override
    public Class<Long> getKeyClass() {
        return Long.class;
    }

    @Override
    public Class<Zeze.Builtin.Game.Online.BOnline> getValueClass() {
        return Zeze.Builtin.Game.Online.BOnline.class;
    }

    public static final int VAR_ServerId = 1;
    public static final int VAR_ReliableNotifyMark = 2;
    public static final int VAR_ReliableNotifyConfirmIndex = 3;
    public static final int VAR_ReliableNotifyIndex = 4;
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
    public Zeze.Builtin.Game.Online.BOnline newValue() {
        return new Zeze.Builtin.Game.Online.BOnline();
    }

    @Override
    public Zeze.Builtin.Game.Online.BOnlineReadOnly getReadOnly(Long _k_) {
        return get(_k_);
    }
}
