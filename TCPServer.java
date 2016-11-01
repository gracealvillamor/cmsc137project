import java.net.*;
import java.io.*;
import java.util.concurrent.*;

public class TCPServer{
	public static void main(String[] args) throws Exception{
		
		TCPServer myServer = new TCPServer();
		myServer.run();
	}
	
	public void run() throws Exception{
		final int numplayers = 3;
		ServerSocket SERVER = new ServerSocket(60010);

		Socket[] SOCK = new Socket[numplayers];
		InputStreamReader[] reader = new InputStreamReader[numplayers];
		BufferedReader[] IN = new BufferedReader[numplayers];
		PrintStream[] OUT = new PrintStream[numplayers];
		final Runnable[] receiver = new Runnable[numplayers];
		final Runnable[] checker = new Runnable[numplayers];
		String[] name = new String[numplayers];
		// CountDownLatch doneSignal = new CountDownLatch(numplayers);
		// CountDownLatch latch = new CountDownLatch(numplayers);

		for(int i = 0; i<numplayers; i+=1){
			SOCK[i] = SERVER.accept();		
			reader[i] = new InputStreamReader(SOCK[i].getInputStream());
			IN[i] = new BufferedReader(reader[i]);
			OUT[i] = new PrintStream(SOCK[i].getOutputStream());
			name[i] = IN[i].readLine();
			final int f = i;
			receiver[i] = new Runnable(){
				public void run(){
					try{
						while(true){
							try{
								String message = IN[f].readLine();
								System.out.println(message);
								if(message.equals("quit")){
									OUT[f].println();
									break;
								} 
								if(message != null){
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

		for(int i = 0; i<numplayers; i+=1) receiverThread[i].join();

		for(int i = 0; i<numplayers; i+=1){
			OUT[i].close();
			IN[i].close();
			SOCK[i].close();
		}
		SERVER.close();
		
	}
}
