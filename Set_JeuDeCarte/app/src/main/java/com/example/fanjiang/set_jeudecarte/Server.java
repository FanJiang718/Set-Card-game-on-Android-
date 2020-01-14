package com.example.fanjiang.set_jeudecarte;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Random;
import java.util.Stack;


public class Server {
	private LinkedList<ClientThread> clients = new LinkedList<ClientThread>();
	private ServerThread server;

	Stack<Integer> cards; // simulate card stack
	final int num_cards = 81;
	Random rand = new Random();
	ArrayList<Integer> cardsOnTable = new ArrayList<>(); // current cards on the table
	HashMap<ClientThread, Integer> scores = new HashMap<>(); // record the scores of each player

	public Server(){
        server = new ServerThread();
        server.start();
    }


	class ServerThread extends Thread{

		public void run(){
			ServerSocket serverSocket = createServer(9900);
			// shuffle card stack
			cards = initCards();
			//~~~~~~~~~ pop out 12 or 15 cards
			for(int i=0; i<12;i++){
				cardsOnTable.add(cards.pop());
			}
			if(!Server.existSet(cardsOnTable)){
				for(int i=0; i<3;i++){
					cardsOnTable.add(cards.pop());
				}
			}
			System.out.println("cards initialized");
			//~~~~~~~~~~
			// accept the connection of players
			int i=0;
			while(true){
				Socket s = acceptConnection(serverSocket);
				ClientThread client = new ClientThread(s);
				scores.put(client,0);
				clients.addLast(client);
				client.start();
				System.out.println(""+(++i)+" clients connected to server");
			}


		}
	};

	class ClientThread extends Thread{
		private Socket client;
		PrintWriter out;
		BufferedReader in;

		public ClientThread(Socket socket){
			super();
			this.client = socket;
		}

		public void run(){
			out = connectionOut(client);
			in = connectionIn(client);
			try{
				// the first login message, tell the client current cards on table.
				String message = "LOGIN:";
				for(Integer element: cardsOnTable){
					message += element.toString();
					message += ",";
				}
				message.substring(0,message.length()-1);
				out.println(message);
				out.flush();
				System.out.println("login message: "+message);
				// wait for the message from the client and process it
				while(true){
					String in_message = in.readLine();
					System.out.println("received message from client: "+ in_message);
					processMessage(in_message,this);
				}
			}
			catch(IOException e){
				e.printStackTrace();
			}

		}
	}

	public void endOfGame(){
		int max_score=0;
		ClientThread winner = clients.getFirst();
		for(ClientThread client: clients){
			if(scores.get(client)>= max_score){
				max_score = scores.get(client);
				winner = client;
			}
		}
		String messageToWinner = "END: You Win !";
		winner.out.println(messageToWinner);
		winner.out.flush();
		System.out.println(messageToWinner);
		String messageToLosers = "END: You Loose !";
		for(ClientThread client: clients){
			if(client == winner) continue;
			client.out.println(messageToLosers);
			client.out.flush();
			System.out.println(messageToLosers);
		}
	}

