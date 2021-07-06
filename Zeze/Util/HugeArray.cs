using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace Zeze.Util
{
    public class HugeArray<T> where T : class
    {
        private List<List<T>> GetArrays(ref long index)
        {
            if (index >= 0)
                return Arrays;
            index = -index - 1;
            return ArraysNegative;
        }

        public T this[long index]
        {
            get
            {
                var arrays = GetArrays(ref index);
                var (block, offset) = ToBlock(index);
                if (block >= arrays.Count)
                    return default(T);

                var array = arrays[block];
                if (null == array || offset >= array.Count)
                    return default(T);

                return array[offset];
            }
            set
            {
                var arrays = GetArrays(ref index);
                var (block, offset) = ToBlock(index);
                EnsureBlock(arrays, block, offset)[offset] = value;
            }
        }

        private T NewAndSet(List<List<T>> arrays, int block, int offset, Func<T> factory)
        {
            T n = factory();
            EnsureBlock(arrays, block, offset)[offset] = n;
            return n;
        }

        public T GetOrAdd(long index, Func<T> factory)
        {
            var arrays = GetArrays(ref index);
            var (block, offset) = ToBlock(index);
            if (block >= arrays.Count)
                return NewAndSet(arrays, block, offset, factory);

            var array = arrays[block];
            if (null == array || offset >= array.Count)
                return NewAndSet(arrays, block, offset, factory);

            T e = array[offset];
            if (null == e)
            {
                e = factory();
                array[offset] = e;
            }
            return e;
        }

        private long GetArraysElementsCount(List<List<T>> arrays)
        {
            if (arrays.Count == 0)
                return 0;
            long count = arrays.Count - 1;
            count *= BlockSize;
            count += arrays[^1].Count;
            return count;
        }

        public long Count => GetArraysElementsCount(Arrays) + GetArraysElementsCount(ArraysNegative);

        public long BlockCount => Arrays.Count + ArraysNegative.Count;

        public int BlockSize { get; }
        public int BlockBits { get; }
        public int BlockMask { get; }

        public HugeArray(int blockSize = 1024 * 1024)
        {
            var (size, bits) = ToPower2(blockSize);
            BlockSize = size;
            BlockBits = bits;
            BlockMask = size - 1;
        }

        private List<List<T>> Arrays { get; } = new List<List<T>>();
        private List<List<T>> ArraysNegative { get; } = new List<List<T>>();

        private (int, int) ToPower2(int needSize)
        {
            int bits = 8; // min
            int size = 1 << bits;
            while (size < needSize)
            {
                size <<= 1;
                bits += 1;
            }
            return (size, bits);
        }

        private List<T> EnsureBlock(List<List<T>> arrays, int blockIndex, int blockOffset)
        {
            int asize = blockIndex + 1;
            for (int i = arrays.Count; i < asize; ++i)
                arrays.Add(null);

            var array = arrays[blockIndex];
            if (null == array)
            {
                array = new List<T>();
                arrays[blockIndex] = array;
            }

            int size = blockOffset + 1;
            for (int i = array.Count; i < size; ++i)
            {
                array.Add(default(T));
            }
            return array;
        }

        private (int, int) ToBlock(long index)
        {
            // safe enough
            // if (index < 0)
            //    throw new ArgumentException();

            long blockIndex = index >> BlockBits;
            int blockOffset = (int)(index & BlockMask);

            if (blockIndex + 1 > int.MaxValue)
                throw new Exception("Index Too Big " + index);

            // Ensure Last Block
            return ((int)blockIndex, blockOffset);
        }
    }
}
