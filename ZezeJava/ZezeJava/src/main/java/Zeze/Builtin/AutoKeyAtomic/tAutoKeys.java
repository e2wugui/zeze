// auto-generated @formatter:off
package Zeze.Builtin.AutoKeyAtomic;

import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.TableX;
import Zeze.Transaction.TableReadOnly;

@SuppressWarnings({"DuplicateBranchesInSwitch", "RedundantSuppression"})
public final class tAutoKeys extends TableX<Zeze.Builtin.AutoKeyAtomic.BSeedKey, Zeze.Builtin.AutoKeyAtomic.BAutoKey>
        implements TableReadOnly<Zeze.Builtin.AutoKeyAtomic.BSeedKey, Zeze.Builtin.AutoKeyAtomic.BAutoKey, Zeze.Builtin.AutoKeyAtomic.BAutoKeyReadOnly> {
    public tAutoKeys() {
        super("Zeze_Builtin_AutoKeyAtomic_tAutoKeys");
    }

    @Override
    public int getId() {
        return -1567221937;
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
    public Zeze.Builtin.AutoKeyAtomic.BSeedKey decodeKey(ByteBuffer _os_) {
        Zeze.Builtin.AutoKeyAtomic.BSeedKey _v_ = new Zeze.Builtin.AutoKeyAtomic.BSeedKey();
        _v_.decode(_os_);
        return _v_;
    }

    @Override
    public ByteBuffer encodeKey(Zeze.Builtin.AutoKeyAtomic.BSeedKey _v_) {
        ByteBuffer _os_ = ByteBuffer.Allocate(16);
        _v_.encode(_os_);
        return _os_;
    }

    @Override
    public Zeze.Builtin.AutoKeyAtomic.BAutoKey newValue() {
        return new Zeze.Builtin.AutoKeyAtomic.BAutoKey();
    }

    @Override
    public Zeze.Builtin.AutoKeyAtomic.BAutoKeyReadOnly getReadOnly(Zeze.Builtin.AutoKeyAtomic.BSeedKey key) {
        return get(key);
    }
}
