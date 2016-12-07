import java.net.*;
import java.io.*;
import java.util.*;
import java.awt.*;
import java.awt.geom.*;
import javax.swing.JButton;
import javax.swing.BoxLayout;
import javax.swing.JTextField;
import javax.swing.JTextArea;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.ImageIcon;
import java.awt.event.*;
import java.awt.image.*;
import javax.imageio.*;
import java.text.*;

/**
 * Class for sending and receiving chat messages to and fro the server.
 */
public class TCPClient implements Constants{
	private boolean isFinished = false;
	public static void main(String[] args) throws Exception{
		if (args.length != 1){
			System.out.println("Usage: java TCPClient <server address>");
			System.exit(1);
		}
		
		String SERVER_ADDRESS = args[0];

		MainFrame frame = new MainFrame(SERVER_ADDRESS);

		frame.setTitle("ICS na Match!");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setVisible(true);
		frame.setResizable(false);
		frame.setSize(new Dimension(800,640));
	}
	public boolean isFinished(){
		return isFinished;
	}
	public void run(DataModel model) throws Exception{
		
		final Socket SOCK = new Socket(model.getServerAddress(), 60010); //ip address of server and port to connect to
		final PrintStream OUT = new PrintStream(SOCK.getOutputStream());
		final InputStreamReader reader = new InputStreamReader(SOCK.getInputStream());
		final BufferedReader user = new BufferedReader(new InputStreamReader(System.in));
		final BufferedReader IN = new BufferedReader(reader);

		final Runnable tcpRunnable = new Runnable() {
            public void run() { 
                
				System.out.println("--- " + model.getNameOfClient() + " ---");
				OUT.println(model.getNameOfClient());


				//runnable for sending messages constantly to the server
				final Runnable send = new Runnable() {
		            public void run() {
		                while(true){
		                	try{
								String clientMessage = model.getTCPMessageToSend();

								System.out.println("\" " + clientMessage + "\" ");
								if(clientMessage != null ){
									
									System.out.println("Sending... \t" + clientMessage);
									OUT.println(clientMessage);

									model.sendTCPMessage(null);

									if((clientMessage.equals("quit"))){ //closes readers if the client has already quit
										IN.close();
										OUT.close();
										break;
									} 
								}
								

								
		                	}catch(IOException e){}
		                	
		                }

		            }
		        };

		        final Thread sendThread = new Thread(send);

		        //runnable for constantly receiving messages from the server
		        final Runnable receive = new Runnable() {
		            public void run() {
		                while(true){
		                	if(sendThread.getState()==Thread.State.TERMINATED) break;
		                	try{
		                		String serverMessage = IN.readLine();
		                		System.out.println(serverMessage);
		                		model.putTCPLatestMessage(serverMessage);
		                	}catch(IOException e){}
		                	
		                }
		            }
		        };
				
				final Thread receiveThread = new Thread(receive);

				sendThread.start();
				receiveThread.start();
				
				while(true){ //closes socket if client has already quit
					if(sendThread.getState()==Thread.State.TERMINATED) {
						isFinished = true;
						try{
							SOCK.close();
						}catch(IOException e){}
						break;
					}
				}
		
            }
        };

        final Thread tcpThread = new Thread(tcpRunnable);
        tcpThread.start();
		
	}
}

/**
 * Class for receving and sending game data to and from the server
 */
class UDPClient implements Constants{
	DatagramSocket socket;
	boolean connected=false;
	boolean isBoardReady = false;
	String SERVER_ADDRESS;

	//sends packet of data to the server
	public void send(String msg){
		try{
			byte[] buf = msg.getBytes();
        	InetAddress address = InetAddress.getByName(SERVER_ADDRESS);
        	DatagramPacket packet = new DatagramPacket(buf, buf.length, address, UDP_PORT);
        	socket.send(packet);
        }catch(Exception e){}
		
	}

