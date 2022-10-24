// auto-generated @formatter:off
package Zeze.Builtin.Provider;

public interface BBroadcastReadOnly {
    public long typeId();
    public void encode(Zeze.Serialize.ByteBuffer _o_);
    public boolean negativeCheck();
    public BBroadcast copy();

    public long getProtocolType();
    public Zeze.Net.Binary getProtocolWholeData();
    public int getTime();
}