	public synchronized void processMessage(String message, ClientThread s){
		String[] selectedCards = message.split(":")[1].split(",");
		if(selectedCards.length!= 3) throw new RuntimeException("Error: Number of selected cards from player is not 3");
		int[] selectedCards_int = new int[3];
		for(int i=0; i< 3; i++){
			selectedCards_int[i] = Integer.valueOf(selectedCards[i]);
			if(!cardsOnTable.contains((Integer) selectedCards_int[i])) return; // if the selected cards are no longer on table, this command is no longer valid.
		}
		if(isSet(selectedCards_int[0], selectedCards_int[1], selectedCards_int[2])){
			int newCard1=0,newCard2=0,newCard3 = 0;
			if(cardsOnTable.size()==15){ // if there were 15 cards on table
				// remove selected cards from table
				cardsOnTable.remove((Integer) selectedCards_int[0]);
				cardsOnTable.remove((Integer) selectedCards_int[1]);
				cardsOnTable.remove((Integer) selectedCards_int[2]);
				if(!existSet(cardsOnTable)){
					if(cards.isEmpty()){
						// end of the game
						endOfGame();
					}
					else{
						newCard1 = cards.pop();
						newCard2 = cards.pop();
						newCard3 = cards.pop();
						cardsOnTable.add(newCard1);
						cardsOnTable.add(newCard2);
						cardsOnTable.add(newCard3);
						//~~~~~~~~~~~~ if 12 cards on table + 3 new cards do not contain one set, pop out 3 new cards until there exist at least one set on table.
						ArrayList<Integer> tmp = new ArrayList<>();
						while(!existSet(cardsOnTable) && !cards.isEmpty()){
							tmp.add(newCard1);
							tmp.add(newCard2);
							tmp.add(newCard3);
							cardsOnTable.remove((Integer) newCard1);
							cardsOnTable.remove((Integer) newCard2);
							cardsOnTable.remove((Integer) newCard3);
							newCard1 = cards.pop();
							newCard2 = cards.pop();
							newCard3 = cards.pop();
							cardsOnTable.add(newCard1);
							cardsOnTable.add(newCard2);
							cardsOnTable.add(newCard3);
						}
						for(Integer card: tmp){
							cards.push(card);
						}
						if(cards.isEmpty()){
							endOfGame();
							return;
						}
						//~~~~~~~~~~~~~~~~
						// transmit message to selector and other players.
						String messageToSelector = "SUCCESS:";
						messageToSelector +=(""+newCard1+","+newCard2+","+newCard3);
						scores.put(s,scores.get(s)+1);
						s.out.println(messageToSelector);
						System.out.println(messageToSelector);
						String messageToOthers = "OTHERSUCCEEDED:";
						messageToOthers +=(""+selectedCards[0]+","+selectedCards[1]+","+selectedCards[2]+";"+newCard1+","+newCard2+","+newCard3);
						for(ClientThread client: clients){
							if(client == s) continue;
							client.out.println(messageToOthers);
							System.out.println(messageToOthers);
						}
					}
				}
				else{
					// if the left 12 cards have one set, we do not pop new cards.
					String messageToSelector = "SUCCESS: ";
					scores.put(s,scores.get(s)+1);
					s.out.println(messageToSelector);
					System.out.println(messageToSelector);
					String messageToOthers = "OTHERSUCCEEDED:"+selectedCards[0]+","+selectedCards[1]+","+selectedCards[2]+"; ";

					for(ClientThread client: clients){
						if(client == s) continue;
						client.out.println(messageToOthers);
						System.out.println(messageToOthers);
					}
				}
			}
			else{
				cardsOnTable.remove((Integer) selectedCards_int[0]);
				cardsOnTable.remove((Integer) selectedCards_int[1]);
				cardsOnTable.remove((Integer) selectedCards_int[2]);
				if(cards.isEmpty()){
					if(!existSet(cardsOnTable)){
						// end of the game
						endOfGame();
						return;
					}
					else{
						// cards is empty and there still exists set on the table
						String messageToSelector = "SUCCESS:";
						scores.put(s,scores.get(s)+1);
						s.out.println(messageToSelector);
						System.out.println(messageToSelector);
						String messageToOthers = "OTHERSUCCEEDED:"+selectedCards[0]+","+selectedCards[1]+","+selectedCards[2]+"; ";
						for(ClientThread client: clients){
							if(client == s) continue;
							client.out.println(messageToOthers);
							System.out.println(messageToOthers);
						}
					}

				}
				else{
					newCard1 = cards.pop();
					newCard2 = cards.pop();
					newCard3 = cards.pop();
					cardsOnTable.add(newCard1);
					cardsOnTable.add(newCard2);
					cardsOnTable.add(newCard3);
					if(!existSet(cardsOnTable)){
						if(cards.isEmpty()){
							// end of the game
							endOfGame();
						}
						else{
							int newCard4 = cards.pop();
							int newCard5 = cards.pop();
							int newCard6 = cards.pop();
							cardsOnTable.add(newCard4);
							cardsOnTable.add(newCard5);
							cardsOnTable.add(newCard6);
							//~~~~~~~~~~~ if there is no set on the table, pop out new 3 cards until there exists one set. then put other popped cards into card stack.
							ArrayList<Integer> tmp = new ArrayList<>();
							while(!existSet(cardsOnTable) && !cards.isEmpty()){
								tmp.add(newCard4);
								tmp.add(newCard5);
								tmp.add(newCard6);
								cardsOnTable.remove((Integer) newCard4);
								cardsOnTable.remove((Integer) newCard5);
								cardsOnTable.remove((Integer) newCard6);
								newCard4 = cards.pop();
								newCard5 = cards.pop();
								newCard6 = cards.pop();
								cardsOnTable.add(newCard4);
								cardsOnTable.add(newCard5);
								cardsOnTable.add(newCard6);
							}
							for(Integer card: tmp){
								cards.push(card);
							}
							if(!existSet(cardsOnTable)){
								endOfGame();
								return;
							}
							//~~~~~~~~~~~~~~~~~~
							String messageToSelector = "SUCCESS:";
							messageToSelector +=(""+newCard1+","+newCard2+","+newCard3+","+newCard4+","+newCard5+","+newCard6);
							scores.put(s,scores.get(s)+1);
							s.out.println(messageToSelector);
							System.out.println(messageToSelector);
							String messageToOthers = "OTHERSUCCEEDED:";
							messageToOthers +=(""+selectedCards[0]+","+selectedCards[1]+","+selectedCards[2]+";"+newCard1+","+newCard2+","+newCard3+
									","+newCard4+","+newCard5+","+newCard6);
							for(ClientThread client: clients){
								if(client == s) continue;
								client.out.println(messageToOthers);
								System.out.println(messageToOthers);
							}

						}
					}
					else{
						String messageToSelector = "SUCCESS:";
						messageToSelector +=(""+newCard1+","+newCard2+","+newCard3);
						scores.put(s,scores.get(s)+1);
						s.out.println(messageToSelector);
						System.out.println(messageToSelector);
						String messageToOthers = "OTHERSUCCEEDED:";
						messageToOthers +=(""+selectedCards[0]+","+selectedCards[1]+","+selectedCards[2]+";"+newCard1+","+newCard2+","+newCard3);
						for(ClientThread client: clients){
							if(client == s) continue;
							client.out.println(messageToOthers);
							System.out.println(messageToOthers);
						}
					}
				}
			}

		}
		else{
			String messageToSelector = "FAIL: ";
			s.out.println(messageToSelector);
			System.out.println(messageToSelector);
		}
	}