	//checks if the initial game board has already been sent by the server
	public boolean isBoardReady(){
		return isBoardReady;
	}
	public void run(DataModel model) throws Exception{
		
		socket = new DatagramSocket();
		SERVER_ADDRESS = model.getServerAddress();

		//runnable for receiving packets of data from the server
		final Runnable udpRunnable = new Runnable() {
            public void run() {

				while(true){
					if (!connected) send("CONNECT:"+model.getNameOfClient());

					byte[] buf = new byte[256];
					DatagramPacket packet = new DatagramPacket(buf, buf.length);
					
					try{
		     			socket.receive(packet);
					}catch(IOException e){}
					
					String serverData = new String(buf);
					serverData=serverData.trim();

					if (serverData.startsWith("CONNECTED")){

						connected=true;
						System.out.println("Connected.");

					}else if (connected){

						if (serverData.startsWith("PLAYER")){

							String[] playersInfo = serverData.split(":");
							for (int i=0;i<playersInfo.length;i++){
								String[] playerInfo = playersInfo[i].split(" ");
								String pname =playerInfo[1];					
							}

						}else if(serverData.startsWith("INITIAL")){ //initial game board from the server
							String[] data = serverData.split(" ");
							model.setInitialBoard(data[1]);
							isBoardReady = true;
							
						}else if(serverData.startsWith("REMOVEROW")){ //another player attacked horizontally

							String[] data = serverData.split(" ");
							model.getGame().removeRow(Integer.parseInt(data[1]), Integer.parseInt(data[2]), Integer.parseInt(data[3]), data[4]);

						}else if(serverData.startsWith("REMOVECOL")){ //another player matched vertically

							String[] data = serverData.split(" ");
							model.getGame().removeCol(Integer.parseInt(data[1]), Integer.parseInt(data[2]), Integer.parseInt(data[3]), data[4]);

						}else if(serverData.startsWith("SCORES")){ //score updates

							String[] data = serverData.split("/");
							int size = Integer.parseInt(data[0].split(":")[1]);

							Map<String, Integer> players = new HashMap<String, Integer>();
							players.put(data[0].split(":")[2], Integer.parseInt(data[0].split(":")[3]));

							System.out.println("!!!\t" + serverData + "!!!\t");
							for(int i = 1; i < size; i++){
								

								players.put(data[i].split(":")[0], Integer.parseInt(data[i].split(":")[1]));
							}

					        if(players.size() == 1){
					        	// @UITEAM: show winning panel here
					        	// painclude po ng name, score. at level na naachieve ni user
					        	//wag na bigyan ng options si user, ipaclose na yung window kasi di naman magrerestart server pag nag new game sya.
					        	System.out.println("\t\t=============YOU WIN!!!==================");
								model.getFrame().showWinnerPanel();
					        }

					        model.setScores(players);
					        model.getScorePanel().updateScores();
					        model.getPlayersPanel().updatePlayers();

						}else if(serverData.startsWith("ELIMINATE")){ //a player has been eliminated from the game

							String[] data = serverData.split(":");
							System.out.println(model.getNameOfClient() + "\t\t" + data[1]);

							if(model.getNameOfClient().equals(data[1])){
								System.out.println("\t\t=============GAME OVER============");
								// @UITEAM: show game over panel here
								// painclude po ng name, score. at level na naachieve ni user
								// wag na po lagyan ng click somewhere or press something to continue. 
								//wag na bigyan ng options si user, ipaclose na yung window kasi di naman magrerestart server pag nag new game sya.
								
								model.getGame().setVisible(false);
								model.getFrame().showGameOverPanel();
							}else{
								// @UITEAM: remove previous timerPanel from gamePanel and add a new timerPanel
								model.levelUp();
								// model.getMyPlayerPanel().updateLevel();
								model.getTimer().stop();
								model.getTimer().start();
							}
						}
					}
				}
		    }
        };

        final Thread udpThread = new Thread(udpRunnable);
        udpThread.start();

		
	}
}

/**
 * Class that can be accessed by any class.
 * This is where all the data that should be seen by outside classes are passed
 */
class DataModel implements Constants{
	private String name = "";
	private String latestTCPMessage, latestUDPMessage;
	private String messageToSend;
	private String serverAddress;
	private int[][] board;
	private UDPClient udpClient;
	private TCPClient tcpClient;
	private GamePanel gamePanel;
	private GameProperPanel game;
	private ScorePanel scorePanel;
	private PlayersPanel playersPanel;
	private MyPlayerPanel myPlayerPanel;
	private MainFrame mainFrame;
	private CardLayout cardLayout;
	private TimerPanel timer;
	private Map players;
	private int level = 1;
	private int state;

	public DataModel(){
		this.board = new int[ROWS][COLS];
		this.state = IN_PROGRESS;
	}

	// set server address provided by user
	public void setServerAddress(String serverAddress){
		this.serverAddress = serverAddress;
	}

	// get address of server
	public String getServerAddress(){
		return this.serverAddress;
	}

	// set username of client
	public void setNameOfClient(String name){
		this.name = name;
	}

	// get the username of the client
	public String getNameOfClient(){
		return this.name;
	}

	// put the latest chat message from other users
	// used in TCPClient class
	public void putTCPLatestMessage(String message){
		this.latestTCPMessage = message;
	}

	// getter for the latest chat message from the other users
	// used for putting the latest retrieved chat message to the chat area
	// set to null after the message has been retrieved and shown in the chat area
	public String getLatestTCPMessage(){
		String message = this.latestTCPMessage;
		this.latestTCPMessage = null;
		return message;
	}

	// send chat message of the client
	// used when the user sends his/her message from the chat area
	public void sendTCPMessage(String message){
		this.messageToSend = message;
	}

	// get the chat message of client
	// used in TCPClient class to get the message sent by the user in chat area
	public String getTCPMessageToSend(){
		return this.messageToSend;
	}

	// set the initial board that was received from the server
	public void setInitialBoard(String initialBoard){
		int cnt = 0;
		System.out.println("wat " + initialBoard);
		for(int i = 0; i < ROWS; i++){
			for(int j = 0; j < COLS; j++){
				this.board[i][j] = Character.getNumericValue(initialBoard.charAt(cnt)); //conver thus to freaking int
				cnt++;
			}
		}
	}
	// puts the main frame used for all other panels
	public void setFrame(MainFrame mainFrame){
		this.mainFrame = mainFrame;
	}

	// gets the main frame used for all panels
	public MainFrame getFrame(){
		return this.mainFrame;
	}

	// puts the card layout used by the mainframe
	public void setCardLayout(CardLayout layout){
		this.cardLayout = layout;
	}

