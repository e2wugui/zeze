package Zeze.Transaction.GTable;

/** {@link BeanMap1} 的只读视图接口。 */
public interface BeanMap1ReadOnly<K, V> {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BeanMap1<K, V> copy();

    Zeze.Transaction.Collections.PMap1ReadOnly<K, V> getMap1ReadOnly();
}
