// auto-generated @formatter:off
package metagame.builtin.World;

import org.jspecify.annotations.Nullable;
import Zeze.Transaction.TableKey;

// 一个具体的操作。
public interface BCommandReadOnly {
    long typeId();
    int preAllocSize();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_);
    boolean negativeCheck();
    BCommand copy();
    BCommand.Data toData();
    void buildString(StringBuilder _s_, int _l_);
    long objectId();
    int variableId();
    @Nullable TableKey tableKey();
    boolean isManaged();
    java.util.ArrayList<Zeze.Transaction.VariableMeta> variables();

    long getMapInstanceId();
    int getCommandId();
    Zeze.Net.Binary getParam();
}
