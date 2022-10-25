// auto-generated @formatter:off
package Zeze.Builtin.Game.Online;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"DuplicateBranchesInSwitch", "RedundantSuppression"})
public final class taccount extends Zeze.Transaction.TableX<String, Zeze.Builtin.Game.Online.BAccount, Zeze.Builtin.Game.Online.BAccountReadOnly> {
    public taccount() {
        super("Zeze_Builtin_Game_Online_taccount");
    }

    @Override
    public int getId() {
        return -1097404497;
    }

    @Override
    public boolean isMemory() {
        return false;
    }

    @Override
    public boolean isAutoKey() {
        return false;
    }

    public static final int VAR_Name = 1;
    public static final int VAR_Roles = 2;
    public static final int VAR_LastLoginRoleId = 3;
    public static final int VAR_LastLoginVersion = 4;

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
    public Zeze.Builtin.Game.Online.BAccount newValue() {
        return new Zeze.Builtin.Game.Online.BAccount();
    }

    @Override
    public Zeze.Builtin.Game.Online.BAccountReadOnly getReadOnly(String k) {
        return get(k);
    }
}
