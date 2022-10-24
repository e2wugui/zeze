// auto-generated @formatter:off
package Zeze.Builtin.GlobalCacheManagerWithRaft;

// rpc
public interface BAcquireParamReadOnly {
    public long typeId();
    public void encode(Zeze.Serialize.ByteBuffer _o_);
    public boolean negativeCheck();
    public BAcquireParam copy();

    public Zeze.Net.Binary getGlobalKey();
    public int getState();
}
