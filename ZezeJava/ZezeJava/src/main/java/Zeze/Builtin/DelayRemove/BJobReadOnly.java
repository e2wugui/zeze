// auto-generated @formatter:off
package Zeze.Builtin.DelayRemove;

public interface BJobReadOnly {
    public long typeId();
    public void encode(Zeze.Serialize.ByteBuffer _o_);
    public boolean negativeCheck();
    public BJob copy();

    public String getJobHandleName();
    public Zeze.Net.Binary getJobState();
}
