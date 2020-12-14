using System;
using System.Collections.Generic;
using System.Diagnostics.CodeAnalysis;
using System.Text;

namespace Zeze.Util
{
    /// <summary>
    /// 把三维空间划分成一个个相邻的Cube。地图中的玩家或者物品Id记录在所在的Cube中。
    /// 用来快速找到某个坐标周围的玩家或物体。
    /// </summary>
    public class CubeIndexMap
    {
        public class ObjectId : Comparer<ObjectId>
        {
            public static readonly int TypeRole = 0;

            public int Type { get; set; }
            public int ConfigId { get; set; }
            public long InstanceId { get; set; }

            public override int Compare(ObjectId x, ObjectId y)
            {
                int c = Type.CompareTo(x.Type);
                if (c != 0)
                    return c;
                c = ConfigId.CompareTo(x.ConfigId);
                if (c != 0)
                    return c;
                return InstanceId.CompareTo(x.InstanceId);
            }

            public override int GetHashCode()
            {
                return Type + ConfigId + InstanceId.GetHashCode();
            }

            public override bool Equals(object obj)
            {
                if (obj == null)
                    return false;

                if (obj == this)
                    return true;

                if (obj is ObjectId o)
                {
                    return Type == o.Type && ConfigId == o.ConfigId && InstanceId == o.InstanceId;
                }
                return false;
            }
        }

        public class Cube
        {
            public HashSet<ObjectId> ObjectIds { get; } = new HashSet<ObjectId>();
        }
        class Index<T>
        {
            public int Start { get; set; }
            public int Count { get; private set; }
            public T[] Elements { get; private set; } = Array.Empty<T>();

            private static int ToPower2(int needSize)
            {
                int size = 128;
                while (size < needSize)
                    size <<= 1;
                return size;
            }

            private bool EnsureSize(int size, int offset)
            {
                if (size > Elements.Length)
                {
                    int newsize = ToPower2(size);
                    T[] newArray = new T[newsize];
                    Array.Copy(Elements, 0, newArray, offset, Count);
                    Elements = newArray;
                    Count = size;
                    return true;
                }
                return false;
            }

            public T GetOrAdd(int index, Func<T> factory)
            {
                if (index < Start)
                {
                    int offset = Start - index;
                    if (false == EnsureSize(Count + offset, offset))
                    {
                        Buffer.BlockCopy(Elements, 0, Elements, offset, Count);
                        Count += offset;
                    }
                    Start = index;
                }
                else
                {
                    EnsureSize(index - Start + 1, 0);
                }
                int realIndex = index - Start;
                T e = Elements[realIndex];
                if (null != e)
                    return e;
                e = factory();
                Elements[realIndex] = e;
                return e;
            }

            public T Get(int index)
            {
                if (index < Start)
                    return default(T);
                if (index - Start + 1 > Elements.Length)
                    return default(T);
                return Elements[index - Start];
            }
        }
        Index<Index<Index<Cube>>> IndexX;

        private int CubeSizeX;
        private int CubeSizeY;
        private int CubeSizeZ;

        public CubeIndexMap(int cubeSizeX = 256, int cubeSizeY = 256, int cubeSizeZ = 256)
        {
            if (cubeSizeX <= 0)
                throw new ArgumentException();
            if (cubeSizeY <= 0)
                throw new ArgumentException();
            if (cubeSizeZ <= 0)
                throw new ArgumentException();

            this.CubeSizeX = cubeSizeX;
            this.CubeSizeY = cubeSizeY;
            this.CubeSizeZ = cubeSizeZ;
        }

        /// <summary>
        /// 角色进入地图时
        /// </summary>
        /// <param name="objId"></param>
        /// <param name="x"></param>
        /// <param name="y"></param>
        /// <param name="z"></param>
        public void OnEnter(ObjectId objId, float x, float y, float z)
        {
            int ix = (int)x / CubeSizeX;
            int iy = (int)y / CubeSizeY;
            int iz = (int)z / CubeSizeZ;

            GetOrAdd(ix, iy, iz).ObjectIds.Add(objId);
        }

        /// <summary>
        /// 角色位置变化时
        /// </summary>
        /// <param name="objId"></param>
        /// <param name="oldx"></param>
        /// <param name="oldy"></param>
        /// <param name="oldz"></param>
        /// <param name="newx"></param>
        /// <param name="newy"></param>
        /// <param name="newz"></param>
        public void OnMove(ObjectId objId, float oldx, float oldy, float oldz, float newx, float newy, float newz)
        {
            int iox = (int)oldx / CubeSizeX;
            int ioy = (int)oldy / CubeSizeY;
            int ioz = (int)oldz / CubeSizeZ;

            int inx = (int)newx / CubeSizeX;
            int iny = (int)newy / CubeSizeY;
            int inz = (int)newz / CubeSizeZ;

            if (iox == inx && ioy == iny && ioz == inz)
                return;

            Get(iox, ioy, ioz)?.ObjectIds.Remove(objId);
            GetOrAdd(inx, iny, inz).ObjectIds.Add(objId);
        }

        /// <summary>
        /// 角色离开地图时
        /// </summary>
        /// <param name="objId"></param>
        /// <param name="x"></param>
        /// <param name="y"></param>
        /// <param name="z"></param>
        public void OnLeave(ObjectId objId, float x, float y, float z)
        {
            int ix = (int)x / CubeSizeX;
            int iy = (int)y / CubeSizeY;
            int iz = (int)z / CubeSizeZ;

            Get(ix, iy, iz)?.ObjectIds.Remove(objId);
        }

        /// <summary>
        /// 返回 center 坐标所在的 cube 周围的所有 cube。
        /// 可以遍历返回的Cube的所有角色，进一步进行精确的距离判断。
        /// </summary>
        /// <param name="centerX"></param>
        /// <param name="centerY"></param>
        /// <param name="centerZ"></param>
        /// <param name="range">周围cube数</param>
        /// <returns></returns>
        public List<Cube> GetCubes(float centerX, float centerY, float centerZ, int range = 2)
        {
            int ix = (int)centerX / CubeSizeX;
            int iy = (int)centerY / CubeSizeY;
            int iz = (int)centerZ / CubeSizeZ;

            List<Cube> result = new List<Cube>();
            for (int i = ix - range; i <= ix + range; ++i)
            {
                for (int j = iy - range; j <= iy + range; ++j)
                {
                    for (int k = iz - range; k <= iz + range; ++k)
                    {
                        Cube cube = Get(i, j, k);
                        if (cube != null)
                            result.Add(cube);
                    }
                }
            }
            return result;
        }

        private Cube GetOrAdd(int ix, int iy, int iz)
        {
            if (IndexX == null)
                IndexX = new Index<Index<Index<Cube>>>() { Start = ix };
            Index<Index<Cube>> IndexY = IndexX.GetOrAdd(ix, () => new Index<Index<Cube>>() { Start = iy });
            Index<Cube> IndexZ = IndexY.GetOrAdd(iy, () => new Index<Cube>() { Start = iz });
            Cube cube = IndexZ.GetOrAdd(iz, () => new Cube());
            return cube;
        }


        private Cube Get(int ix, int iy, int iz)
        {
            if (IndexX == null)
                return null;
            Index<Index<Cube>> IndexY = IndexX.Get(ix);
            if (IndexY == null)
                return null;
            Index<Cube> IndexZ = IndexY.Get(iy);
            if (IndexZ == null)
                return null;
            return IndexZ.Get(iz);
        }
    }
}
