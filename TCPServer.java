import java.net.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;

public class TCPServer implements Constants{
	public static void main(String[] args) throws Exception{
		
		TCPServer tcpServer = new TCPServer();
		UDPServer udpServer = new UDPServer();

		tcpServer.run();
		udpServer.run();
	}
	
public void run() throws Exception{

		// final int numplayers = 2; //number of players; must be changed
		ServerSocket SERVER = new ServerSocket(60010); //port to connect to

		Socket[] SOCK = new Socket[numplayers];
		InputStreamReader[] reader = new InputStreamReader[numplayers];
		BufferedReader[] IN = new BufferedReader[numplayers];
		PrintStream[] OUT = new PrintStream[numplayers];
		final Runnable[] receiver = new Runnable[numplayers];
		final Runnable[] checker = new Runnable[numplayers];
		String[] name = new String[numplayers];

		final Runnable tcpRunnable = new Runnable() {
            public void run() {
					// CountDownLatch doneSignal = new CountDownLatch(numplayers);
					// CountDownLatch latch = new CountDownLatch(numplayers);


					for(int i = 0; i<numplayers; i+=1){
						try{
							SOCK[i] = SERVER.accept();
							reader[i] = new InputStreamReader(SOCK[i].getInputStream());
							IN[i] = new BufferedReader(reader[i]);
							OUT[i] = new PrintStream(SOCK[i].getOutputStream());
							name[i] = IN[i].readLine();		
						}catch(IOException e){}
						System.out.println("!!!NAME!!! " + name[i]);
						final int f = i; //current iteration
						receiver[i] = new Runnable(){
							public void run(){
								try{
									while(true){
										try{
											String message = IN[f].readLine(); //read message from client
											System.out.println(message);
											if(message.equals("quit")){ // quits the client
												OUT[f].println();
												break;
											} 
											if(message != null){ //broadcasts message of client
												for(int j = 0; j<numplayers; j+=1){
														if(j!=f) OUT[j].println(name[f] + ": " + message);
												}
											}
										}catch(IOException e){}
										
									}
									//latch.countDown();
								}catch(Exception e) {}
								

							}
						};
					}
					
					final Thread[] receiverThread = new Thread[numplayers];

					for(int i = 0; i<numplayers; i+=1){
						receiverThread[i] = new Thread(receiver[i]);
						receiverThread[i].start();
						
					}

					//doneSignal.await();

					try{
						//waits for all threads to terminate
						for(int i = 0; i<numplayers; i+=1) receiverThread[i].join();
					}catch(InterruptedException e){}

					try{
						for(int i = 0; i<numplayers; i+=1){ 
							OUT[i].close();
							IN[i].close();
							SOCK[i].close();
						}
						SERVER.close();
					}catch(IOException e){}
            }
        };
        final Thread tcpThread = new Thread(tcpRunnable);
        tcpThread.start();
		
	}
}
class UDPServer implements Constants{

	private GameState game;
	private DatagramSocket serverSocket;
	private int playerCount = 0;

	public void broadcast(String msg){
		for(Iterator ite=game.getPlayers().keySet().iterator();ite.hasNext();){
			String name=(String)ite.next();
			NetPlayer player=(NetPlayer)game.getPlayers().get(name);			
			send(player,msg);	
		}
	}


	/**
	 * Send a message to a player
	 * @param player
	 * @param msg
	 */
	public void send(NetPlayer player, String msg){
		DatagramPacket packet;	
		byte buf[] = msg.getBytes();		
		packet = new DatagramPacket(buf, buf.length, player.getAddress(),player.getPort());
		try{
			serverSocket.send(packet);
		}catch(IOException ioe){
			ioe.printStackTrace();
		}
	}

	public UDPServer getSelf(){
		return this;
	}
	
