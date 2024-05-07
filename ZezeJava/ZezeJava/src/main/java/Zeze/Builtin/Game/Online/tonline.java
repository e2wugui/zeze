// auto-generated @formatter:off
package Zeze.Builtin.Game.Online;

import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.TableX;
import Zeze.Transaction.TableReadOnly;

// tables
@SuppressWarnings({"DuplicateBranchesInSwitch", "NullableProblems", "RedundantSuppression"})
public final class tonline extends TableX<Long, Zeze.Builtin.Game.Online.BOnline>
        implements TableReadOnly<Long, Zeze.Builtin.Game.Online.BOnline, Zeze.Builtin.Game.Online.BOnlineReadOnly> {
    public tonline() {
        super(-1571889602, "Zeze_Builtin_Game_Online_tonline");
    }

    public tonline(String suffix) {
        super(-1571889602, "Zeze_Builtin_Game_Online_tonline", suffix);
    }

    @Override
    public Class<Long> getKeyClass() {
        return Long.class;
    }

    @Override
    public Class<Zeze.Builtin.Game.Online.BOnline> getValueClass() {
        return Zeze.Builtin.Game.Online.BOnline.class;
    }

    public static final int VAR_Link = 3;
    public static final int VAR_LoginVersion = 4;
    public static final int VAR_ReliableNotifyMark = 5;
    public static final int VAR_ReliableNotifyConfirmIndex = 6;
    public static final int VAR_ReliableNotifyIndex = 7;
    public static final int VAR_ServerId = 8;
    public static final int VAR_LogoutVersion = 9;
    public static final int VAR_UserData = 10;
    public static final int VAR_Account = 11;

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
    public Long decodeKeyResultSet(java.sql.ResultSet rs) throws java.sql.SQLException {
        long _v_;
        _v_ = rs.getLong("__key");
        return _v_;
    }

    @Override
    public void encodeKeySQLStatement(Zeze.Serialize.SQLStatement st, Long _v_) {
        st.appendLong("__key", _v_);
    }

    @Override
    public Zeze.Builtin.Game.Online.BOnline newValue() {
        return new Zeze.Builtin.Game.Online.BOnline();
    }

    @Override
    public Zeze.Builtin.Game.Online.BOnlineReadOnly getReadOnly(Long key) {
        return get(key);
    }
}
