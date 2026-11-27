// auto-generated @formatter:off
package metagame.builtin.World.Static;

import org.jspecify.annotations.Nullable;
import Zeze.Transaction.TableKey;

public interface BSwitchWorldReadOnly {
    long typeId();
    int preAllocSize();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_);
    boolean negativeCheck();
    BSwitchWorld copy();
    BSwitchWorld.Data toData();
    void buildString(StringBuilder _s_, int _l_);
    long objectId();
    int variableId();
    @Nullable TableKey tableKey();
    boolean isManaged();
    java.util.ArrayList<Zeze.Transaction.VariableMeta> variables();

    int getMapId();
    int getFromMapId();
    int getFromGateId();
}
