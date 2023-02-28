// auto-generated @formatter:off
package Zeze.Builtin.Game.Rank;

public interface BRankValueReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BRankValue copy();

    long getRoleId();
    long getValue();
    Zeze.Net.Binary getValueEx();
}
