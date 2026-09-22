package Game.Buf;

public interface IBufs {
	IBuf getBuf(int id);
	void calculateFighter(Game.Fight.IFighter fighter);
}
