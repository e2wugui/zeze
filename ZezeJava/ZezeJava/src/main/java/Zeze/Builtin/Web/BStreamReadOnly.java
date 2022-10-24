// auto-generated @formatter:off
package Zeze.Builtin.Web;

public interface BStreamReadOnly {
    public long typeId();
    public void encode(Zeze.Serialize.ByteBuffer _o_);
    public boolean negativeCheck();
    public BStream copy();

    public long getExchangeId();
    public Zeze.Net.Binary getBody();
    public boolean isFinish();
}
