import java.net.*;
import java.io.*;

public class TCPClient{
	public static void main(String[] args) throws Exception{
		TCPClient myClient = new TCPClient();
		myClient.run();
	}
	public void run() throws Exception{
		Socket SOCK = new Socket("localhost", 60010); //ip address of server and port to connect to
		PrintStream OUT = new PrintStream(SOCK.getOutputStream());
		BufferedReader user = new BufferedReader(new InputStreamReader(System.in));
		InputStreamReader reader = new InputStreamReader(SOCK.getInputStream());
		BufferedReader IN = new BufferedReader(reader);

		//gets the preferred name of client
		System.out.print("Enter your name: ");
		String name = user.readLine();
		OUT.println(name);

		final Runnable send = new Runnable() {
            public void run() {
                while(true){
                	try{
                		//System.out.print("Enter message: ");
						String clientMessage = user.readLine();
						OUT.println(clientMessage);

						if((clientMessage.equals("quit"))){ //closes readers if the client has already quit
							IN.close();
							OUT.close();
							break;
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