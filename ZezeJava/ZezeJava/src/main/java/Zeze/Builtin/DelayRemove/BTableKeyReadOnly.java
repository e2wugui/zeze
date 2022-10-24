// auto-generated @formatter:off
package Zeze.Builtin.DelayRemove;

public interface BTableKeyReadOnly {
    public long typeId();
    public void encode(Zeze.Serialize.ByteBuffer _o_);
    public boolean negativeCheck();
    public BTableKey copy();

    public String getTableName();
    public Zeze.Net.Binary getEncodedKey();
    public long getEnqueueTime();
}
