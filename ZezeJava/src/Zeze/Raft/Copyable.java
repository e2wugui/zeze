package Zeze.Raft;

import Zeze.Serialize.*;
import Zeze.Util.*;
import Zeze.*;
import java.io.*;

// 并发说明
// 如果你的数据可以用 ConcurrentDictionary 管理，并且只使用下面的方法：
// 1. value = GetOrAdd
// 2. lock (value) { read_write_value; }
// 3. Remove
// 没有单独的 TryAdd 操作。多个不同的项的访问是并发的。
// 此时可以使用下面的 ConcurrentMap。它使用 copy-on-write 的机制
// 实现并发的Raft要求的Snapshot。
// 【注意】GetOrAdd 引起的Add操作不会使用Raft日志同步状态，
// 每个Raft-Node都使用一致的GetOrAdd得到相同的记录。
//
// 【注意】GetOrAdd 和 lock(value) 之间存在时间窗口，使得可能拿到被删除的项。
// 这种情况一般都是有问题的，此时需要自己在 value 里面设置标志并检查。
// 伪码如下：
// void SomeRemove()
// {
//      var value = map.GetOrAdd(key);
//      lock (value)
//      {
//          if (checkAndNeedRemove())
//          {
//              value.State = Removed; // last State
//              map.Remove(key); // real remove。safe。
//          }
//      }
// }
// 
// void SomeProcess()
// {
//      while (true)
//      {
//          var value = map.GetOrAdd(key);
//          lock (value)
//          {
//              if (value.State == Removed)
//                  continue; // GetOrAdd again
//              normal_process;
//              return; // end of process
//          }
//      }
// }

public interface Copyable<T> extends Serializable {
	public T Copy();
}