// auto-generated @formatter:off
package Zeze.Builtin.Game.Online;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"DuplicateBranchesInSwitch", "RedundantSuppression"})
public final class tlocal extends Zeze.Transaction.TableX<Long, Zeze.Builtin.Game.Online.BLocal> {
    public tlocal() {
        super("Zeze_Builtin_Game_Online_tlocal");
    }

    @Override
    public int getId() {
        return -1657900798;
    }

    @Override
    public boolean isMemory() {
        return true;
    }

    @Override
    public boolean isAutoKey() {
        return false;
    }

    public static final int VAR_LoginVersion = 1;
    public static final int VAR_Datas = 2;

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
    public Zeze.Builtin.Game.Online.BLocal newValue() {
        return new Zeze.Builtin.Game.Online.BLocal();
    }
}
