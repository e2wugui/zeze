// auto-generated @formatter:off
package Zeze.Builtin.Game.Online;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"DuplicateBranchesInSwitch", "RedundantSuppression"})
public final class tversion extends Zeze.Transaction.TableX<Long, Zeze.Builtin.Game.Online.BVersion> {
    public tversion() {
        super("Zeze_Builtin_Game_Online_tversion");
    }

    @Override
    public boolean isMemory() {
        return false;
    }

    @Override
    public boolean isAutoKey() {
        return false;
    }

    public static final int VAR_All = 0;
    public static final int VAR_LoginVersion = 1;
    public static final int VAR_ReliableNotifyMark = 2;
    public static final int VAR_ReliableNotifyQueue = 3;
    public static final int VAR_ReliableNotifyConfirmCount = 4;
    public static final int VAR_ReliableNotifyTotalCount = 5;
    public static final int VAR_ServerId = 6;

    @Override
    public Long DecodeKey(ByteBuffer _os_) {
        long _v_;
        _v_ = _os_.ReadLong();
        return _v_;
    }

    @Override
    public ByteBuffer EncodeKey(Long _v_) {
        ByteBuffer _os_ = ByteBuffer.Allocate(16);
        _os_.WriteLong(_v_);
        return _os_;
    }

    @Override
    public Zeze.Builtin.Game.Online.BVersion NewValue() {
        return new Zeze.Builtin.Game.Online.BVersion();
    }

    @Override
    public Zeze.Transaction.ChangeVariableCollector CreateChangeVariableCollector(int variableId) {
        switch (variableId) {
            case 0: return new Zeze.Transaction.ChangeVariableCollectorChanged();
            case 1: return new Zeze.Transaction.ChangeVariableCollectorChanged();
            case 2: return new Zeze.Transaction.ChangeVariableCollectorSet();
            case 3: return new Zeze.Transaction.ChangeVariableCollectorChanged();
            case 4: return new Zeze.Transaction.ChangeVariableCollectorChanged();
            case 5: return new Zeze.Transaction.ChangeVariableCollectorChanged();
            case 6: return new Zeze.Transaction.ChangeVariableCollectorChanged();
            default: return null;
        }
    }
}
