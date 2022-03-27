using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using RocksDbSharp;

namespace Zeze.Util
{
    public class AsyncRocksDb
    {
        public RocksDb RocksDb { get; }

        public static AsyncExecutor Executor { get; }
        public static int MaxPoolSize { get; set; }

        static AsyncRocksDb()
        {
            MaxPoolSize = 100;
            Executor = new AsyncExecutor(() => MaxPoolSize);
        }

        public AsyncRocksDb(RocksDb db)
        {
            RocksDb = db;
        }

        public static async Task<AsyncRocksDb> OpenAsync(OptionsHandle options, string path)
        {
            var source = new TaskCompletionSource<bool>(TaskCreationOptions.RunContinuationsAsynchronously);
            RocksDb db = null;
            Executor.Execute(source, () => db = RocksDb.Open(options, path));
            await source.Task;
            return new AsyncRocksDb(db);
        }

        public static async Task<AsyncRocksDb> Open(DbOptions options, string path, ColumnFamilies columnFamilies)
        {
            var source = new TaskCompletionSource<bool>(TaskCreationOptions.RunContinuationsAsynchronously);
            RocksDb db = null;
            Executor.Execute(source, () => db = RocksDb.Open(options, path, columnFamilies));
            await source.Task;
            return new AsyncRocksDb(db);
        }

        public static async Task<IEnumerable<string>> ListColumnFamilies(DbOptions options, string name)
        {
            var source = new TaskCompletionSource<bool>(TaskCreationOptions.RunContinuationsAsynchronously);
            IEnumerable<string> value = null;
            Executor.Execute(source, () => value = RocksDb.ListColumnFamilies(options, name));
            await source.Task;
            return value;
        }

        public async Task<ColumnFamilyHandle> CreateColumnFamily(ColumnFamilyOptions cfOptions, string name)
        {
            var source = new TaskCompletionSource<bool>(TaskCreationOptions.RunContinuationsAsynchronously);
            ColumnFamilyHandle value = null;
            Executor.Execute(source, () => value = RocksDb.CreateColumnFamily(cfOptions, name));
            await source.Task;
            return value;
        }

        public async Task<string> GetAsync(string key, ColumnFamilyHandle cf = null, ReadOptions readOptions = null, Encoding encoding = null)
        {
            var source = new TaskCompletionSource<bool>(TaskCreationOptions.RunContinuationsAsynchronously);
            string value = null;
            Executor.Execute(source, () => value = RocksDb.Get(key, cf, readOptions, encoding));
            await source.Task;
            return value;
        }

        public async Task<byte[]> GetAsync(byte[] key, ColumnFamilyHandle cf = null, ReadOptions readOptions = null)
        {
            var source = new TaskCompletionSource<bool>(TaskCreationOptions.RunContinuationsAsynchronously);
            byte[] value = null;
            Executor.Execute(source, () => value = RocksDb.Get(key, cf, readOptions));
            await source.Task;
            return value;
        }

        public async Task<byte[]> GetAsync(byte[] key, long keyLength, ColumnFamilyHandle cf = null, ReadOptions readOptions = null)
        {
            var source = new TaskCompletionSource<bool>(TaskCreationOptions.RunContinuationsAsynchronously);
            byte[] value = null;
            Executor.Execute(source, () => value = RocksDb.Get(key, keyLength, cf, readOptions));
            await source.Task;
            return value;
        }

        public async Task<long> GetAsync(byte[] key, byte[] buffer, long offset, long length, ColumnFamilyHandle cf = null, ReadOptions readOptions = null)
        {
            var source = new TaskCompletionSource<bool>(TaskCreationOptions.RunContinuationsAsynchronously);
            long value = 0;
            Executor.Execute(source, () => value = RocksDb.Get(key, buffer, offset, length, cf, readOptions));
            await source.Task;
            return value;
        }

        public async Task<long> GetAsync(byte[] key, long keyLength, byte[] buffer, long offset, long length, ColumnFamilyHandle cf = null, ReadOptions readOptions = null)
        {
            var source = new TaskCompletionSource<bool>(TaskCreationOptions.RunContinuationsAsynchronously);
            long value = 0;
            Executor.Execute(source, () => value = RocksDb.Get(key, keyLength, buffer, offset, length, cf, readOptions));
            await source.Task;
            return value;
        }

        public async Task<byte[]> GetAsync(byte[] key, int offset, int length, ColumnFamilyHandle cf = null, ReadOptions readOptions = null)
        {
            if (offset == 0)
                return await GetAsync(key, length, cf, readOptions);

            Array.Copy(key, offset, key, 0, length);
            return await GetAsync(key, length, cf, readOptions);
        }

        public async Task RemoveAsync(string key, ColumnFamilyHandle cf = null, WriteOptions writeOptions = null)
        {
            var source = new TaskCompletionSource<bool>(TaskCreationOptions.RunContinuationsAsynchronously);
            Executor.Execute(source, () => RocksDb.Remove(key, cf, writeOptions));
            await source.Task;
        }

        public async Task RemoveAsync(byte[] key, ColumnFamilyHandle cf = null, WriteOptions writeOptions = null)
        {
            var source = new TaskCompletionSource<bool>(TaskCreationOptions.RunContinuationsAsynchronously);
            Executor.Execute(source, () => RocksDb.Remove(key, cf, writeOptions));
            await source.Task;
        }

        public async Task RemoveAsync(byte[] key, long keyLength, ColumnFamilyHandle cf = null, WriteOptions writeOptions = null)
        {
            var source = new TaskCompletionSource<bool>(TaskCreationOptions.RunContinuationsAsynchronously);
            Executor.Execute(source, () => RocksDb.Remove(key, keyLength, cf, writeOptions));
            await source.Task;
        }

        public async Task PutAsync(string key, string value, ColumnFamilyHandle cf = null, WriteOptions writeOptions = null, Encoding encoding = null)
        {
            var source = new TaskCompletionSource<bool>(TaskCreationOptions.RunContinuationsAsynchronously);
            Executor.Execute(source, () => RocksDb.Put(key, value, cf, writeOptions, encoding));
            await source.Task;
        }

        public async Task PutAsync(byte[] key, byte[] value, ColumnFamilyHandle cf = null, WriteOptions writeOptions = null)
        {
            var source = new TaskCompletionSource<bool>(TaskCreationOptions.RunContinuationsAsynchronously);
            Executor.Execute(source, () => RocksDb.Put(key, value, cf, writeOptions));
            await source.Task;
        }

        public async Task PutAsync(byte[] key, long keyLength, byte[] value, long valueLength, ColumnFamilyHandle cf = null, WriteOptions writeOptions = null)
        {
            var source = new TaskCompletionSource<bool>(TaskCreationOptions.RunContinuationsAsynchronously);
            Executor.Execute(source, () => RocksDb.Put(key, keyLength, value, valueLength, cf, writeOptions));
            await source.Task;
        }

        public async Task WriteAsync(WriteBatch writeBatch, WriteOptions writeOptions = null)
        {
            var source = new TaskCompletionSource<bool>(TaskCreationOptions.RunContinuationsAsynchronously);
            Executor.Execute(source, () => RocksDb.Write(writeBatch, writeOptions));
            await source.Task;
        }
    }
}
