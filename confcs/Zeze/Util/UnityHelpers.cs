using System;
using System.Collections.Concurrent;
using System.Collections.Generic;
using System.IO;

namespace Zeze.Util
{

    public static class PathHelper
    {
        public static String GetRelativePath(String fromPath, String toPath)
        {
            if (String.IsNullOrEmpty(fromPath)) throw new ArgumentNullException("fromPath");
            if (String.IsNullOrEmpty(toPath))   throw new ArgumentNullException("toPath");

            Uri fromUri = new Uri(fromPath);
            Uri toUri = new Uri(toPath);

            if (fromUri.Scheme != toUri.Scheme) { return toPath; } // path can't be made relative.

            Uri relativeUri = fromUri.MakeRelativeUri(toUri);
            String relativePath = Uri.UnescapeDataString(relativeUri.ToString());

            if (toUri.Scheme.Equals("file", StringComparison.InvariantCultureIgnoreCase))
            {
                relativePath = relativePath.Replace(Path.AltDirectorySeparatorChar, Path.DirectorySeparatorChar);
            }

            return relativePath;
        }
    }
    
    public static class ConcurrentDictionaryExtension
    {
        // unity 不支持这个接口， 先实现一个不安全接口，让他跑起来
        public static bool TryRemove<TKey, TValue>(this ConcurrentDictionary<TKey, TValue> concurrentDictionary,
            KeyValuePair<TKey, TValue> pair)
        {
            if (!concurrentDictionary.TryGetValue(pair.Key, out var value))
            {
                return false;
            }

            if (pair.Value.Equals(value))
            {
                return concurrentDictionary.TryRemove(pair.Key, out value);
            }
            else
            {
                return false;
            }
        }
    }
    
    public static class KeyValuePairHelper
    {
        public static KeyValuePair<TKey, TValue> Create<TKey, TValue>(TKey key, TValue value)
        {
            return new KeyValuePair<TKey, TValue>(key, value);
        }
    }

}