	static public ServerSocket createServer(int server_port){ 	
		try {
			return new ServerSocket(server_port);
		} catch (IOException e) {
			throw new RuntimeException("Impossible d'attendre sur le port "+ server_port);
		}
	}

	static Socket acceptConnection(ServerSocket s) {
		try {
			return s.accept();
		} catch (IOException e) {
			throw new RuntimeException("Impossible de recevoir une connection");
		}
	}

	static Socket establishConnection(String ip, int port) {
		try {
			return new Socket(ip,port);
		} 
		catch (UnknownHostException e){
			throw new RuntimeException("Impossible de resoudre l'adresse");
		}
		catch (IOException e){
			throw new RuntimeException("Impossible de se connecter a l'adresse");	
		}
	}

	static PrintWriter connectionOut(Socket s){
		try {
			return new PrintWriter(s.getOutputStream(),	true);
		} catch (IOException e) {
			throw new RuntimeException("Impossible d'extraire le flux sortant");
		}
	}

	static BufferedReader connectionIn(Socket s){
		try {
			return new BufferedReader(new InputStreamReader(s.getInputStream()));
		} catch (IOException e) {
			throw new RuntimeException("Impossible d'extraire le flux entrant");
		}
	}

	static int valueOf(int number, int color, int filling, int shape) {
		if (number <= 0 || number > 3 ||
				color <= 0 || color > 3 ||
				filling <= 0 || filling > 3 ||
				shape <= 0 || shape > 3) {
			throw new IllegalArgumentException("Characteristics out of range.");
		}
		return number | (color << 2) | (filling << 4) | (shape << 6);
	}

	static boolean isSet(int a, int b, int c) {
		for (int i = 0; i < 4; ++i) {
			if (((a & 0x3) + (b & 0x3) + (c & 0x3)) % 3 != 0) {
				return false;
			}
			a >>= 2;
			b >>= 2;
			c >>= 2;
		}
		return true;
	}
	// shuffle the card stack
	private Stack<Integer> initCards(){
		ArrayList<Integer> cards = new ArrayList<>();
		Stack<Integer> cards_random = new Stack<>();
		int count = 0;
		for(int i =1; i < 4;i++){
			for(int j =1; j<4;j++){
				for(int k=1; k<4;k++){
					for(int l =1;l<4;l++){
						cards.add( valueOf(i, j, k, l));
					}
				}
			}
		}
		count = 0;
		int tmp;
		while(count < num_cards) {
			tmp = rand.nextInt(cards.size());
			cards_random.push(cards.get(tmp));
			count++;
			cards.remove(tmp);

		}
		return cards_random;
	}
	// test if there exist at least one set for a card list
	static boolean existSet(ArrayList<Integer> cardsOnTable){
		for(int i = 0; i < cardsOnTable.size()-2;i++){
			for(int j =i+1; j< cardsOnTable.size()-1;j++){
				for(int k = j+1; k< cardsOnTable.size();k++){
					if(isSet(cardsOnTable.get(i),cardsOnTable.get(j), cardsOnTable.get(k))) return true;
				}
			}
		}
		return false;
	}



	
	public static void main(String args[]){
		System.out.println("Server Thread begins");
        Server s = new Server();
	}

}

