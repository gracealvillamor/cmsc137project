import java.net.*;
import java.io.*;
import java.util.*;
import java.awt.*;
import javax.swing.*;
import java.awt.event.*;
import java.awt.image.*;
import javax.imageio.*;

public class TCPClient{
	private boolean isFinished = false;

	public static void main(String[] args) throws Exception{

		MainFrame frame = new MainFrame();

		frame.setTitle("ICS a Match!");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setVisible(true);
		frame.setResizable(false);
		frame.setSize(new Dimension(800,620));
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
		cards.setSize(new Dimension(800, 620));
		cards.setOpaque(true);

		cards.add(menuPanel, "menu");
		cards.add(gamePanel, "game");
		cards.add(resultPanel, "result");

		actionMethods method = new actionMethods();

		//BUTTONS IN MENUPANEL
		JoinButton join = new JoinButton();
		join.setActionCommand("join_game");
		join.addActionListener(method);

		menuPanel.add(join, BorderLayout.SOUTH);

		Container container = getContentPane();
		container.setPreferredSize(new Dimension(800,620));
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
	private JPanel panel;

	public MenuPanel(DataModel model){

	    img = new ImageIcon("bg.png").getImage();
	    Dimension size = new Dimension(img.getWidth(null), img.getHeight(null));
	    setSize(size);
	    setLayout(null);

	    ImagePanel logo = new ImagePanel();

		nameField = new JTextField("Enter your name",100);
		Font bigFont = nameField.getFont().deriveFont(Font.PLAIN, 20f);
    	nameField.setFont(bigFont);
		nameField.setSize(600,40);
		nameField.setLocation(100, 230);

		panel = new JPanel();
		panel.setSize(new Dimension(480,200));
		panel.setLayout(new BorderLayout());
		panel.add(logo, BorderLayout.NORTH);
		panel.setLocation(175, 10);
    	this.add(panel);
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
	private JPanel eastContainer, chatContainer, gameContainer, resultContainer;
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

		resultContainer = new JPanel();
		resultContainer.setSize(new Dimension(200,100));
		JLabel jlabel = new JLabel("Results");
		resultContainer.setLayout(new BorderLayout());
		resultContainer.add(jlabel);

	    chatArea = new JTextArea(10, 30);
	    chatArea.setLineWrap(true);
	    chatArea.setWrapStyleWord(true);
	    chatArea.setEditable(false);

	    scroll = new JScrollPane(chatArea);
		// scroll.setPreferredSize(new Dimension(700,100));
		scroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
	    

	    final JTextField userInputField = new JTextField(30);
	    userInputField.setSize(100,200);
		//userInputField.setLocation(100, 200);
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

		QuitChatButton quitButton = new QuitChatButton();

		eastContainer = new JPanel();
		eastContainer.setSize(new Dimension(200,100));
		eastContainer.setLayout(new BorderLayout());
		
		chatContainer.add(scroll,BorderLayout.CENTER);
		chatContainer.add(userInputField, BorderLayout.SOUTH);
		chatContainer.add(quitButton, BorderLayout.NORTH);
		eastContainer.add(chatContainer, BorderLayout.SOUTH);
		eastContainer.add(resultContainer, BorderLayout.NORTH);
		this.add(eastContainer, BorderLayout.EAST);
		
		gameProper = new GameProperPanel();
		gameContainer = new JPanel();
		gameContainer.setSize(new Dimension(705,520));
		gameContainer.setLayout(new GridLayout());
		gameContainer.add(gameProper);
		this.add(gameContainer, BorderLayout.WEST);
	    
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
class GameProperPanel extends JPanel implements MouseListener{
	JButton[][] buttons = new JButton[12][5];

	ImageIcon i1 = new ImageIcon("i1.png");
	ImageIcon i2 = new ImageIcon("i2.png");
	ImageIcon i3 = new ImageIcon("i3.png");
	ImageIcon i4 = new ImageIcon("i4.png");

	int clicks = 0;
	LinkedList<Integer> previousIndices;

	public GameProperPanel(){
		this.setLayout(new GridLayout(12,5));

		for(int i=0; i<12; i++){
			for(int j=0; j<5; j++){
				buttons[i][j] = new JButton(generateRandomImage());
				buttons[i][j].addMouseListener(this);
				this.add(buttons[i][j]);
			}
		}


	}
	public void mouseClicked(MouseEvent evt){
		clicks += 1;

		

		// buttons[indices.get(0)][indices.get(1)];
		if(clicks == 1){
			previousIndices = getIndicesOfButton(buttons, (JButton)evt.getComponent());
		}
		else{
			LinkedList<Integer> indices = getIndicesOfButton(buttons, (JButton)evt.getComponent());

			System.out.println(previousIndices);
			System.out.println(indices);

			//swap previously clicked and just clicked buttons
			JButton prevClickedButton = buttons[previousIndices.get(0)][previousIndices.get(1)];
			buttons[previousIndices.get(0)][previousIndices.get(1)] = buttons[indices.get(0)][indices.get(1)];
			buttons[indices.get(0)][indices.get(1)] = prevClickedButton;

			this.removeAll();
			for(int i=0; i<5; i++){
				for(int j=0; j<5; j++){
					this.add(buttons[i][j]);
				}
			}
			this.revalidate();
			this.repaint();
			clicks = 0;
		} 
	}
	
	public void mousePressed(MouseEvent e){}	
	public void mouseReleased(MouseEvent e){}	
	public void mouseEntered(MouseEvent e){}	
	public void mouseExited(MouseEvent e){}	
			

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
		for(int i=0; i<5; i++){
			for(int j=0; j<5; j++){
				if(button == buttonArray[i][j])
					return new LinkedList<Integer>(Arrays.asList(i, j));
			}
		}
		return null;
	}




	public void paintComponent(Graphics g) {
		Graphics2D g2d = (Graphics2D)g;

		this.setSize(805,620);
		//this.setLocation(200,300);
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

class QuitChatButton extends JButton{ //button in MenuPanel
	public QuitChatButton(){
		super("Quit Chat");
		this.setSize(30,15);
		this.setForeground(Color.RED);
	   	this.setBorderPainted(false);
	    this.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
	}
}

class ImagePanel extends JPanel{
	private Image logo; 

	public ImagePanel(){
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