	public void run() throws Exception{
        
        serverSocket = new DatagramSocket(60011);

		final Runnable udpRunnable = new Runnable() {
            public void run() {
				// serverSocket.setSoTimeout(1000);

				game = new GameState();
				int gameStage = WAITING_FOR_PLAYERS;

				while(true){System.out.println("PASOK BA");
					byte[] buf = new byte[256];
					DatagramPacket packet = new DatagramPacket(buf, buf.length);

					try{
		     			serverSocket.receive(packet);
					}catch(IOException e){}

					String playerData = new String(buf);
					System.out.println(playerData);
					playerData = playerData.trim();

					switch(gameStage){
						case WAITING_FOR_PLAYERS:
							if (playerData.startsWith("CONNECT")){ System.out.println("HUY");
								String tokens[] = playerData.split(":");
								NetPlayer player=new NetPlayer(tokens[1],packet.getAddress(),packet.getPort());
								System.out.println("Player connected: "+tokens[1]);
								game.update(tokens[1].trim(),player);
								broadcast("CONNECTED "+tokens[1]);
								playerCount++;

								System.out.println("PLAYERS " + playerCount + "numplayers " + numplayers);
								if (playerCount==numplayers){System.out.println("equality????");
									gameStage=GAME_START;
									System.out.println("Game State: START");
									broadcast("START");
									gameStage=IN_PROGRESS;
									String arr = game.generateInitialBoard();
									broadcast("INITIAL " + arr);
									broadcast("SCORES:" + game.getStringPlayers());
									//send random images array here
								}
							}
							break;

						case GAME_START:
							System.out.println("Game State: START");
							broadcast("START");
							gameStage=IN_PROGRESS;
							break;

						case IN_PROGRESS:
							//System.out.println("Game State: IN_PROGRESS");
							  
							//Player data was received!
							if (playerData.startsWith("SWAP")){
								//Tokenize:
								//The format: PLAYER <player name> <x> <y>
								String[] playerInfo = playerData.split("/");
								
								String[] stringPrevIndices = playerInfo[0].split(":");
								LinkedList<Integer> prevIndices = new LinkedList<Integer>(Arrays.asList(Integer.parseInt(stringPrevIndices[1]), Integer.parseInt(stringPrevIndices[2]))); 
								
								String[] stringIndices = playerInfo[1].split(":");
								LinkedList<Integer> indices = new LinkedList<Integer>(Arrays.asList(Integer.parseInt(stringIndices[0]), Integer.parseInt(stringIndices[1]))); 

								NetPlayer player = (NetPlayer)game.getPlayers().get(stringIndices[2]); //name of client
								game.swapButtons(getSelf(), player, prevIndices, indices);
								System.out.println("===\n" + game.getStringPlayers() + "\n===\n");
				
							}else if(playerData.startsWith("TIMEUP")){
								String[] playerInfo = playerData.split(" ");
								if(game.getLevel() == Integer.parseInt(playerInfo[1])){
									broadcast("ELIMINATE:" + game.getLowestPlayer());
									System.out.println("ELIMINATE:" + game.getLowestPlayer());
									game.eliminateLowestPlayer();
									broadcast("SCORES:" + game.getStringPlayers());
									System.out.println("SCORES:" + game.getStringPlayers());
									game.levelUp();
								}

							}
							break;
					}

				}
            }
        };
        final Thread udpThread = new Thread(udpRunnable);
        udpThread.start();
		
	}
}
class GameState implements Constants{
	/**
	 * This is a map(key-value pair) of <player name,NetPlayer>
	 */
	private Map players=new HashMap();
	private int rows, cols;
	private int[][] board;
	private int level = 1;
	private String lowest = null;
	
	/**
	 * Simple constructor
	 *
	 */
	public GameState(){
		this.rows = ROWS;
		this.cols = COLS;
		this.board = new int[ROWS][COLS];
	}
	
	/**
	 * Update the game state. Called when player moves
	 * @param name
	 * @param player
	 */

