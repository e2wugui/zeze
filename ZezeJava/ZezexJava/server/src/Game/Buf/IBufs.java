package Game.Buf;

import org.jspecify.annotations.Nullable;

public interface IBufs {
	@Nullable IBuf getBuf(int id);
	void calculateFighter(Game.Fight.IFighter fighter);
}
