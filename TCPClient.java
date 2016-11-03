import java.net.*;
import java.io.*;
import java.util.*;
import java.awt.*;
import javax.swing.*;
import java.awt.event.*;
import java.awt.image.*;
import javax.imageio.*;

public class TCPClient{
	public static void main(String[] args) throws Exception{

		MainFrame frame = new MainFrame();

		frame.setTitle("Title ng game hehe");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setVisible(true);
		frame.setResizable(false);
		frame.setSize(new Dimension(800,640));

		// TCPClient myClient = new TCPClient();
		// myClient.run();
	}
	public void run(DataModel model) throws Exception{
		Socket SOCK = new Socket("localhost", 60010); //ip address of server and port to connect to
		PrintStream OUT = new PrintStream(SOCK.getOutputStream());
		BufferedReader user = new BufferedReader(new InputStreamReader(System.in));
		InputStreamReader reader = new InputStreamReader(SOCK.getInputStream());
		BufferedReader IN = new BufferedReader(reader);

		//gets the preferred name of client
		//System.out.print("Enter your name: ");
		//String name = user.readLine();
		System.out.println("--- " + model.getNameOfClient() + " ---");
		OUT.println(model.getNameOfClient());

		final Runnable send = new Runnable() {
            public void run() { //for constantly receiving messages from client
                while(true){
                	try{
                		//System.out.print("Enter message: ");
						// String clientMessage = user.readLine();
						if(model.getMessageToSend() != null){
							String clientMessage = model.getMessageToSend();
							System.out.println("Sending... \t" + clientMessage);
							OUT.println(model.getMessageToSend());

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
		return this.latestMessage;
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
class GamePanel extends JPanel implements Runnable{

	private Image img;
	private final DataModel model;
	Thread T = null;

	public GamePanel(DataModel model){
		img = new ImageIcon("bg.png").getImage();
		Dimension size = new Dimension(img.getWidth(null), img.getHeight(null));
	    setSize(size);
	    setLayout(null);

	    this.setLayout(new FlowLayout());


	    this.model = model;

	    JTextArea chatArea = new JTextArea(10, 50);
	    chatArea.setLineWrap(true);
	    chatArea.setWrapStyleWord(true);
	    chatArea.setEditable(false);
	    // chatArea.setBackground(Color.RED);

	    JScrollPane scroll = new JScrollPane(chatArea);
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
		// this.add(userInputField);
		// this.add(chatArea, BorderLayout.CENTER);
	    
	}
	public void run(){
		TCPClient myClient = new TCPClient();
		try{
			System.out.println("!!!!!NAME!!!!!!\t" + this.model.getNameOfClient());
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
class ResultPanel extends JPanel{

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
	    
	    g.drawString("Game panel", 270, 310);

  }
}
class JoinButton extends JButton{
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