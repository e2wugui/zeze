package Zeze.Transaction.Collections;

import ConfCsDynamicLogs.Demo.Detail;
import Zeze.Serialize.ByteBuffer;
import Zeze.Transaction.Changes;

public final class LogOneFixture {
    private LogOneFixture() {
    }

    public static byte[] encodeChildEdit(ByteBuffer key, Changes.Record record, int variableId) {
        if (!(record.getVariableLog(variableId) instanceof LogOneValue<?> captured) || captured.logBean == null)
            throw new AssertionError("expected a captured LogOne child edit");

        // The current transaction writer also carries the complete Detail, which takes precedence
        // in encode. Exercise the child-only layout using the actual captured child and Java codec.
        var childOnly = new LogOneValue<>(variableId, LogOneMeta.get(Detail.class), captured.getThis());
        childOnly.logBean = captured.logBean;
        var root = new LogBean(0, null);
        root.getVariablesOrNew().put(variableId, childOnly);

        var bb = ByteBuffer.Allocate();
        bb.WriteString("orders");
        bb.WriteByteBuffer(key);
        bb.WriteUInt(Changes.Record.Edit);
        bb.WriteUInt(1);
        root.encode(bb);
        return bb.Copy();
    }
}
