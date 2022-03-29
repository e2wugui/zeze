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

        public AsyncExecutor AsyncExecutor { get; }

        public AsyncRocksDb(RocksDb db, AsyncExecutor executor)
        {
            RocksDb = db;
            AsyncExecutor = executor;
        }

        public static async Task<AsyncRocksDb> OpenAsync(OptionsHandle options, string path, AsyncExecutor executor)
        {
            RocksDb db = null;
            await executor.RunAsync(() => db = RocksDb.Open(options, path));
            return new AsyncRocksDb(db, executor);
        }

        public static async Task<AsyncRocksDb> OpenAsync(DbOptions options, string path, ColumnFamilies columnFamilies, AsyncExecutor executor)
        {
            RocksDb db = null;
            await executor.RunAsync(() => db = RocksDb.Open(options, path, columnFamilies));
            return new AsyncRocksDb(db, executor);
        }

        public static async Task<IEnumerable<string>> ListColumnFamilies(DbOptions options, string name, AsyncExecutor executor)
        {
            IEnumerable<string> value = null;
            await executor.RunAsync(() => value = RocksDb.ListColumnFamilies(options, name));
            return value;
        }

        public async Task<ColumnFamilyHandle> CreateColumnFamily(ColumnFamilyOptions cfOptions, string name)
        {
            ColumnFamilyHandle value = null;
            await AsyncExecutor.RunAsync(() => value = RocksDb.CreateColumnFamily(cfOptions, name));
            return value;
        }

        public async Task<string> GetAsync(string key, ColumnFamilyHandle cf = null, ReadOptions readOptions = null, Encoding encoding = null)
        {
            string value = null;
            await AsyncExecutor.RunAsync(() => value = RocksDb.Get(key, cf, readOptions, encoding));
            return value;
        }

        public async Task<byte[]> GetAsync(byte[] key, ColumnFamilyHandle cf = null, ReadOptions readOptions = null)
        {
            byte[] value = null;
            await AsyncExecutor.RunAsync(() => value = RocksDb.Get(key, cf, readOptions));
            return value;
        }

        public async Task<byte[]> GetAsync(byte[] key, long keyLength, ColumnFamilyHandle cf = null, ReadOptions readOptions = null)
        {
            byte[] value = null;
            await AsyncExecutor.RunAsync(() => value = RocksDb.Get(key, keyLength, cf, readOptions));
            return value;
        }

        public async Task<long> GetAsync(byte[] key, byte[] buffer, long offset, long length, ColumnFamilyHandle cf = null, ReadOptions readOptions = null)
        {
            long value = 0;
            await AsyncExecutor.RunAsync(() => value = RocksDb.Get(key, buffer, offset, length, cf, readOptions));
            return value;
        }

        public async Task<long> GetAsync(byte[] key, long keyLength, byte[] buffer, long offset, long length, ColumnFamilyHandle cf = null, ReadOptions readOptions = null)
        {
            long value = 0;
            await AsyncExecutor.RunAsync(() => value = RocksDb.Get(key, keyLength, buffer, offset, length, cf, readOptions));
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
            await AsyncExecutor.RunAsync(() => RocksDb.Remove(key, cf, writeOptions));
        }

        public async Task RemoveAsync(byte[] key, ColumnFamilyHandle cf = null, WriteOptions writeOptions = null)
        {
            await AsyncExecutor.RunAsync(() => RocksDb.Remove(key, cf, writeOptions));
        }

        public async Task RemoveAsync(byte[] key, long keyLength, ColumnFamilyHandle cf = null, WriteOptions writeOptions = null)
        {
            await AsyncExecutor.RunAsync(() => RocksDb.Remove(key, keyLength, cf, writeOptions));
        }

        public async Task PutAsync(string key, string value, ColumnFamilyHandle cf = null, WriteOptions writeOptions = null, Encoding encoding = null)
        {
            await AsyncExecutor.RunAsync(() => RocksDb.Put(key, value, cf, writeOptions, encoding));
        }

        public async Task PutAsync(byte[] key, byte[] value, ColumnFamilyHandle cf = null, WriteOptions writeOptions = null)
        {
            await AsyncExecutor.RunAsync(() => RocksDb.Put(key, value, cf, writeOptions));
        }

        public async Task PutAsync(byte[] key, long keyLength, byte[] value, long valueLength, ColumnFamilyHandle cf = null, WriteOptions writeOptions = null)
        {
            await AsyncExecutor.RunAsync(() => RocksDb.Put(key, keyLength, value, valueLength, cf, writeOptions));
        }

        public async Task WriteAsync(WriteBatch writeBatch, WriteOptions writeOptions = null)
        {
            var source = new TaskCompletionSource<object>(TaskCreationOptions.RunContinuationsAsynchronously);
            await AsyncExecutor.RunAsync(() => RocksDb.Write(writeBatch, writeOptions));
        }
    }
}
