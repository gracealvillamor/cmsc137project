public interface Constants {
	/**
	 * Number of rows and columns of the game grid
	 */
	public static final int ROWS=5;
	public static final int COLS=5;

	/**
	 * Change number of players here
	 */
	public final int numplayers = 2;
    // public final Timer stopwatch;
    public final int SEC = 5;

    /**
	 * Game states.
	 */
	public static final int GAME_START=0;
	public static final int IN_PROGRESS=1;
	public final int GAME_END=2;
	public final int WAITING_FOR_PLAYERS=3;

	/**
	 * Ports
	 */

	public final int TCP_PORT = 60010;
	public final int UDP_PORT = 60011;

}