	// gets the card layout used in main frame
	public CardLayout getCardLayout(){
		return this.cardLayout;
	}
	// gets the initial board sent by the server
	public int[][] getInitialBoard(){
		return this.board;
	}

	// puts the GameProperPanel used for all the other classes to be able to access it
	public void setGame(GameProperPanel game){
		this.game = game;
	}

	// gets the GameProperPanel used
	public GameProperPanel getGame(){
		return this.game;
	}

	public void setGamePanel(GamePanel panel){
		this.gamePanel = panel;
	}

	public GamePanel getGamePanel(){
		return this.gamePanel;
	}
	// puts the TimerPanel used for all the other classes to be able to access it
	public void setTimer(TimerPanel timer){
		this.timer = timer;
	}

	// gets the GameProperPanel used
	public TimerPanel getTimer(){
		return this.timer;
	}

	// puts the ScorePanel used for all the other classes to be able to access it
	public void setScorePanel(ScorePanel panel){
		this.scorePanel = panel;
	}

	// gets the ScorePanel used
	public ScorePanel getScorePanel(){
		return this.scorePanel;
	}

	// puts the PlayerPanel used for all the other classes to be able to access it
	public void setPlayersPanel(PlayersPanel panel){
		this.playersPanel = panel;
	}

	// gets the PlayersPanel used
	public PlayersPanel getPlayersPanel(){
		return this.playersPanel;
	}

	public void setMyPlayerPanel(MyPlayerPanel panel){
		this.myPlayerPanel = panel;
	}

	public MyPlayerPanel getMyPlayerPanel(){
		return this.myPlayerPanel;
	}

	// puts the UDPClient used for all the other classes to be able to access it
	public void setUDPClient(UDPClient udpClient){
		this.udpClient = udpClient;
	}

	// gets the UDPClient used so that any class can send data anytime
	public UDPClient getUDPclient(){
		return this.udpClient;
	}

	// sets scores of the players
	public void setScores(Map<String, Integer> players){
		this.players = players;
	}

	//gets the map of scores of players
	public Map<String, Integer> getScores(){
		return this.players;
	}

	// gets the current level of the game
	public int getLevel(){
		return this.level;
	}

	// increments the current level
	public void levelUp(){
		this.level += 1;
	}

}

/**
 * Container for all the components
 */
class MainFrame extends JFrame{
	final JPanel cards;
	final MenuPanel menuPanel;
	final DataModel model;
	final ResultPanel resultPanel;
	final WinnerPanel winnerPanel;
	private String status = "";
	public MainFrame(String serverAddress){
		model = new DataModel();
		menuPanel = new MenuPanel(model);
		resultPanel = new ResultPanel(model);
		winnerPanel = new WinnerPanel(model);

		model.setServerAddress(serverAddress);

		cards = new JPanel(new CardLayout());
		cards.setSize(new Dimension(800,640));
		cards.setOpaque(true);

		cards.add(menuPanel, "menu");
	    cards.add(resultPanel, "result");
	    cards.add(winnerPanel, "winner");

		actionMethods method = new actionMethods();

		//BUTTONS IN MENUPANEL
		JoinButton join = new JoinButton();
		join.setActionCommand("join_game");
		join.addActionListener(method);

		menuPanel.add(join, BorderLayout.CENTER);

		Container container = getContentPane();
		container.setPreferredSize(new Dimension(800,640));
		container.add(cards, BorderLayout.CENTER);
	}

	public void showGameOverPanel() {
			model.getCardLayout().show(cards, "result");
	}

	public void showWinnerPanel() {
			model.getCardLayout().show(cards, "winner");
	}

	class actionMethods implements ActionListener{ 
		public void actionPerformed(ActionEvent e){
			CardLayout layout = (CardLayout)(cards.getLayout());
			model.setCardLayout(layout);

			if(e.getActionCommand().equals("join_game")){
				System.out.println("join");
				model.setNameOfClient(menuPanel.getNameOfClient());
				GamePanel gamePanel = new GamePanel(model);
				cards.add(gamePanel, "game");
				layout.show(cards, "game");
				gamePanel.start();
			}else if(e.getActionCommand().equals("game_over")){
				// ResultPanel resultPanel = new ResultPanel(model);
				// cards.add(resultPanel, "result");
				// layout.show(cards, "result");
			}
			
		}
	}
}

/**
 * Panel where the user can enter his/her preferred name
 */
class MenuPanel extends JPanel{

	private Image img;
	private JTextField nameField;

	public MenuPanel(DataModel model){

	    img = new ImageIcon("logo.jpg").getImage();
	    Dimension size = new Dimension(img.getWidth(null), img.getHeight(null));
	    setSize(size);
	    setLayout(null);
		
		nameField = new JTextField("Enter your name",100);
		Font bigFont = nameField.getFont().deriveFont(Font.PLAIN, 20f);
    	nameField.setFont(bigFont);
		nameField.setSize(600,40);
		nameField.setLocation(100, 320);

    	this.add(nameField);
	}

	public String getNameOfClient(){
		return nameField.getText();
	}

	public void paintComponent(Graphics g) {
		Graphics2D g2d = (Graphics2D)g;

		g.drawImage(img, 0, 0, null);

		g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON); //to avoid pixelation
	}
}

/**
 * Panel for showing all the players that are still in the game
 */
class PlayersPanel extends JPanel{ 
	private DataModel model;
	JTextArea playersArea;

