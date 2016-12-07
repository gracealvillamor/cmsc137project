public interface Constants {
	/**
	 * Number of rows and columns of the game grid
	 */
	public static final int ROWS=10;
	public static final int COLS=7;

	/**
	 * Change number of players here
	 */
	public int numplayers = 0;

    public final int SEC = 5; // number of seconds buttons will be disabled when correctly matched
    public final int TIME_LIMIT = 1; //number of minutes the timer for each level will last

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



	// String SERVER_ADDRESS = "";

}