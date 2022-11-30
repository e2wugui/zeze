// auto-generated @formatter:off
package Zeze.Builtin.Game.TaskBase;

public interface BCollectCoinTaskReadOnly {
    public long typeId();
    public void encode(Zeze.Serialize.ByteBuffer _o_);
    public boolean negativeCheck();
    public BCollectCoinTask copy();

    public String getName();
    public long getTargetCoinCount();
    public long getCurrentCoinCount();
}
