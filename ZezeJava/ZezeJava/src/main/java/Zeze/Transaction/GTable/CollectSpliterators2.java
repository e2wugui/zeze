package Zeze.Transaction.GTable;

import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.function.Function;
import javax.annotation.CheckForNull;
import static java.lang.Math.max;

public class CollectSpliterators2 {
	static <InElementT, OutElementT>
	Spliterator<OutElementT> map(
			Spliterator<InElementT> fromSpliterator,
			Function<? super InElementT, ? extends OutElementT> function) {
		Utils.checkNotNull(fromSpliterator);
		Utils.checkNotNull(function);
		return new Spliterator<>() {
			@Override
			public boolean tryAdvance(Consumer<? super OutElementT> action) {
				return fromSpliterator.tryAdvance(
						fromElement -> action.accept(function.apply(fromElement)));
			}

			@Override
			public void forEachRemaining(Consumer<? super OutElementT> action) {
				fromSpliterator.forEachRemaining(fromElement -> action.accept(function.apply(fromElement)));
			}

			@Override
			@CheckForNull
			public Spliterator<OutElementT> trySplit() {
				Spliterator<InElementT> fromSplit = fromSpliterator.trySplit();
				return (fromSplit != null) ? map(fromSplit, function) : null;
			}

			@Override
			public long estimateSize() {
				return fromSpliterator.estimateSize();
			}

			@Override
			public int characteristics() {
				return fromSpliterator.characteristics()
						& ~(Spliterator.DISTINCT | Spliterator.NONNULL | Spliterator.SORTED);
			}
		};
	}

	static <InElementT, OutElementT>
	Spliterator<OutElementT> flatMap(
			Spliterator<InElementT> fromSpliterator,
			Function<? super InElementT, Spliterator<OutElementT>> function,
			int topCharacteristics,
			long topSize) {
		Utils.checkArgument(
				(topCharacteristics & Spliterator.SUBSIZED) == 0,
				"flatMap does not support SUBSIZED characteristic");
		Utils.checkArgument(
				(topCharacteristics & Spliterator.SORTED) == 0,
				"flatMap does not support SORTED characteristic");
		Utils.checkNotNull(fromSpliterator);
		Utils.checkNotNull(function);
		return new FlatMapSpliteratorOfObject<>(
				null, fromSpliterator, function, topCharacteristics, topSize);
	}

	abstract static class FlatMapSpliterator<
			InElementT,
			OutElementT,
			OutSpliteratorT extends Spliterator<OutElementT>>
			implements Spliterator<OutElementT> {
		/** Factory for constructing {@link FlatMapSpliterator} instances. */
		@FunctionalInterface
		interface Factory<InElementT, OutSpliteratorT extends Spliterator<?>> {
			OutSpliteratorT newFlatMapSpliterator(
					@CheckForNull OutSpliteratorT prefix,
					Spliterator<InElementT> fromSplit,
					Function<? super InElementT, OutSpliteratorT> function,
					int splitCharacteristics,
					long estSplitSize);
		}

		@CheckForNull OutSpliteratorT prefix;
		final Spliterator<InElementT> from;
		final Function<? super InElementT, OutSpliteratorT> function;
		final Factory<InElementT, OutSpliteratorT> factory;
		int characteristics;
		long estimatedSize;

		FlatMapSpliterator(
				@CheckForNull OutSpliteratorT prefix,
				Spliterator<InElementT> from,
				Function<? super InElementT, OutSpliteratorT> function,
				Factory<InElementT, OutSpliteratorT> factory,
				int characteristics,
				long estimatedSize) {
			this.prefix = prefix;
			this.from = from;
			this.function = function;
			this.factory = factory;
			this.characteristics = characteristics;
			this.estimatedSize = estimatedSize;
		}

		/*
		 * The tryAdvance and forEachRemaining in FlatMapSpliteratorOfPrimitive are overloads of these
		 * methods, not overrides. They are annotated @Override because they implement methods from
		 * Spliterator.OfPrimitive (and override default implementations from Spliterator.OfPrimitive or
		 * a subtype like Spliterator.OfInt).
		 */

		@Override
		public final boolean tryAdvance(Consumer<? super OutElementT> action) {
			while (true) {
				if (prefix != null && prefix.tryAdvance(action)) {
					if (estimatedSize != Long.MAX_VALUE) {
						estimatedSize--;
					}
					return true;
				}
				prefix = null;
				if (!from.tryAdvance(fromElement -> prefix = function.apply(fromElement))) {
					return false;
				}
			}
		}

		@Override
		public final void forEachRemaining(Consumer<? super OutElementT> action) {
			if (prefix != null) {
				prefix.forEachRemaining(action);
				prefix = null;
			}
			from.forEachRemaining(
					fromElement -> {
						Spliterator<OutElementT> elements = function.apply(fromElement);
						if (elements != null) {
							elements.forEachRemaining(action);
						}
					});
			estimatedSize = 0;
		}

		@Override
		@CheckForNull
		public final OutSpliteratorT trySplit() {
			Spliterator<InElementT> fromSplit = from.trySplit();
			if (fromSplit != null) {
				int splitCharacteristics = characteristics & ~Spliterator.SIZED;
				long estSplitSize = estimateSize();
				if (estSplitSize < Long.MAX_VALUE) {
					estSplitSize /= 2;
					this.estimatedSize -= estSplitSize;
					this.characteristics = splitCharacteristics;
				}
				OutSpliteratorT result =
						factory.newFlatMapSpliterator(
								this.prefix, fromSplit, function, splitCharacteristics, estSplitSize);
				this.prefix = null;
				return result;
			}
			if (prefix != null) {
				OutSpliteratorT result = prefix;
				this.prefix = null;
				return result;
			}
			return null;
		}

		@Override
		public final long estimateSize() {
			if (prefix != null) {
				estimatedSize = max(estimatedSize, prefix.estimateSize());
			}
			return max(estimatedSize, 0);
		}

		@Override
		public final int characteristics() {
			//noinspection MagicConstant
			return characteristics;
		}
	}

	static final class FlatMapSpliteratorOfObject<InElementT, OutElementT>
			extends FlatMapSpliterator<InElementT, OutElementT, Spliterator<OutElementT>> {
		FlatMapSpliteratorOfObject(
				@CheckForNull Spliterator<OutElementT> prefix,
				Spliterator<InElementT> from,
				Function<? super InElementT, Spliterator<OutElementT>> function,
				int characteristics,
				long estimatedSize) {
			super(
					prefix, from, function, FlatMapSpliteratorOfObject::new, characteristics, estimatedSize);
		}
	}
}
