package Zeze.Transaction.Collections;

import java.util.Map;
import java.util.Set;
import Zeze.Serialize.ByteBuffer;
import Zeze.Serialize.IByteBuffer;
import Zeze.Transaction.Bean;
import Zeze.Transaction.EmptyBean;
import Zeze.Transaction.Record;
import Zeze.Transaction.TableKey;
import harness.Fast;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

@Fast
public class TestMapValueKeyOwnership {
    public static class Value extends Bean {
        private @Nullable Object key;

        public Value() {
        }

        @Override
        public @Nullable Object mapKey() {
            return key;
        }

        @Override
        public void mapKey(Object value) {
            key = value;
        }

        @Override
        public void encode(ByteBuffer bb) {
            bb.WriteByte(0);
        }

        @Override
        public void decode(IByteBuffer bb) {
            bb.ReadByte();
        }
    }

    private static void attach(Bean map, boolean redo) {
        var owner = new EmptyBean();
        var root = new Record.RootInfo(null, new TableKey(123, 1L));
        owner.initRootInfo(root, null);
        map.variableId(1);
        if (redo)
            map.initRootInfoWithRedo(root, owner);
        else
            map.initRootInfo(root, owner);
    }

    private static void verifyChangeKey(Bean map, Value value) {
        var log = (LogMap2Base<?, ?, ?>)map.createLogBean(null);
        var child = new LogBean(0, value);
        log.addChild(map, child);
        assertEquals(Set.of(1), log.getChangedWithKey().keySet());
        assertSame(child, log.getChangedWithKey().get(1));
    }

    @Test
    public void transientMapsPreserveManagedValueKey() {
        var map = new PMap2<>(Integer.class, Value.class);
        var value = new Value();
        map.put(1, value);
        attach(map, false);

        var alias = new PMap2<>(Integer.class, Value.class);
        alias.put(2, value);
        assertEquals(1, value.mapKey());
        alias.putAll(Map.of(3, value));
        assertEquals(1, value.mapKey());
        assertSame(map, value.parent());
        verifyChangeKey(map, value);
    }

    @Test
    public void transientSortedMapsPreserveManagedValueKey() {
        var map = new PSortedMap2<>(Integer.class, Value.class);
        var value = new Value();
        map.put(1, value);
        attach(map, false);

        var alias = new PSortedMap2<>(Integer.class, Value.class);
        alias.put(2, value);
        assertEquals(1, value.mapKey());
        alias.putAll(Map.of(3, value));
        assertEquals(1, value.mapKey());
        assertSame(map, value.parent());
        verifyChangeKey(map, value);
    }

    @Test
    public void mapAttachmentBindsTheActualOwnerKey() {
        for (boolean redo : new boolean[]{false, true}) {
            var map = new PMap2<>(Integer.class, Value.class);
            var alias = new PMap2<>(Integer.class, Value.class);
            var value = new Value();
            map.put(1, value);
            alias.putAll(Map.of(2, value));
            attach(map, redo);
            assertEquals(1, value.mapKey());
            verifyChangeKey(map, value);
        }
    }

    @Test
    public void sortedMapAttachmentBindsTheActualOwnerKey() {
        for (boolean redo : new boolean[]{false, true}) {
            var map = new PSortedMap2<>(Integer.class, Value.class);
            var alias = new PSortedMap2<>(Integer.class, Value.class);
            var value = new Value();
            map.put(1, value);
            alias.putAll(Map.of(2, value));
            attach(map, redo);
            assertEquals(1, value.mapKey());
            verifyChangeKey(map, value);
        }
    }
}