	public PlayersPanel(DataModel model){
		this.model = model;

		//@UITEAM: paimprove naman po ng itsura nito, kaawa awa e haha (ako may gawa nito kaya panget -geli)
		playersArea = new JTextArea(10, 5);
		this.add(playersArea);
	}

	public void updatePlayers(){
		Map<String, Integer> players = model.getScores();
	    
	    playersArea.setText("");
	    for(Map.Entry<String, Integer> entry : players.entrySet()){
			playersArea.append(entry.getKey() + "\n");
        }

		this.revalidate();
		this.repaint();
	}

	public void paintComponent(Graphics g) {
	  	Graphics2D g2d = (Graphics2D)g;
    	
    	this.setLocation(680,400);
    	

  	}
}

/**
 * Panel for showing all the scores of players that are still in the game
 */
class ScorePanel extends JPanel{
	private DataModel model;
	JTextArea scoreArea;

	public ScorePanel(DataModel model){
		this.model = model;

		//@UITEAM: pasama na rin po sa iimprove, pag mahaba kasi yung name ng users
		//lumalaki din yung scoreArea, hindi sya maganda tingnan. 
		scoreArea = new JTextArea(10, 30);
		this.add(scoreArea);
	}
	public void updateScores(){
		Map<String, Integer> scores = model.getScores();
    	int i = 0;
	    
	    scoreArea.setText("");
	    for(Map.Entry<String, Integer> entry : scores.entrySet()){
			scoreArea.append(entry.getKey() + "\t" + entry.getValue() + "\n");
        }

		this.revalidate();
		this.repaint();
	}
	public void paintComponent(Graphics g) {
	  	Graphics2D g2d = (Graphics2D)g;
  	}

}

/**
 * Panel for showing all the remaining time
 */
class TimerPanel extends JPanel implements Runnable, Constants{
	private DataModel model;
	private Image img;
	private String time = "0" + TIME_LIMIT + ":00";
	private boolean timesUp = false;
	private Thread T;

	public TimerPanel(DataModel model){

		img = new ImageIcon("bg.png").getImage();
	    Dimension size = new Dimension(380, 40);
	    setSize(size);
	    setLayout(null);

	    // @UITEAM: not really sure kung nagpapakita ba tong textArea na to,
	    // pero ang intention ko sana talaga dito ay timer na may black na background
	    // (yung time-bg.png). Anyway, paayos na lang din po itsura.
	    JTextArea playersArea = new JTextArea(10, 35);
		this.add(playersArea);

		this.model = model;
		this.start();
	}
	public void run(){
		long start = System.currentTimeMillis();
        int t = TIME_LIMIT*60;
        long delay = t*1000;

        do{

            int minutes = t/60;
            int seconds = t%60;
            //int seconds = (int) t / 1000;
            SwingUtilities.invokeLater(new Runnable() {
                 public void run()
                 {
                 	DecimalFormat formatter = new DecimalFormat("00");
					String fseconds = formatter.format(seconds);
                    time = ((minutes+ "") +":"+ (fseconds + ""));
                 }
            });
            try { 
            	Thread.sleep(1000);
            	// System.out.println("\t\t\t\t\t\t\t" + time);
		        this.revalidate();
		        this.repaint();
            	t -= 1;
            	delay -= 1000;
             } catch(Exception e) {}
        }while (delay!=0);
        
        // lets the server know that the time limit for the current level is over
        this.model.getUDPclient().send("TIMEUP " + this.model.getLevel());
	}
	public void start(){
		while(T==null){
			T = new Thread(this);
			T.start();
		}
	}
	
	public void stop(){
		while(T != null){
			T = null;
		}
	}
	
	public void paintComponent(Graphics g) {
	  	Graphics2D g2d = (Graphics2D)g;
    
    	g.drawImage(img, 0, 0, this);
	    
	    g.setColor(Color.WHITE);
		g.setFont(new Font("Courier", Font.BOLD, 25));
		g.drawString(time, 30, 20);

    	this.setLocation(0,0);
    	this.setSize(380,40);
  	}
}


/**
 * Panel containing all the components of the game
 */
class GamePanel extends JPanel implements Runnable{ //panel showing the game proper

	private Image img;
	private final DataModel model;
	private JTextArea chatArea;
	private JScrollPane scroll;
	private GameProperPanel gameProper;
	private JPanel eastContainer, chatContainer, gameContainer, container;
	Thread T = null;

