package nagginghammer.utils;

public class Turn {
	static private int turn = 0;

	public static void increaseTurn() {
		++turn;
	}

	public static int currentTurn() {
		return turn;
	}
}
