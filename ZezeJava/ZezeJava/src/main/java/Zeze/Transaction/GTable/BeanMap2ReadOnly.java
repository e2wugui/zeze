package Zeze.Transaction.GTable;

import Zeze.Transaction.Bean;

/** {@link BeanMap2} 的只读视图接口。 */
public interface BeanMap2ReadOnly<K, V extends Bean, VReadOnly> {
    long typeId();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BeanMap2<K, V, VReadOnly> copy();

    Zeze.Transaction.Collections.PMap2ReadOnly<K, V, VReadOnly> getMap2ReadOnly();
}