	public GamePanel(DataModel model){
		img = new ImageIcon("bg.png").getImage();
		Dimension size = new Dimension(img.getWidth(null), img.getHeight(null));
	    setSize(size);
	    setLayout(null);

	    this.setLayout(new BorderLayout());
	    

	    this.model = model;

	    chatContainer = new JPanel();
		chatContainer.setSize(new Dimension(200,100));
		chatContainer.setLayout(new BorderLayout());

	    chatArea = new JTextArea(10, 32);
	    chatArea.setLineWrap(true);
	    chatArea.setWrapStyleWord(true);
	    chatArea.setEditable(false);

	    scroll = new JScrollPane(chatArea);
		// scroll.setPreferredSize(new Dimension(700,100));
		scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

	    final JTextField userInputField = new JTextField(30);
	    userInputField.setSize(600,130);
		
		userInputField.addActionListener(new ActionListener(){
		    public void actionPerformed(ActionEvent event){
		        //We get the text from the textfield
		        String fromUser = userInputField.getText();

		        if (fromUser != null) {
		            //We append the text from the user
		            chatArea.append(model.getNameOfClient() + ": " + fromUser + "\n");
		 			model.sendTCPMessage(fromUser);
		 			System.out.println("\n\n\n" + chatArea + "\n\n\n");
		            //The pane auto-scrolls with each new response added
		            chatArea.setCaretPosition(chatArea.getDocument().getLength());
		            //We reset our text field to "" each time the user presses Enter
		            userInputField.setText("");
		        }
		    }
		});

		eastContainer = new JPanel();
		eastContainer.setPreferredSize(new Dimension(350,350));
		eastContainer.setLayout(new BorderLayout());
		
		chatContainer.add(scroll,BorderLayout.CENTER);
		chatContainer.add(userInputField, BorderLayout.SOUTH);
		eastContainer.add(chatContainer, BorderLayout.CENTER);
		//this.add(eastContainer, BorderLayout.EAST);	    
	}
	public void run(){

		TCPClient tcpClient = new TCPClient();
		UDPClient udpClient = new UDPClient();

		this.model.setUDPClient(udpClient);
		try{
			System.out.println("!!!!!NAME!!!!!!\t" + this.model.getNameOfClient());

			final Runnable checker = new Runnable() {
	            public void run() { //for constantly receiving messages from server
	                while(true){
						System.out.println("yes");
						if(tcpClient.isFinished()) break;
						String msg = model.getLatestTCPMessage();
						if(msg != null){
							chatArea.append(msg + "\n");
						}
					}
	            }
	        };

	        final Thread checkerThread = new Thread(checker);
			checkerThread.start();

			ScorePanel scorePanel = new ScorePanel(model);
			model.setScorePanel(scorePanel);

			PlayersPanel playersPanel = new PlayersPanel(model);
			model.setPlayersPanel(playersPanel);


			tcpClient.run(this.model);
			udpClient.run(this.model);
			
			//di rin gumagana sa kin withough this "eh" garbage lol
			while(!udpClient.isBoardReady()) System.out.println("eh");

			gameProper = new GameProperPanel(model);
			model.setGame(gameProper);

			TimerPanel timerPanel = new TimerPanel(model);
			model.setTimer(timerPanel);

			MyPlayerPanel myPlayerPanel = new MyPlayerPanel(model);
			model.setMyPlayerPanel(myPlayerPanel);

			container = new JPanel();
			container.setSize(new Dimension(200,620));
			BoxLayout boxlayout = new BoxLayout(container, BoxLayout.Y_AXIS);
			container.setLayout(boxlayout);

			// @UITEAM: pls fix postioning nito, kaya nagbblink yung gawa kong gui ay dahil sa maling positioning
			// make sure po na kita yung chatArea, timerPanel, scorePanel, gameProperPanel, at playersPanel. Maskiw ag na yung quit chat :)
			// ps: yung timerPanel pala ay nirereplace kada new round, thread kasi yung timerPanel, hindi narerevive kailangan ireplace
			// makikita ang pagreplace ng timerPanel sa may ELIMINATE na packet na rereceive
			// pacheck na lang if gumagana yung pagreplace ng timerpanel. basta dapat every level bumabalik sa timelimit
			
			this.add(gameProper, BorderLayout.WEST);
			timerPanel.setMaximumSize( timerPanel.getPreferredSize() );
			eastContainer.setMaximumSize( eastContainer.getPreferredSize() );
			scorePanel.setMaximumSize( scorePanel.getPreferredSize() );
			myPlayerPanel.setMaximumSize( myPlayerPanel.getPreferredSize() );
			container.add(timerPanel);
			container.add(myPlayerPanel);
			container.add(scorePanel);
			container.add(eastContainer);
			this.add(container, BorderLayout.EAST);
			
			timerPanel.start();

			this.revalidate();
			this.repaint();
		}catch(Exception ex){}
	}
	public void start(){
		while(T==null){
			T = new Thread(this);
			T.start();
		}
	}
	public void paintComponent(Graphics g) {
	  	Graphics2D g2d = (Graphics2D)g;
    
    	g.drawImage(img, 0, 0, null);
  	}
}

class Tile{
	private ImageIcon icon;
	private int row, col;

	public Tile(ImageIcon icon, int row, int col){
		this.icon = icon;
		this.row = row;
		this.col = col;
	}
	public void draw(Graphics g){
        Graphics2D g2 = (Graphics2D)g;

    	this.icon.paintIcon(null, g, 10+this.col*63, 7+this.row*60);
	}
}
/**
 * Panel containing all the game buttons
 */
class GameProperPanel extends JPanel implements ActionListener, Constants{
	private final JButton[][] buttons = new JButton[ROWS][COLS];
	private final Image[][] buttonsImg = new Image[ROWS][COLS];
	private final Tile[][] tiles = new Tile[ROWS][COLS];

	int dimension = 52;

	ImageIcon icon1 = new ImageIcon("i1.png");
	Image img1 = icon1.getImage();
	Image newimg1 = img1.getScaledInstance(dimension, dimension, java.awt.Image.SCALE_SMOOTH);
	ImageIcon i1 = new ImageIcon(newimg1);

