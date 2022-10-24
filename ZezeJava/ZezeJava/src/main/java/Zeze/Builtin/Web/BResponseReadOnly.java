// auto-generated @formatter:off
package Zeze.Builtin.Web;

public interface BResponseReadOnly {
    public long typeId();
    public void encode(Zeze.Serialize.ByteBuffer _o_);
    public boolean negativeCheck();
    public BResponse copy();

    public int getCode();
    public Zeze.Transaction.Collections.PMap2ReadOnly<String, Zeze.Builtin.Web.BHeader, Zeze.Builtin.Web.BHeaderReadOnly> getHeadersReadOnly();
    public Zeze.Net.Binary getBody();
    public boolean isFinish();
    public String getMessage();
    public String getStacktrace();
}
