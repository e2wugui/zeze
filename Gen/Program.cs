using System;

namespace Gen
{
    public interface BeanReadOnly
    {
    }

    public class Bean : BeanReadOnly
    {
    }

    public interface PList1ReadOnly<out E>
    {
        public E this[int index] { get; }
    }

    public class PList1<E> : PList1ReadOnly<E> where E : Bean
    {
        public E this[int index] { get => null; set { } }
    }

    public class Program
    {
        public static void Main(string[] args)
        {
            var real = new PList1<Bean>();
            PList1ReadOnly<BeanReadOnly> r = real;

            Zeze.Gen.Program.Main(args);
        }
    }
}