	public String generateInitialBoard(){
		String retval = "";

		for(int i = 0; i < this.rows; i++){
			for(int j = 0; j < this.cols; j++){
				this.board[i][j] = (new Random()).nextInt(4) + 1;
			}
		}

		while(removeMatches(null, null, false));

		for(int i = 0; i < this.rows; i++){
			for(int j = 0; j < this.cols; j++){
				System.out.print(this.board[i][j] + " ");
				retval += this.board[i][j];
			}
			System.out.println();
		}

		return retval;
	}
	public int getLevel(){
		return this.level;
	}
	public void levelUp(){
		this.level += 1;
	}
	public String stringify(){
		String retval = "";

		for(int i = 0; i < this.rows; i++){
			for(int j = 0; j < this.cols; j++){
				retval += this.board[i][j];
			}
			System.out.println();
		}

		return retval;
	}
	public int[] getColumn(int col){
		int[] colArr = new int[ROWS];

		
		for(int i=0; i<ROWS; i++){
			colArr[i] = this.board[i][col];
		}

		return colArr;
	}
	public LinkedList<Integer> getMatchesFromArray(int[] intArray){
		LinkedList<Integer> list = new LinkedList<Integer>();

		int cnt = 1; //number of consecutive duplicates

		for(int i = 0; i < (intArray.length-1); i++){
			if(intArray[i] == intArray[i+1]){
				// System.out.println("nice " + i + ": " + intArray[i]);
				cnt++;

				if((i+1) == (intArray.length-1) && cnt > 2){
					int startIndex = i - cnt + 2;
					list.add(startIndex);
					list.add(cnt);
				}
			}else{
				if(cnt > 2){
					int startIndex = i - cnt + 1;
					list.add(startIndex);
					list.add(cnt);
				}
				cnt = 1;
			}	
		}

		return list;
	}
	public void swapButtons(UDPServer udpServer, NetPlayer player, LinkedList<Integer> prev, LinkedList<Integer> current){

		//swap previously clicked and just clicked buttons
		int prevClickedButton = this.board[prev.get(0)][prev.get(1)];
		this.board[prev.get(0)][prev.get(1)] = this.board[current.get(0)][current.get(1)];
		this.board[current.get(0)][current.get(1)] = prevClickedButton;

		while(removeMatches(udpServer, player, true));

	}
	public int getEquivalentScore(int count){
		return ((count-2) * 10);
	}
	public boolean removeMatches(UDPServer udpServer, NetPlayer player, boolean withScore){

		final Thread[] rowcheckerThread = new Thread[ROWS];
		final Thread[] colcheckerThread = new Thread[COLS];

		final Runnable[] rowchecker = new Runnable[ROWS];
		final Runnable[] colchecker = new Runnable[COLS];

		int matchChecker = 0;
		int newMatchChecker = matchChecker;

	    for(int r = 0; r< COLS; r++){ // change this so that the bigger value bet. ROWS & cols will be the basis

			final LinkedList<Integer> rowMatches = getMatchesFromArray(this.board[r]);
			final LinkedList<Integer> colMatches = getMatchesFromArray(getColumn(r));

			newMatchChecker += (rowMatches.size() / 2) + (colMatches.size() / 2);

			final int temp = r;

			rowchecker[r] = new Runnable() {
	            public void run() { //for constantly receiving messages from server
	                if(rowMatches.size() > 0){ //get matches from each row
						for(int a = 0; a < rowMatches.size(); a++){
							if(a % 2 == 0){ 
								int startIndex = rowMatches.get(a);
								int endIndex = startIndex + rowMatches.get(a+1) -1;

								
								for(int i = startIndex; i <= endIndex; i++){
									for(int j = temp; j > 0; j--){
										board[j][i] = board[j-1][i];
									}
									board[0][i] = (new Random()).nextInt(4) + 1;
								}

								if(withScore) {
									udpServer.broadcast("REMOVEROW " + temp + " " + startIndex + " " + endIndex + " " + stringify());
									player.updateScore(getEquivalentScore(endIndex - startIndex + 1));
									System.out.println("---diff: " + (endIndex - startIndex + 1) + "  ---eq: "+ getEquivalentScore(endIndex - startIndex + 1));
									udpServer.broadcast("SCORES:" + getStringPlayers());
								}
							}
						}
					}
	            }
	        };

	        colchecker[r] = new Runnable() {
	            public void run() { //for constantly receiving messages from server
	                if(colMatches.size() > 0){ //get matches from each column
						for(int a = 0; a < colMatches.size(); a++){
							if(a % 2 == 0){ 
								int startIndex = colMatches.get(a);
								int endIndex = startIndex + colMatches.get(a+1) -1;
								
								
								for(int i = endIndex, j = startIndex-1; j >= 0; i--, j--){
									board[i][temp] = board[j][temp];
								}

								for(int b = endIndex - startIndex; b >= 0; b--){
									board[b][temp] = (new Random()).nextInt(4) + 1;
								}

								
								if(withScore) {
									udpServer.broadcast("REMOVECOL " + temp + " " + startIndex + " " + endIndex + " " + stringify());
									player.updateScore(getEquivalentScore(endIndex - startIndex + 1));
									System.out.println("---diff: " + (endIndex - startIndex + 1) + "  ---eq: "+ getEquivalentScore(endIndex - startIndex + 1));
									udpServer.broadcast("SCORES:" + getStringPlayers());
								}
							}
						}
					}
	            }
	        };

	        rowcheckerThread[r] = new Thread(rowchecker[r]);
	        colcheckerThread[r] = new Thread(colchecker[r]);

	        rowcheckerThread[r].start();
	        colcheckerThread[r].start();
	    }
		
		try{
			for(int i = 0; i<COLS; i+=1){
				rowcheckerThread[i].join();
				colcheckerThread[i].join();
			} 
		}catch(InterruptedException e){
			e.printStackTrace();
		}

		if(matchChecker == newMatchChecker){
			return false;
		}else{
			matchChecker = newMatchChecker;
			return true;
		}
	}
	public void update(String name, NetPlayer player){
		players.put(name,player);
	}
	
