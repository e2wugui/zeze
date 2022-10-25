// auto-generated @formatter:off
package Zeze.Builtin.Game.Online;

import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.TableX;
import Zeze.Transaction.TableReadOnly;

@SuppressWarnings({"DuplicateBranchesInSwitch", "RedundantSuppression"})
public final class tonline extends TableX<Long, Zeze.Builtin.Game.Online.BOnline>
        implements TableReadOnly<Long, Zeze.Builtin.Game.Online.BOnline, Zeze.Builtin.Game.Online.BOnlineReadOnly> {
    public tonline() {
        super("Zeze_Builtin_Game_Online_tonline");
    }

    @Override
    public int getId() {
        return -1571889602;
    }

    @Override
    public boolean isMemory() {
        return false;
    }

    @Override
    public boolean isAutoKey() {
        return false;
    }

    public static final int VAR_LinkName = 1;
    public static final int VAR_LinkSid = 2;

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
    public Zeze.Builtin.Game.Online.BOnline newValue() {
        return new Zeze.Builtin.Game.Online.BOnline();
    }

    @Override
    public Zeze.Builtin.Game.Online.BOnlineReadOnly getReadOnly(Long key) {
        return get(key);
    }
}