	ImageIcon icon2 = new ImageIcon("i2.png");
	Image img2 = icon2.getImage();
	Image newimg2 = img2.getScaledInstance(dimension, dimension, java.awt.Image.SCALE_SMOOTH);
	ImageIcon i2 = new ImageIcon(newimg2);

	ImageIcon icon3 = new ImageIcon("i3.png");
	Image img3 = icon3.getImage();
	Image newimg3 = img3.getScaledInstance(dimension, dimension, java.awt.Image.SCALE_SMOOTH);
	ImageIcon i3 = new ImageIcon(newimg3);

	ImageIcon icon4 = new ImageIcon("i4.png");
	Image img4 = icon4.getImage();
	Image newimg4 = img4.getScaledInstance(dimension, dimension, java.awt.Image.SCALE_SMOOTH);
	ImageIcon i4 = new ImageIcon(newimg4);

	ImageIcon icon5 = new ImageIcon("i5.png");
	Image img5 = icon5.getImage();
	Image newimg5 = img5.getScaledInstance(dimension, dimension, java.awt.Image.SCALE_SMOOTH);
	ImageIcon i5 = new ImageIcon(newimg5);

	private UDPClient udpClient;
	private DataModel model;

	int clicks = 0;
	LinkedList<Integer> previousIndices;
	int matchChecker= 0;
	boolean hasMatches = true;

	public GameProperPanel(DataModel model){
		this.model = model;
		this.setLayout(new GridLayout(ROWS,COLS));
		this.udpClient = model.getUDPclient();

		// ganerates images for the board based on the initial board given by the server
		for(int i=0; i<ROWS; i++){
			for(int j=0; j<COLS; j++){
				// buttons[i][j] = new JButton(generateImage(model.getInitialBoard()[i][j]));
				buttons[i][j] = new JButton();
				buttons[i][j].addActionListener(this);
				buttons[i][j].setOpaque(false);
				// buttons[i][j].setBorderPainted(false);
				buttons[i][j].setContentAreaFilled(false);
				buttons[i][j].setMargin(new Insets(25, 25, 25, 25));
				buttons[i][j].setSize(750,60);
				this.add(buttons[i][j]);

				// buttonsImg[i][j] = generateImage(model.getInitialBoard()[i][j]);
			}
		}

		// add the buttons to the panel
		// for(int i=0; i<ROWS; i++){
		// 	for(int j=0; j<COLS; j++){
		// 		this.add(buttons[i][j]);
		// 		tiles[i][j] = new Tile(generateImage(model.getInitialBoard()[i][j]), i, j);
		// 		tiles.draw
		// 		// this.add(buttonsImg[i][j]);
		// 	}
		// }

	}
	public void drawTiles(Graphics g){
		for(int i=0; i<ROWS; i++){
			for(int j=0; j<COLS; j++){
				tiles[i][j] = new Tile(generateImage(model.getInitialBoard()[i][j]), i, j);
				tiles[i][j].draw(g);
			}
		}

	}
	public void actionPerformed(ActionEvent evt){
		// @UITEAM: pahighlight po yung mga buttons na kiniclick ni user
		clicks += 1;

		if(clicks == 1){
			previousIndices = getIndicesOfButton(buttons, (JButton)evt.getSource());
		}
		else{
			LinkedList<Integer> indices = getIndicesOfButton(buttons, (JButton)evt.getSource());

			System.out.println(previousIndices);
			System.out.println(indices);

			if(((previousIndices.get(0) == indices.get(0)) && (Math.abs(previousIndices.get(1) - indices.get(1)) == 1))
				|| ((previousIndices.get(1) == indices.get(1)) && (Math.abs(previousIndices.get(0) - indices.get(0)) == 1))){
				swapButtons(previousIndices, indices); //allow swapping of adjacent buttons only

				if(getMatchesFromArray(buttons[previousIndices.get(0)]).size() > 0 ||
					getMatchesFromArray(getColumnFromButtons(previousIndices.get(1))).size() > 0 ||
					getMatchesFromArray(buttons[indices.get(0)]).size() > 0 ||
					getMatchesFromArray(getColumnFromButtons(indices.get(1))).size() > 0){
						// send to server action of user
						String stringIndices = "SWAP:" + previousIndices.get(0) + ":" +previousIndices.get(1);
						stringIndices += "/" + indices.get(0) + ":" +indices.get(1) + ":" + model.getNameOfClient();
						this.udpClient.send(stringIndices);
				} else{
					// swapping buttons clicked by the user yields to no result; revert the swap
					swapButtons(previousIndices, indices);
				}
				
				this.removeAll();
				for(int i=0; i<ROWS; i++){
					for(int j=0; j<COLS; j++){
						this.add(buttons[i][j]);
					}
				}
				this.revalidate();
				this.repaint();
			
				clicks = 0;
			}

			clicks = 1; //clicked a button that is not adjacent
			previousIndices = getIndicesOfButton(buttons, (JButton)evt.getSource());
		} 
	}
	
	// sets the images for the 2D integer board
	public void setBoard(int[][] board){
		for(int i=0; i<ROWS; i++){
			for(int j=0; j<COLS; j++){
				buttons[i][j].setIcon(generateImage(board[i][j]));
			}
		}
	}

