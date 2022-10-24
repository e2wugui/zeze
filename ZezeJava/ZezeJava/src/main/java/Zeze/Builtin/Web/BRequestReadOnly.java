// auto-generated @formatter:off
package Zeze.Builtin.Web;

public interface BRequestReadOnly {
    public long typeId();
    public void encode(Zeze.Serialize.ByteBuffer _o_);
    public boolean negativeCheck();
    public BRequest copy();

    public long getExchangeId();
    public String getMethod();
    public String getPath();
    public String getQuery();
    public Zeze.Transaction.Collections.PMap2ReadOnly<String, Zeze.Builtin.Web.BHeader, Zeze.Builtin.Web.BHeaderReadOnly> getHeadersReadOnly();
    public Zeze.Net.Binary getBody();
    public boolean isFinish();
    public String getAuthedAccount();
}
