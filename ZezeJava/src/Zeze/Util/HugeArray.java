package Zeze.Util;

import Zeze.*;
import java.util.*;

/** 
 这个类没啥用。先留着吧。
 
 <typeparam name="T"></typeparam>
*/
public class HugeArray<T> {
	private ArrayList<ArrayList<T>> GetArrays(tangible.RefObject<Long> index, boolean forSet) {
		if (forSet) {
			if (index.refArgValue > getMaxIndex()) {
				setMaxIndex(index.refArgValue);
			}
			else if (index.refArgValue < getMinIndex()) {
				setMinIndex(index.refArgValue);
			}
		}

		if (index.refArgValue >= 0) {
			return getArrays();
		}
		index.refArgValue = -index.refArgValue - 1;
		return getArraysNegative();
	}

	public final T get(long index) {
		tangible.RefObject<Long> tempRef_index = new tangible.RefObject<Long>(index);
		var arrays = GetArrays(tempRef_index, false);
	index = tempRef_index.refArgValue;
//C# TO JAVA CONVERTER TODO TASK: Java has no equivalent to C# deconstruction declarations:
		var(block, offset) = ToBlock(index);
		if (block >= arrays.size()) {
			return null;
		}

		var array = arrays.get(block);
		if (null == array || offset >= array.size()) {
			return null;
		}

		return array[offset];
	}
	public final void set(long index, T value) {
		tangible.RefObject<Long> tempRef_index = new tangible.RefObject<Long>(index);
		var arrays = GetArrays(tempRef_index, true);
	index = tempRef_index.refArgValue;
//C# TO JAVA CONVERTER TODO TASK: Java has no equivalent to C# deconstruction declarations:
		var(block, offset) = ToBlock(index);
		EnsureBlock(arrays, block, offset).set(offset, value);
	}

	private T NewAndSet(ArrayList<ArrayList<T>> arrays, int block, int offset, tangible.Func0Param<T> factory) {
		T n = factory.invoke();
		EnsureBlock(arrays, block, offset).set(offset, n);
		return n;
	}

	public final T GetOrAdd(long index, tangible.Func0Param<T> factory) {
		tangible.RefObject<Long> tempRef_index = new tangible.RefObject<Long>(index);
		var arrays = GetArrays(tempRef_index, true);
	index = tempRef_index.refArgValue;
//C# TO JAVA CONVERTER TODO TASK: Java has no equivalent to C# deconstruction declarations:
		var(block, offset) = ToBlock(index);
		if (block >= arrays.size()) {
			return NewAndSet(arrays, block, offset, factory);
		}

		var array = arrays.get(block);
		if (null == array || offset >= array.size()) {
			return NewAndSet(arrays, block, offset, factory);
		}

		T e = array[offset];
		if (null == e) {
			e = factory.invoke();
			array[offset] = e;
		}
		return e;
	}

	/*
	private long GetArraysElementsCount(List<List<T>> arrays)
	{
	    if (arrays.Count == 0)
	        return 0;
	    long count = arrays.Count - 1;
	    count *= BlockSize;
	    count += arrays[^1].Count;
	    return count;
	}
	*/

	private long MinIndex = 0;
	public final long getMinIndex() {
		return MinIndex;
	}
	private void setMinIndex(long value) {
		MinIndex = value;
	}
	private long MaxIndex = -1;
	public final long getMaxIndex() {
		return MaxIndex;
	}
	private void setMaxIndex(long value) {
		MaxIndex = value;
	}

	public final long getCount() {
		return getMaxIndex() + 1 - getMinIndex();
	}
	// => GetArraysElementsCount(Arrays) + GetArraysElementsCount(ArraysNegative);

	public final long getBlockCount() {
		return getArrays().size() + getArraysNegative().size();
	}

	private int BlockSize;
	public final int getBlockSize() {
		return BlockSize;
	}
	private int BlockBits;
	public final int getBlockBits() {
		return BlockBits;
	}
	private int BlockMask;
	public final int getBlockMask() {
		return BlockMask;
	}


	public HugeArray() {
		this(1024 * 1024);
	}

//C# TO JAVA CONVERTER NOTE: Java does not support optional parameters. Overloaded method(s) are created above:
//ORIGINAL LINE: public HugeArray(int blockSize = 1024 * 1024)
	public HugeArray(int blockSize) {
//C# TO JAVA CONVERTER TODO TASK: Java has no equivalent to C# deconstruction declarations:
		var(size, bits) = ToPower2(blockSize);
		BlockSize = size;
		BlockBits = bits;
		BlockMask = size - 1;
	}

	private ArrayList<ArrayList<T>> Arrays = new ArrayList<ArrayList<T>> ();
	private ArrayList<ArrayList<T>> getArrays() {
		return Arrays;
	}
	private ArrayList<ArrayList<T>> ArraysNegative = new ArrayList<ArrayList<T>> ();
	private ArrayList<ArrayList<T>> getArraysNegative() {
		return ArraysNegative;
	}

//C# TO JAVA CONVERTER TODO TASK: Methods returning tuples are not converted by C# to Java Converter:
//	private (int, int) ToPower2(int needSize)
//		{
//			int bits = 8; // min
//			int size = 1 << bits;
//			while (size < needSize)
//			{
//				size <<= 1;
//				bits += 1;
//			}
//			return (size, bits);
//		}

	private ArrayList<T> EnsureBlock(ArrayList<ArrayList<T>> arrays, int blockIndex, int blockOffset) {
		int asize = blockIndex + 1;
		for (int i = arrays.size(); i < asize; ++i) {
			arrays.add(null);
		}

		var array = arrays.get(blockIndex);
		if (null == array) {
			array = new ArrayList<T>();
			arrays.set(blockIndex, array);
		}

		int size = blockOffset + 1;
		for (int i = array.size(); i < size; ++i) {
			array.add(null);
		}
		return array;
	}

//C# TO JAVA CONVERTER TODO TASK: Methods returning tuples are not converted by C# to Java Converter:
//	private (int, int) ToBlock(long index)
//		{
//			// safe enough
//			// if (index < 0)
//			//    throw new ArgumentException();
//
//			long blockIndex = index >> BlockBits;
//			int blockOffset = (int)(index & BlockMask);
//
//			if (blockIndex + 1 > int.MaxValue)
//				throw new Exception("Index Too Big " + index);
//
//			// Ensure Last Block
//			return ((int)blockIndex, blockOffset);
//		}
}