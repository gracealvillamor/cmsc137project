import java.net.*;
import java.io.*;

public class TCPClient{
	public static void main(String[] args) throws Exception{
		TCPClient myClient = new TCPClient();
		myClient.run();
	}
	public void run() throws Exception{
		Socket SOCK = new Socket("localhost", 60010);
		PrintStream OUT = new PrintStream(SOCK.getOutputStream());
		BufferedReader user = new BufferedReader(new InputStreamReader(System.in));
		InputStreamReader reader = new InputStreamReader(SOCK.getInputStream());
		BufferedReader IN = new BufferedReader(reader);
		final Thread sendThread, receiveThread;

		System.out.print("Enter your name: ");
		String name = user.readLine();
		OUT.println(name);
		
		final Runnable send = new Runnable() {
            public void run() {
                while(true){
                	try{
                		//System.out.print("Enter message: ");
						String message1 = user.readLine();
						OUT.println(message1);

						if((message1.equals("quit"))){
							IN.close();
							OUT.close();
							break;
						} 
                	}catch(IOException e){}
                	
                }

            }
        };

        sendThread = new Thread(send);

        final Runnable receive = new Runnable() {
            public void run() {
                while(true){
                	if(sendThread.getState()==Thread.State.TERMINATED) break;
                	try{
                		String message = IN.readLine();
                		System.out.println(message);
                	}catch(IOException e){}
                	
                }
            }
        };
		
		receiveThread = new Thread(receive);

		sendThread.start();
		receiveThread.start();
		
		while(true){
			if(sendThread.getState()==Thread.State.TERMINATED) {
				SOCK.close();
				break;
			}
		}
		
	}
}