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
    void encode(Zeze.Serialize.ByteBuffer _o_);
    boolean negativeCheck();
    BLocateBucket copy();

    String getDatabase();
    String getTable();
    Zeze.Net.Binary getKey();
}
