// auto-generated @formatter:off
package Zeze.Builtin.GlobalCacheManagerWithRaft;

public interface BReduceParamReadOnly {
    public long typeId();
    public void encode(Zeze.Serialize.ByteBuffer _o_);
    public boolean negativeCheck();
    public BReduceParam copy();

    public Zeze.Net.Binary getGlobalKey();
    public int getState();
}
