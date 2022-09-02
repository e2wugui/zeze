// auto-generated @formatter:off
package Zeze.Builtin.AutoKey;

import Zeze.Serialize.ByteBuffer;

@SuppressWarnings({"DuplicateBranchesInSwitch", "RedundantSuppression"})
public final class tAutoKeys extends Zeze.Transaction.TableX<Zeze.Builtin.AutoKey.BSeedKey, Zeze.Builtin.AutoKey.BAutoKey> {
    public tAutoKeys() {
        super("Zeze_Builtin_AutoKey_tAutoKeys");
    }

    @Override
    public int getId() {
        return -716529252;
    }

    @Override
    public boolean isMemory() {
        return false;
    }

    @Override
    public boolean isAutoKey() {
        return false;
    }

    public static final int VAR_NextId = 1;

    @Override
    public Zeze.Builtin.AutoKey.BSeedKey DecodeKey(ByteBuffer _os_) {
        Zeze.Builtin.AutoKey.BSeedKey _v_ = new Zeze.Builtin.AutoKey.BSeedKey();
        _v_.Decode(_os_);
        return _v_;
    }

    @Override
    public ByteBuffer EncodeKey(Zeze.Builtin.AutoKey.BSeedKey _v_) {
        ByteBuffer _os_ = ByteBuffer.Allocate(16);
        _v_.Encode(_os_);
        return _os_;
    }

    @Override
    public Zeze.Builtin.AutoKey.BAutoKey NewValue() {
        return new Zeze.Builtin.AutoKey.BAutoKey();
    }
}
