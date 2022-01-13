
using System.Collections.Concurrent;
using System;
using System.IO;

namespace Zeze.Util
{
    public class PersistentAtomicLong
    {
        private readonly AtomicLong CurrentId = new AtomicLong();
        private AtomicLong allocated = new AtomicLong();

        private readonly string FileName;
        private readonly int AllocateSize;

        private static ConcurrentDictionary<string, PersistentAtomicLong> pals = new ConcurrentDictionary<string, PersistentAtomicLong>();

        /**
         * 【小优化】请保存返回值，重复使用。
         *
         * @param ProgramInstanceName
         * 程序实例名字。
         * 当进程只有一份实例时，可以直接使用程序名。
         * 有多个实例时，需要另一个Id区分，这里不能使用进程id（pid），需要稳定的。
         * 对于网络程序，可以使用"进程名+Main.Acceptor.Name"
         */
        public static PersistentAtomicLong GetOrAdd(string ProgramInstanceName, int allocateSize)
        {
            var name = ProgramInstanceName.Replace(':', '.');
            // 这样写，不小心重名也能工作。
            return pals.GetOrAdd(name, (k) => new PersistentAtomicLong(k, allocateSize));
        }

        public static PersistentAtomicLong GetOrAdd(string ProgramInstanceName)
        {
            return GetOrAdd(ProgramInstanceName, 5000);
        }

        private PersistentAtomicLong(string ProgramInstanceName, int allocateSize)
        {
            if (allocateSize <= 0)
                throw new ArgumentException();

            FileName = ProgramInstanceName + ".PersistentAtomicLong.txt";
            AllocateSize = allocateSize;

            if (File.Exists(FileName))
            {
                var lines = File.ReadAllLines(FileName);
                if (lines.Length > 0)
                {
                    var alloc = long.Parse(lines[0]);
                    allocated.GetAndSet(alloc);
                    CurrentId.GetAndSet(alloc);
                }
            }
            // 初始化的时候不allocate，如果程序启动，没有分配就退出，保持原来的值。
        }

        public long Next()
        {
            for (; ; )
            {
                long current = CurrentId.Get();
                if (current >= allocated.Get())
                {
                    Allocate();
                    continue;
                }
                if (CurrentId.CompareAndSet(current, current + 1))
                    return current + 1;
            }
        }

        private void Allocate()
        {
            lock (this) {
                if (CurrentId.Get() < allocated.Get())
                    return; // has allocated. concurrent.

                // 应该尽量减少allocate的次数，所以这里文件就不保持打开了。
                File.WriteAllText(FileName, (allocated.Get() + AllocateSize).ToString());
                allocated.AddAndGet(AllocateSize);
            }
        }
    }
}
