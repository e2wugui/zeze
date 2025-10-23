// auto-generated @formatter:off
package Zeze.Builtin.Dbh2.Master;

/*
				Dbh2发现桶没找到错误时，使用GetBuckets得到完整的信息。
				因为只LocateBucket最新的桶信息虽然能用，但是出现桶没找到错误时，通常意味着前一个桶的信息也需要更新。
				不更新旧桶，桶的定位方法可以工作（只依赖桶的KeyFirst），但感觉不好。
				所以LocateBucket先不用，仅使用GetBuckets。
*/
public interface BLocateBucketReadOnly {
    long typeId();
    int preAllocSize();
    void encode(Zeze.Serialize.ByteBuffer _o_);
    void encodeSQLStatement(java.util.ArrayList<String> _p_, Zeze.Serialize.SQLStatement _s_);
    boolean negativeCheck();
    BLocateBucket copy();
    BLocateBucket.Data toData();
    void buildString(StringBuilder _s_, int _l_);
    long objectId();
    int variableId();
    Zeze.Transaction.TableKey tableKey();
    boolean isManaged();
    java.util.ArrayList<Zeze.Builtin.HotDistribute.BVariable.Data> variables();

    String getDatabase();
    String getTable();
    Zeze.Net.Binary getKey();
}
