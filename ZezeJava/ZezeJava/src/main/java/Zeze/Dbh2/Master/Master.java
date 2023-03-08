package Zeze.Dbh2.Master;

import Zeze.Builtin.Dbh2.Master.GetBuckets;
import Zeze.Builtin.Dbh2.Master.LocateBucket;

public class Master extends AbstractMaster {
	@Override
	protected long ProcessGetBucketsRequest(GetBuckets r) throws Exception {
		return 0;
	}

	@Override
	protected long ProcessLocateBucketRequest(LocateBucket r) throws Exception {
		return 0;
	}
}
