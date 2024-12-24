// auto-generated @formatter:off
package Zeze.Transaction.GTable;

public interface BeanMap1ReadOnly<K extends Comparable<K>, V> {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BeanMap1<K, V> copy();

    Zeze.Transaction.Collections.PMap1ReadOnly<K, V> getMap1ReadOnly();
}
