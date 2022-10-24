// auto-generated @formatter:off
package Zeze.Builtin.Online;

public interface BLocalsReadOnly {
    public long typeId();
    public void encode(Zeze.Serialize.ByteBuffer _o_);
    public boolean negativeCheck();
    public BLocals copy();

    public Zeze.Transaction.Collections.PMap2ReadOnly<String, Zeze.Builtin.Online.BLocal, Zeze.Builtin.Online.BLocalReadOnly> getLoginsReadOnly();
}