	// swaps previously clicked and just clicked buttons
	public void swapButtons(LinkedList<Integer> prev, LinkedList<Integer> current){
		// @UITEAM: kung kaya po, palagyan po ito ng effects na nagsswap :)
		JButton prevClickedButton = buttons[prev.get(0)][prev.get(1)];
		buttons[prev.get(0)][prev.get(1)] = buttons[current.get(0)][current.get(1)];
		buttons[current.get(0)][current.get(1)] = prevClickedButton;

	}

	public void animateSwap(LinkedList<Integer> prev, LinkedList<Integer> current){

	}

	// generate image according to a given integer
	public ImageIcon generateImage(int val){
		switch(val){
			case 1: return i1;
			case 2: return i2;
			case 3: return i3;
			case 4: return i4;
		}
		return null;
	}

	// gets the indices of a certain button 
	public LinkedList<Integer> getIndicesOfButton(JButton[][] buttonArray, JButton button){
		for(int i=0; i<ROWS; i++){
			for(int j=0; j<COLS; j++){
				if(button == buttonArray[i][j])
					return new LinkedList<Integer>(Arrays.asList(i, j));
			}
		}
		return null;
	}

	// retrieves column of buttons
	public JButton[] getColumnFromButtons(int col){
		JButton[] colArr = new JButton[ROWS];

		for(int i=0; i<ROWS; i++){
			colArr[i] = buttons[i][col];
		}

		return colArr;
	}

	// returns all the matches from an array (3 & above consecutive duplicates)
	public LinkedList<Integer> getMatchesFromArray(JButton[] buttonArray){
		LinkedList<Integer> list = new LinkedList<Integer>();

		int cnt = 1; //number of consecutive duplicates

		for(int i = 0; i < buttonArray.length-1; i++){
			if(((ImageIcon)buttonArray[i].getIcon()).equals((ImageIcon)buttonArray[i+1].getIcon())){
				cnt++;

				if((i+1) == (buttonArray.length-1) && cnt > 2){
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

	// converts stringified board from server into a 2D int array board
	public int[][] getBoardFromString(String board){
		int[][] retBoard = new int[ROWS][COLS];

		int cnt = 0;
		for(int i = 0; i < ROWS; i++){
			for(int j = 0; j < COLS; j++){
				retBoard[i][j] = Character.getNumericValue(board.charAt(cnt)); //conver thus to freaking int
				cnt++;
			}
		}

		return retBoard;
	}

	// removes horizontal matches
	public void removeRow(int row, int startIndex, int endIndex, String board){
		//@UITEAM: kung kaya po, palagyan po ito ng effects. wag na idisable yung buttons,
		//pawalain na lang sila. as in yung parang sa mga candy crush, nawawala yung buttons pagkapinagmatch sila
		for(int b = startIndex; b <= endIndex; b++){
			buttons[row][b].setEnabled(false); 
		}

		final Timer stopwatch = new Timer(SEC * 1000, new EnableListener(buttons));
		stopwatch.setRepeats(false);
		stopwatch.start();

		setBoard(getBoardFromString(board));
	}

	// removes vertical matches
	public void removeCol(int col, int startIndex, int endIndex, String board){
		//@UITEAM: kung kaya po, palagyan po ito ng effects. wag na idisable yung buttons,
		//pawalain na lang sila. as in yung parang sa mga candy crush, nawawala yung buttons pagkapinagmatch sila
		for(int b = startIndex; b <= endIndex; b++){
			buttons[b][col].setEnabled(false); 
		}

		final Timer stopwatch = new Timer(SEC * 1000, new EnableListener(buttons));
		stopwatch.setRepeats(false);
		stopwatch.start();

		setBoard(getBoardFromString(board));
	}

	// reenables button after a specific amount of time
	class EnableListener implements ActionListener{
		JComponent[][] target = new JComponent[ROWS][COLS];

		public EnableListener(JComponent[][] target){
			for(int i =0; i < ROWS; i++){
				for(int j = 0; j < COLS; j++){
					this.target[i][j] = target[i][j];
				}
			}
		}

        @Override
        public void actionPerformed(ActionEvent e) {
            for(int i =0; i < ROWS; i++){
				for(int j = 0; j < COLS; j++){
					this.target[i][j].setEnabled(true);
				}
			}
        }
	}
	public void paintComponent(Graphics g) {
		Graphics2D g2d = (Graphics2D)g;

		drawTiles(g);

		this.setSize(806,640);

		g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON); //to avoid pixelation
	}

}

class ResultPanel extends JPanel{ //panel showing results

	private Image img;
	private DataModel model;
	private String name;

	public ResultPanel(DataModel model){
		img = new ImageIcon("bg.png").getImage();
		Dimension size = new Dimension(img.getWidth(null), img.getHeight(null));
	    setSize(size);
	    setLayout(null);

	    this.model = model;

	    GameOverImagePanel go = new GameOverImagePanel();
	    name = model.getNameOfClient();

	    go.setLocation(200, 10);
    	this.add(go);
	}
	public void paintComponent(Graphics g) {
	  	Graphics2D g2d = (Graphics2D)g;
    
    	g.drawImage(img, 0, 0, null);
    	g.setColor(Color.WHITE);
		g.setFont(new Font("Courier", Font.BOLD, 25));
		g.drawString(name, 200, 300);
  }
}

class WinnerPanel extends JPanel{ //panel showing results

	private Image img;
	private DataModel model;
	private String name;

	public WinnerPanel(DataModel model){
		img = new ImageIcon("bg.png").getImage();
		Dimension size = new Dimension(img.getWidth(null), img.getHeight(null));
	    setSize(size);
	    setLayout(null);

	    this.model = model;

	    WinnerImagePanel go = new WinnerImagePanel();
	    name = model.getNameOfClient();

	    go.setLocation(120, 10);
    	this.add(go);
	}
	public void paintComponent(Graphics g) {
	  	Graphics2D g2d = (Graphics2D)g;
    
    	g.drawImage(img, 0, 0, null);
    	g.setColor(Color.WHITE);
		g.setFont(new Font("Courier", Font.BOLD, 25));
		g.drawString(name, 200, 300);
  }
}

class JoinButton extends JButton{ //button in MenuPanel
	public JoinButton(){
		super("Join Game!");
		this.setSize(400,100);
		this.setLocation(200, 400);
		this.setBackground(new Color(220, 84, 9));
		this.setForeground(Color.WHITE);
	   	this.setBorderPainted(false);
	    this.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

	}
}
// class JoinButton extends JButton{ //button in MenuPanel
// 	public JoinButton(){
// 	    super("Join Game!");

// 		// These statements enlarge the button so that it 
// 		// becomes a circle rather than an oval.
// 	    Dimension size = getPreferredSize();
// 	    size.width = size.height = Math.max(size.width, 
// 	    size.height);
// 	    setPreferredSize(size);
// 	    setBackground(new Color(220, 84, 9));
// 	    setForeground(Color.WHITE);
// 		// This call causes the JButton not to paint 
// 	   	// the background.
// 		// This allows us to paint a round background.
// 	    setContentAreaFilled(false);
// 	}

// 	// Paint the round background and label.
// 	protected void paintComponent(Graphics g) {
// 		if (getModel().isArmed()) {
// 		// You might want to make the highlight color 
// 		// a property of the RoundButton class.
// 			g.setColor(Color.lightGray);
// 		} else {
// 			g.setColor(getBackground());
// 		}
// 		g.fillOval(0, 0, getSize().width-1, getSize().height-1);

// 		// This call will paint the label and the 
// 		// focus rectangle.
// 		super.paintComponent(g);
// 	}

// 	// Paint the border of the button using a simple stroke.
// 	protected void paintBorder(Graphics g) {
// 		g.setColor(getForeground());
// 		g.drawOval(0, 0, getSize().width-1, getSize().height-1);
// 	}

// 	// Hit detection.
// 	Shape shape;
// 	public boolean contains(int x, int y) {
// 	// If the button has changed size, 
// 	// make a new shape object.
// 	if (shape == null || 
// 	  !shape.getBounds().equals(getBounds())) {
// 	  shape = new Ellipse2D.Float(0, 0, 
// 	    getWidth(), getHeight());
// 	}
// 	return shape.contains(x, y);
// 	}
// }

class LogoImagePanel extends JPanel{
	private Image logo; 

	public LogoImagePanel(){
		logo = new ImageIcon("logo.png").getImage();
		Dimension size = new Dimension(logo.getWidth(null), logo.getHeight(null));
	    setSize(size);
	    setLayout(null);
	}

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        this.setSize(480,200);
        g.drawImage(logo, 0, 0, this); // see javadoc for more info on the parameters            
    }

}

class GameOverImagePanel extends JPanel{
	private Image logo; 

	public GameOverImagePanel(){
		logo = new ImageIcon("gameover.png").getImage();
		Dimension size = new Dimension(logo.getWidth(null), logo.getHeight(null));
	    setSize(size);
	    setLayout(null);
	}

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        this.setSize(350,260);
        g.drawImage(logo, 0, 0, this); // see javadoc for more info on the parameters            
    }

}

class WinnerImagePanel extends JPanel{
	private Image logo; 

