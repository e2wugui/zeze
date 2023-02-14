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

    @Override
    public int getId() {
        return -1673876055;
    }

    @Override
    public boolean isMemory() {
        return false;
    }

    @Override
    public boolean isAutoKey() {
        return false;
    }

    public static final int VAR_LoginVersion = 1;
    public static final int VAR_ReliableNotifyMark = 2;
    public static final int VAR_ReliableNotifyConfirmIndex = 3;
    public static final int VAR_ReliableNotifyIndex = 4;
    public static final int VAR_ServerId = 5;
    public static final int VAR_UserData = 6;

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
    public Zeze.Builtin.Game.Online.BVersion newValue() {
        return new Zeze.Builtin.Game.Online.BVersion();
    }

    @Override
    public Zeze.Builtin.Game.Online.BVersionReadOnly getReadOnly(Long key) {
        return get(key);
    }
}
