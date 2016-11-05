import java.net.*;
import java.io.*;
import java.util.*;
import java.awt.*;
import javax.swing.JButton;
import javax.swing.JTextField;
import javax.swing.JTextArea;
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

public class TCPClient{
	private boolean isFinished = false;

	public static void main(String[] args) throws Exception{

		MainFrame frame = new MainFrame();

		frame.setTitle("Title ng game hehe");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setVisible(true);
		frame.setResizable(false);
		frame.setSize(new Dimension(800,640));
	}
	public boolean isFinished(){
		return isFinished;
	}
	public void run(DataModel model) throws Exception{
		Socket SOCK = new Socket("localhost", 60010); //ip address of server and port to connect to
		PrintStream OUT = new PrintStream(SOCK.getOutputStream());
		BufferedReader user = new BufferedReader(new InputStreamReader(System.in));
		InputStreamReader reader = new InputStreamReader(SOCK.getInputStream());
		BufferedReader IN = new BufferedReader(reader);

		System.out.println("--- " + model.getNameOfClient() + " ---");
		OUT.println(model.getNameOfClient());

		final Runnable send = new Runnable() {
            public void run() { //for constantly receiving messages from client
                while(true){
                	try{
						String clientMessage = model.getMessageToSend();

						System.out.println("\" " + clientMessage + "\" ");
						if(clientMessage != null ){
							
							System.out.println("Sending... \t" + clientMessage);
							OUT.println(clientMessage);

							model.sendMessage(null);

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

        final Runnable receive = new Runnable() {
            public void run() { //for constantly receiving messages from server
                while(true){
                	if(sendThread.getState()==Thread.State.TERMINATED) break;
                	try{
                		String serverMessage = IN.readLine();
                		System.out.println(serverMessage);
                		model.putLatestMessage(serverMessage);
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
				SOCK.close();
				break;
			}
		}
		
	}
}

interface Constants {
	/**
	 * Number of rows and columns of the game grid
	 */
	public static final int ROWS=5;
	public static final int COLS=5;
    // public final Timer stopwatch;
    public final int SEC = 5;
}

class DataModel{
	private String name;
	private String latestMessage;
	private String messageToSend;

	public DataModel(){
		this.name = getNameOfClient();
	}
	public void setNameOfClient(String name){
		this.name = name;
	}
	public String getNameOfClient(){
		return this.name;
	}
	public void putLatestMessage(String message){
		this.latestMessage = message;
	}
	public String getLatestMessage(){
		String message = this.latestMessage;
		this.latestMessage = null;
		return message;
	}
	public void sendMessage(String message){
		this.messageToSend = message;
	}
	public String getMessageToSend(){
		return this.messageToSend;
	}
}

class MainFrame extends JFrame{
	final JPanel cards;
	final MenuPanel menuPanel;
	final GamePanel gamePanel;
	final ResultPanel resultPanel;
	final DataModel model;
	public MainFrame(){
		model = new DataModel();
		menuPanel = new MenuPanel(model);
		gamePanel = new GamePanel(model);
		resultPanel = new ResultPanel(model);

		cards = new JPanel(new CardLayout());
		cards.setSize(new Dimension(800,640));
		cards.setOpaque(true);

		cards.add(menuPanel, "menu");
		cards.add(gamePanel, "game");
		cards.add(resultPanel, "result");

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
	class actionMethods implements ActionListener{ 
		public void actionPerformed(ActionEvent e){
			CardLayout layout = (CardLayout)(cards.getLayout());

			if(e.getActionCommand().equals("join_game")){
				System.out.println("join");
				model.setNameOfClient(menuPanel.getNameOfClient());
				layout.show(cards, "game");
				gamePanel.start();
			}else if(e.getActionCommand().equals("quit_game")){
				layout.show(cards, "menu");
			}
			
		}
	}
}

class MenuPanel extends JPanel{

	private Image img;
	private JTextField nameField;

	public MenuPanel(DataModel model){

	    img = new ImageIcon("bg.png").getImage();
	    Dimension size = new Dimension(img.getWidth(null), img.getHeight(null));
	    setSize(size);
	    setLayout(null);

		nameField = new JTextField("Enter your name",100);
		Font bigFont = nameField.getFont().deriveFont(Font.PLAIN, 20f);
    	nameField.setFont(bigFont);
		nameField.setSize(600,40);
		nameField.setLocation(100, 200);
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
class GamePanel extends JPanel implements Runnable{ //panel showing the game proper

	private Image img;
	private final DataModel model;
	private JTextArea chatArea;
	private JScrollPane scroll;
	private GameProperPanel gameProper;
	Thread T = null;

	public GamePanel(DataModel model){
		img = new ImageIcon("bg.png").getImage();
		Dimension size = new Dimension(img.getWidth(null), img.getHeight(null));
	    setSize(size);
	    setLayout(null);

	    this.setLayout(new FlowLayout());


	    this.model = model;

	    chatArea = new JTextArea(10, 50);
	    chatArea.setLineWrap(true);
	    chatArea.setWrapStyleWord(true);
	    chatArea.setEditable(false);

	    scroll = new JScrollPane(chatArea);
		// scroll.setPreferredSize(new Dimension(700,100));
		scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
	    

	    final JTextField userInputField = new JTextField(30);
	    userInputField.setSize(600,40);
		userInputField.setLocation(100, 200);
		userInputField.addActionListener(new ActionListener(){
		    public void actionPerformed(ActionEvent event){
		        //We get the text from the textfield
		        String fromUser = userInputField.getText();

		        if (fromUser != null) {
		            //We append the text from the user
		            chatArea.append(model.getNameOfClient() + ": " + fromUser + "\n");
		 			model.sendMessage(fromUser);
		 			System.out.println("\n\n\n" + chatArea + "\n\n\n");
		            //The pane auto-scrolls with each new response added
		            chatArea.setCaretPosition(chatArea.getDocument().getLength());
		            //We reset our text field to "" each time the user presses Enter
		            userInputField.setText("");
		        }
		    }
		});

		
		this.add(scroll);
		this.add(userInputField);
		
		gameProper = new GameProperPanel();
		this.add(gameProper, BorderLayout.CENTER);
	    
	}
	public void run(){
		TCPClient myClient = new TCPClient();
		try{
			System.out.println("!!!!!NAME!!!!!!\t" + this.model.getNameOfClient());

			final Runnable checker = new Runnable() {
	            public void run() { //for constantly receiving messages from server
	                while(true){
						System.out.println("yes");
						if(myClient.isFinished()) break;
						String msg = model.getLatestMessage();
						if(msg != null){
							chatArea.append(msg + "\n");
						}
					}
	            }
	        };

	        final Thread checkerThread = new Thread(checker);

			checkerThread.start();

			myClient.run(this.model);
			
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
	    
	    g.drawString("Game panel", 270, 310);

  }
}
class GameProperPanel extends JPanel implements ActionListener, Constants{
	final JButton[][] buttons = new JButton[ROWS][COLS];

	ImageIcon i1 = new ImageIcon("i1.png");
	ImageIcon i2 = new ImageIcon("i2.png");
	ImageIcon i3 = new ImageIcon("i3.png");
	ImageIcon i4 = new ImageIcon("i4.png");

	int clicks = 0;
	LinkedList<Integer> previousIndices;
	int matchChecker= 0;
	boolean hasMatches = true;

	public GameProperPanel(){
		this.setLayout(new GridLayout(ROWS,COLS));

		for(int i=0; i<ROWS; i++){
			for(int j=0; j<COLS; j++){
				buttons[i][j] = new JButton(generateRandomImage());
				buttons[i][j].addActionListener(this);
			}
		}

		while(removeMatches(false));

		for(int i=0; i<ROWS; i++){
			for(int j=0; j<COLS; j++){
				this.add(buttons[i][j]);
			}
		}

	}
	public void actionPerformed(ActionEvent evt){
		clicks += 1;

		

		// buttons[indices.get(0)][indices.get(1)];
		if(clicks == 1){
			previousIndices = getIndicesOfButton(buttons, (JButton)evt.getSource());
		}
		else{
			LinkedList<Integer> indices = getIndicesOfButton(buttons, (JButton)evt.getSource());

			System.out.println(previousIndices);
			System.out.println(indices);

			swapButtons(previousIndices, indices);

			if(getMatchesFromArray(buttons[previousIndices.get(0)]).size() > 0 ||
				getMatchesFromArray(getColumnFromButtons(previousIndices.get(1))).size() > 0 ||
				getMatchesFromArray(buttons[indices.get(0)]).size() > 0 ||
				getMatchesFromArray(getColumnFromButtons(indices.get(1))).size() > 0){
					while(removeMatches(true));
			} else{
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
	}
			
	public void swapButtons(LinkedList<Integer> prev, LinkedList<Integer> current){

		//swap previously clicked and just clicked buttons
		JButton prevClickedButton = buttons[prev.get(0)][prev.get(1)];
		buttons[prev.get(0)][prev.get(1)] = buttons[current.get(0)][current.get(1)];
		buttons[current.get(0)][current.get(1)] = prevClickedButton;

	}

	public ImageIcon generateRandomImage(){
		switch((new Random()).nextInt(4) + 1){
			case 1: return i1;
			case 2: return i2;
			case 3: return i3;
			case 4: return i4;
		}
		return null;
	}

	public LinkedList<Integer> getIndicesOfButton(JButton[][] buttonArray, JButton button){
		for(int i=0; i<ROWS; i++){
			for(int j=0; j<COLS; j++){
				if(button == buttonArray[i][j])
					return new LinkedList<Integer>(Arrays.asList(i, j));
			}
		}
		return null;
	}

	public JButton[] getColumnFromButtons(int col){
		JButton[] colArr = new JButton[5];

		for(int i=0; i<ROWS; i++){
			colArr[i] = buttons[i][col];
		}

		return colArr;
	}

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

	public boolean removeMatches(boolean withScore){

		final Thread[] rowcheckerThread = new Thread[ROWS];
		final Thread[] colcheckerThread = new Thread[COLS];

		final Runnable[] rowchecker = new Runnable[ROWS];
		final Runnable[] colchecker = new Runnable[COLS];

		int newMatchChecker = matchChecker;

		// final Timer[][] stopwatch = new Timer[ROWS+COLS][ROWS];
		

	    for(int r = 0; r< ROWS; r++){ // change this so that the bigger value bet. ROWS & cols will be the basis

			final LinkedList<Integer> rowMatches = getMatchesFromArray(buttons[r]);
			final LinkedList<Integer> colMatches = getMatchesFromArray(getColumnFromButtons(r));

			newMatchChecker += (rowMatches.size() / 2) + (colMatches.size() / 2);

			final int temp = r;

			rowchecker[r] = new Runnable() {
	            public void run() { //for constantly receiving messages from server
	                if(rowMatches.size() > 0){ //get matches from each row
						for(int a = 0; a < rowMatches.size(); a++){
							if(a % 2 == 0){ 
								int startIndex = rowMatches.get(a);
								int endIndex = startIndex + rowMatches.get(a+1) -1;

								//replace the duplicates with new images
								for(int b = startIndex; b <= endIndex; b++){
									buttons[temp][b].setEnabled(false);
								}
								// stopwatch[temp][a/2] = new Timer(SEC * 1000, new EnableListener(buttons));
								// stopwatch[temp][a/2].setRepeats(false);
								// stopwatch[temp][a/2].start();

								final Timer stopwatch = new Timer(SEC * 1000, new EnableListener(buttons));
								stopwatch.setRepeats(false);
								stopwatch.start();
								// new Timer(SEC * 1000, new EnableListener(buttons)).start();
								// stopwatch[temp].join();
								for(int b = startIndex; b <= endIndex; b++){
									buttons[temp][b].setIcon(generateRandomImage());
								}

								
								removeAll();
								for(int i=0; i<ROWS; i++){
									for(int j=0; j<COLS; j++){
										add(buttons[i][j]);
									}
								}
								revalidate();
								repaint();
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

								//replace the duplicates with new images
								for(int b = startIndex; b <= endIndex; b++){
									buttons[b][temp].setEnabled(false);
								}
								// stopwatch[temp + ROWS][a/2] = new Timer(SEC * 1000, new EnableListener(buttons));
								// stopwatch[temp + ROWS][a/2].setRepeats(false);
								// stopwatch[temp + ROWS][a/2].start();

								final Timer stopwatch = new Timer(SEC * 1000, new EnableListener(buttons));
								stopwatch.setRepeats(false);
								stopwatch.start();
								// new Timer(SEC * 1000, new EnableListener(buttons)).start();
								// stopwatch[temp + ROWS].join();
								for(int b = startIndex; b <= endIndex; b++){
									buttons[b][temp].setIcon(generateRandomImage());
								}
								removeAll();
								for(int i=0; i<ROWS; i++){
									for(int j=0; j<COLS; j++){
										add(buttons[i][j]);
									}
								}
								revalidate();
								repaint();
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
			for(int i = 0; i<ROWS; i+=1){
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

		this.setSize(500,400);
		this.setLocation(200,300);
		//g.drawImage(img, 0, 0, null);

		g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON); //to avoid pixelation
	}

}
class ResultPanel extends JPanel{ //panel showing results

	private Image img;

	public ResultPanel(DataModel model){
		img = new ImageIcon("bg.png").getImage();
		Dimension size = new Dimension(img.getWidth(null), img.getHeight(null));
	    setSize(size);
	    setLayout(null);
	}
	public void paintComponent(Graphics g) {
	  	Graphics2D g2d = (Graphics2D)g;
    
    	g.drawImage(img, 0, 0, null);
	    
	    g.drawString("Result panel", 270, 310);

  }
}

class JoinButton extends JButton{ //button in MenuPanel
	public JoinButton(){
		super("Join Game!");
		this.setSize(400,100);
		this.setLocation(200, 280);
		this.setBackground(new Color(220, 84, 9));
		this.setForeground(Color.WHITE);
	   	this.setBorderPainted(false);
	    this.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

	}
}