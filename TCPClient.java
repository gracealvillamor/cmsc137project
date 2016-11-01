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
        final Runnable receive = new Runnable() {
            public void run() {
                while(true){
                	try{
                		String message = IN.readLine();
                		System.out.println(message);
                	}catch(IOException e){}
                	
                }
            }
        };

		Thread sendThread = new Thread(send);
		Thread receiveThread = new Thread(receive);

		sendThread.start();
		receiveThread.start();
		
		if(sendThread.getState()==Thread.State.TERMINATED) {
			System.out.println("close");
			System.out.println(sendThread.getState());
			SOCK.close();
		}
	}
}