// auto-generated @formatter:off
package metagame.builtin.TaskModule;

public interface BConditionReadOnly {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BCondition copy();

    String getClassName();
    Zeze.Net.Binary getParameter();
}