	/**
	 * String representation of this object. Used for data transfer
	 * over the network
	 */
	public String toString(){
		String retval="";
		for(Iterator ite=players.keySet().iterator();ite.hasNext();){
			String name=(String)ite.next();
			NetPlayer player=(NetPlayer)players.get(name);
			retval+=player.toString()+":";
		}
		return retval;
	}
	
	/**
	 * Returns the map
	 * @return
	 */
	public Map getPlayers(){
		return players;
	}

	public String getStringPlayers(){
		String retval = players.size() + ":";
		for(Iterator ite=players.keySet().iterator();ite.hasNext();){
			String name=(String)ite.next();
			NetPlayer player=(NetPlayer)players.get(name);			
			retval += name + ":" + player.getScore() + "/";
		}
		return retval;
	}

	public void eliminateLowestPlayer(){
		players.remove(this.lowest);
	}

	public boolean checkLowestDuplicate(NetPlayer lowestPlayer){
		for(Iterator ite=players.keySet().iterator();ite.hasNext();){
			String name=(String)ite.next();
			NetPlayer player=(NetPlayer)players.get(name);			
			if(player.getScore() == lowestPlayer.getScore() && 
				!(player.getName().equals(lowestPlayer.getName()))){ //duplicate lowest score but different players
					this.lowest = null;
					return true;
			}
		}

		return false;
	}
	public String getLowestPlayer(){
		NetPlayer lowestPlayer = (NetPlayer)players.get(players.keySet().toArray()[0]);

		for(Iterator ite=players.keySet().iterator();ite.hasNext();){
			String name=(String)ite.next();
			NetPlayer player=(NetPlayer)players.get(name);			
			if(player.getScore() < lowestPlayer.getScore()){
				lowestPlayer = player;
			}
		}

		if(!checkLowestDuplicate(lowestPlayer)) //no duplicate of lowest score
			this.lowest = lowestPlayer.getName();

		return this.lowest;
	}
}

class NetPlayer {
	/**
	 * The network address of the player
	 */
	private InetAddress address;
	
	/**
	 * The port number of  
	 */
	private int port;
	
	/**
	 * The name of the player
	 */
	private String name;
	
	/**
	 * The position of player
	 */
	private int x,y;

	private int score = 0;

	/**
	 * Constructor
	 * @param name
	 * @param address
	 * @param port
	 */
	public NetPlayer(String name,InetAddress address, int port){
		this.address = address;
		this.port = port;
		this.name = name;
	}

	/**
	 * Returns the address
	 * @return
	 */
	public InetAddress getAddress(){
		return address;
	}

	/**
	 * Returns the port number
	 * @return
	 */
	public int getPort(){
		return port;
	}

	/**
	 * Returns the name of the player
	 * @return
	 */
	public String getName(){
		return name;
	}
	
	public void updateScore(int score){
		this.score += score;
	}
	
	public int getScore(){
		return this.score;
	}
	/**
	 * Sets the X coordinate of the player
	 * @param x
	 */
	public void setX(int x){
		this.x=x;
	}
	
	
	/**
	 * Returns the X coordinate of the player
	 * @return
	 */
	public int getX(){
		return x;
	}
	
	
	/**
	 * Returns the y coordinate of the player
	 * @return
	 */
	public int getY(){
		return y;
	}
	
	/**
	 * Sets the y coordinate of the player
	 * @param y
	 */
	public void setY(int y){
		this.y=y;		
	}

	/**
	 * String representation. used for transfer over the network
	 */
	public String toString(){
		String retval="";
		retval+="PLAYER ";
		retval+=name+" ";
		retval+=x+" ";
		retval+=y;
		return retval;
	}	
}
