// auto-generated @formatter:off
package Zeze.Builtin.Game.Rank;

public interface BRankValueReadOnly {
    public long typeId();
    public void encode(Zeze.Serialize.ByteBuffer _o_);
    public boolean negativeCheck();
    public BRankValue copy();

    public long getRoleId();
    public long getValue();
    public Zeze.Net.Binary getValueEx();
}
