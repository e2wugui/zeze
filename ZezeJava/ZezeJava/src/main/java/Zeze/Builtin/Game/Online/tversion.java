// auto-generated @formatter:off
package Zeze.Builtin.Game.Online;

import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.TableX;
import Zeze.Transaction.TableReadOnly;

@SuppressWarnings({"DuplicateBranchesInSwitch", "RedundantSuppression"})
public final class tversion extends TableX<Long, Zeze.Builtin.Game.Online.BVersion>
        implements TableReadOnly<Long, Zeze.Builtin.Game.Online.BVersion, Zeze.Builtin.Game.Online.BVersionReadOnly> {
    public tversion() {
        super("Zeze_Builtin_Game_Online_tversion");
    }

    public tversion(String suffix) {
        super("Zeze_Builtin_Game_Online_tversion" + suffix);
    }

    public String getOriginName() {
        return "Zeze_Builtin_Game_Online_tversion";
    }

    @Override
    public int getId() {
        return -1673876055;
    }

    public static final int VAR_LoginVersion = 1;
    public static final int VAR_ReliableNotifyMark = 2;
    public static final int VAR_ReliableNotifyConfirmIndex = 3;
    public static final int VAR_ReliableNotifyIndex = 4;
    public static final int VAR_ServerId = 5;
    public static final int VAR_LogoutVersion = 6;
    public static final int VAR_UserData = 7;

    @Override
    public Long decodeKey(ByteBuffer _os_) {
        long _v_;
        _v_ = _os_.ReadLong();
        return _v_;
    }

    @Override
    public ByteBuffer encodeKey(Long _v_) {
        ByteBuffer _os_ = ByteBuffer.Allocate(16);
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
    public Zeze.Builtin.Game.Online.BVersion newValue() {
        return new Zeze.Builtin.Game.Online.BVersion();
    }

    @Override
    public Zeze.Builtin.Game.Online.BVersionReadOnly getReadOnly(Long key) {
        return get(key);
    }
}
