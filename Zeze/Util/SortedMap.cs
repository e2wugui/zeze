
using System;
using System.Collections.Generic;

namespace Zeze.Util;

/**
 * 这根本不是Map。
 *
 * @param <K>
 * @param <V>
 */
public class SortedMap<K, V>
	where K : IComparable<K>
{
	public class Entry<K2, V2> : IComparable<Entry<K2, V2>>
		where K2 : IComparable<K2>
    {
        public K2 Key { get; }
        public V2 Value { get; }

		public Entry(K2 key, V2 value) {
			Key = key;
			Value = value;
		}

		public int CompareTo(Entry<K2, V2> o)
		{
			return Key.CompareTo(o.Key);
		}

		public override string ToString()
		{
			return $"({Key},{Value})";
		}
	}

	private List<Entry<K, V>> elements = new();

	public int Count => elements.Count;

	public Entry<K, V> First => elements.Count == 0 ? null : elements[0];

	public Entry<K, V> LowerBound(K key)
	{
		var index = LowerBoundIndex(key);
		if (index < elements.Count)
			return elements[index];
		return null;
	}

	public Entry<K, V> UpperBound(K key)
	{
		var index = UpperBoundIndex(key);
		if (index < elements.Count)
			return elements[index];
		return null;
	}

    /// <summary>
    /// std::lower_bound 定义。返回指定key为上限的索引。即在>=key范围内找最小的key的索引
    /// </summary>
    /// <param name="key"></param>
    /// <returns>index locate，不存在时返回lastIndex+1。</returns>
    public int LowerBoundIndex(K key)
	{
		var first = 0;
		var count = elements.Count;
		while (count > 0)
		{
			var it = first;
			var step = count >> 1;
			it += step;
			if (elements[it].Key.CompareTo(key) < 0)
			{
				first = it + 1;
				count -= step + 1;
			}
			else
				count = step;
		}
		return first;
	}

    /// <summary>
    /// std::upper_bound 定义。返回指定key为下限的索引。即在>key范围内找最小的key的索引 
    /// </summary>
    /// <param name="key"></param>
    /// <returns>index locate，不存在时返回lastIndex+1</returns>
    public int UpperBoundIndex(K key)
	{
		var first = 0;
		var count = elements.Count;
		while (count > 0)
		{
			var it = first;
			var step = count >> 1;
			it += step;
			if (elements[it].Key.CompareTo(key) <= 0)
			{
				first = it + 1;
				count -= step + 1;
			}
			else
				count = step;
		}
		return first;
	}

    /// <summary>
	/// 二分法查找key
    /// </summary>
    /// <param name="key"></param>
    /// <returns>index found. -1 means not found.</returns>
    public int FindIndex(K key)
	{
		int low = 0;
		int high = elements.Count - 1;
		while (low <= high)
		{
			int middle = (low + high) / 2;
			int c = key.CompareTo(elements[middle].Key);
			if (c == 0)
				return middle;
			if (c > 0)
				low = middle + 1;
			else
				high = middle - 1;
		}
		return -1;
	}

	public Entry<K, V> Get(K key)
	{
		var index = FindIndex(key);
		if (index >= 0)
			return elements[index];
		return null;
	}

	public int Add(K key, V value)
	{
		var index = LowerBoundIndex(key);
		if (index >= 0) {
			if (index < elements.Count&& elements[index].Key.CompareTo(key) == 0)
				return -1; // duplicate.
			elements.Insert(index, new Entry<K, V>(key, value));
			return index;
		}
		throw new Exception("internal error");
	}

	// 返回冲突entry(原elements中的,未被覆盖)列表
	public List<Entry<K, V>> AddAll(K[] keys, V value)
	{
		Array.Sort(keys);
		return AddSortedAll(keys, value);
	}

	// 返回冲突entry(原elements中的,未被覆盖)列表
	public List<Entry<K, V>> AddSortedAll(K[] keys, V value)
	{
		List<Entry<K, V>> r = null;
		int input = elements.Count, jn = keys.Length;
		var newElements = new List<Entry<K, V>>(input + jn);
		int i = 0, j = 0;
		if (i < input && j < jn)
		{
			var ie = elements[i];
			for (K ik = ie.Key, jk = keys[j]; ; )
			{
				int c = ik.CompareTo(jk);
				if (c < 0)
				{
					newElements.Add(ie);
					if (++i >= input)
						break;
					ie = elements[i];
					ik = ie.Key;
				}
				else if (c > 0)
				{
					newElements.Add(new Entry<K, V>(jk, value));
					if (++j >= jn)
						break;
					jk = keys[j];
				} else {
					if (r == null)
						r = new List<Entry<K, V>>();
					r.Add(ie);
					newElements.Add(ie);
					++j;
					if (++i >= input || j >= jn)
						break;
					ie = elements[i];
					ik = ie.Key;
					jk = keys[j];
				}
			}
		}
		if (i < input)
		{
            //newElements.addAll(elements.subList(i, input));
			for (var ii = i; ii < input; ++ ii)
				newElements.Add(elements[ii]);
        }
        else
		{
			while (j < jn)
				newElements.Add(new Entry<K, V>(keys[j++], value));
		}
		elements = newElements;
		return r != null ? r : new List<Entry<K, V>>();
	}

	public Entry<K, V> Remove(K key)
	{
		var index = FindIndex(key);
		if (index >= 0)
			return RemoveAt(index);
		return null;
	}

	public Entry<K, V> Remove(K key, V value)
	{
		var index = FindIndex(key);
		if (index >= 0 && object.ReferenceEquals(elements[index].Value, value))
			return RemoveAt(index);
		return null;
	}

    public Entry<K, V> this[int index]
	{
		get { return elements[index]; }	
		set { elements[index] = value; }
	}

    public Entry<K, V> RemoveAt(int index) {
		var e = elements[index];
		elements.RemoveAt(index);
		return e;
	}

	public override string ToString() {
		return elements.ToString();
	}
}