	public WinnerImagePanel(){
		logo = new ImageIcon("result.png").getImage();
		Dimension size = new Dimension(logo.getWidth(null), logo.getHeight(null));
	    setSize(size);
	    setLayout(null);
	}

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        this.setSize(560,180);
        g.drawImage(logo, 0, 0, this); // see javadoc for more info on the parameters            
    }

}

class MyPlayerPanel extends JPanel{ 
	private DataModel model;
	private Image img;
	private String name;
	private int level;

	public MyPlayerPanel(DataModel model){
		img = new ImageIcon("bg.png").getImage();
		Dimension size = new Dimension(380, 100);
	    setSize(size);
	    setLayout(null);

	    this.model = model;
		this.name = model.getNameOfClient();
		// this.level = model.getLevel();
	}

	public void updateLevel(){
		this.revalidate();
		this.repaint();
		this.model.getGamePanel().revalidate();
		this.model.getGamePanel().repaint();
		System.out.println("\t\t\t\t!!! level = " + model.getLevel());
	}
	public void paintComponent(Graphics g) {
	  	Graphics2D g2d = (Graphics2D)g;
    	
    	g.drawImage(img, 0, 0, this);
    	g.setColor(Color.WHITE);
		g.setFont(new Font("Courier", Font.BOLD, 15));
		g.drawString(name, 10, 50);
		g.drawString("Level " + model.getLevel(), 230, 50);
  	}
}