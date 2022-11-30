// auto-generated @formatter:off
package Zeze.Builtin.Game.TaskBase;

public interface BCollectCoinEventReadOnly {
    public long typeId();
    public void encode(Zeze.Serialize.ByteBuffer _o_);
    public boolean negativeCheck();
    public BCollectCoinEvent copy();

    public String getName();
    public long getCoinCount();
}
