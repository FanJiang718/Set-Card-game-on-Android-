package com.example.fanjiang.set_jeudecarte;

import android.content.Intent;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Chronometer;
import android.widget.ImageView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Stack;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class TwoPlayer extends AppCompatActivity {

    TableLayout tableLayout;
    TableRow[] tableRows;
    ImageView[] imageViews;
    ImageView[] lastSuccessViews;
    TextView textView;
    final Handler callback = new Handler();
    Chronometer chronometer;

    int score=0;
    LinkedList<ImageView> cards_selected = new LinkedList<ImageView>(); // cards selected by player
    LinkedList<ImageView> cards_toBeUpdate = new LinkedList<ImageView>(); // imageviews need to be updated after the selection
    HashMap<Integer,Integer> cardsID = new HashMap<Integer, Integer>(); // record the mapping of card and imageView's ID
    Lock lock = new ReentrantLock();
    Condition lessThan3Cards = lock.newCondition();

    Socket socket;
    PrintWriter out;
    BufferedReader in;
    String messageFromServer;

    private Runnable success_update = new Runnable() {

        public void run(){
            ImageView img1 = cards_toBeUpdate.poll();
            ImageView img2 = cards_toBeUpdate.poll();
            ImageView img3 = cards_toBeUpdate.poll();
            CardDrawable card1 = (CardDrawable) img1.getDrawable();
            CardDrawable card2 = (CardDrawable) img2.getDrawable();
            CardDrawable card3 = (CardDrawable) img3.getDrawable();
            card1.setSelected(0);
            card2.setSelected(0);
            card3.setSelected(0);
            for(int i=0; i< lastSuccessViews.length;i++){
                lastSuccessViews[i].invalidate();
            }
            lastSuccessViews[0].setImageDrawable(card1);
            lastSuccessViews[1].setImageDrawable(card2);
            lastSuccessViews[2].setImageDrawable(card3);
            textView.setText("Score: "+ (++score));

            String updateInformation = messageFromServer.split(":")[1];
            String[] new_cards = updateInformation.split(",");
            cardsID.remove(card1.getCard());
            cardsID.remove(card2.getCard());
            cardsID.remove(card3.getCard());

            if(new_cards.length ==6){
                // only one situation: there were 12 cards on table before selection, after taking 3 cards, combined with three new popped cards, there is no set on the table
                // so, pop out three new cards. Thus, we receive 6 new cards from the server.
                int[] new_cards_int = new int[6];
                for(int i=0; i< new_cards_int.length; i++){
                    new_cards_int[i] = Integer.valueOf(new_cards[i]);
                }
                if(cardsID.size()!= 9) throw new RuntimeException("Unexpected errors: cards on table are not 9 after taking one set");
                // update imageviews
                img1.invalidate();
                img1.setImageDrawable(new CardDrawable(new_cards_int[0]));
                cardsID.put(new_cards_int[0],img1.getId());
                img2.invalidate();
                img2.setImageDrawable(new CardDrawable(new_cards_int[1]));
                cardsID.put(new_cards_int[1],img2.getId());
                img3.invalidate();
                img3.setImageDrawable(new CardDrawable(new_cards_int[2]));
                cardsID.put(new_cards_int[2],img3.getId());
                imageViews[12].invalidate();
                imageViews[12].setImageDrawable(new CardDrawable(new_cards_int[3]));
                cardsID.put(new_cards_int[3],imageViews[12].getId());
                imageViews[12].setOnClickListener(listener);
                imageViews[13].invalidate();
                imageViews[13].setImageDrawable(new CardDrawable(new_cards_int[4]));
                cardsID.put(new_cards_int[4],imageViews[13].getId());
                imageViews[13].setOnClickListener(listener);
                imageViews[14].invalidate();
                imageViews[14].setImageDrawable(new CardDrawable(new_cards_int[5]));
                cardsID.put(new_cards_int[5],imageViews[14].getId());
                imageViews[14].setOnClickListener(listener);
            }
            else if(new_cards.length ==3){
                // two cases:
                // 1: there were 12 cards, and replace three cards selected
                // 2: there were 15 cards, after taking three cards, there were still no set in the left 12 cards, so, taking three new cards.
                int[] new_cards_int = new int[3];
                for(int i=0; i< new_cards_int.length; i++){
                    new_cards_int[i] = Integer.valueOf(new_cards[i]);
                }
                if(cardsID.size()== 9|| cardsID.size()==12){
                    img1.invalidate();
                    img1.setImageDrawable(new CardDrawable(new_cards_int[0]));
                    cardsID.put(new_cards_int[0],img1.getId());
                    img2.invalidate();
                    img2.setImageDrawable(new CardDrawable(new_cards_int[1]));
                    cardsID.put(new_cards_int[1],img2.getId());
                    img3.invalidate();
                    img3.setImageDrawable(new CardDrawable(new_cards_int[2]));
                    cardsID.put(new_cards_int[2],img3.getId());
                }
                else{
                    throw new RuntimeException("Unexpected errors: cards on table are not 9 or 12 after taking one set");
                }
            }
            else if(new_cards.length ==1){ // the return value of " ".split(",").length is 1
                // no new cards popped out by server.
                if(cardsID.size()==12){
                    int i=0;
                    for(Integer key: cardsID.keySet()){
                        imageViews[i].invalidate();
                        imageViews[i].setImageDrawable(new CardDrawable(key));
                        cardsID.put(key, imageViews[i++].getId());
                    }
                    for(i =12; i< 15;i++){
                        imageViews[i].invalidate();
                        imageViews[i].setImageDrawable(null);
                        imageViews[i].setOnClickListener(null);
                    }
                }
                else{
                    img1.invalidate();
                    img1.setImageDrawable(null);
                    img1.setOnClickListener(null);
                    img2.invalidate();
                    img2.setImageDrawable(null);
                    img2.setOnClickListener(null);
                    img3.invalidate();
                    img3.setImageDrawable(null);
                    img3.setOnClickListener(null);
                }

            }
            else{
                throw new RuntimeException("Unexpected errors: new cards from server are not 6, 3, or 0");
            }

        }
    };

    private Runnable fail_update = new Runnable() {

        public void run() {

            for(int i=0; i< 3;i++){
                ImageView img = cards_toBeUpdate.poll();
                CardDrawable card= (CardDrawable) img.getDrawable();
                img.invalidate();
                card.setSelected(0);
                img.setImageDrawable(card);
            }
            textView.setText("Attempt failed");
        }
    };

    private Runnable otherSucceed_update = new Runnable() {

        public void run() {
            String updateInformation = messageFromServer.split(":")[1];
            String[] removed_cards = updateInformation.split(";")[0].split(",");
            String[] new_cards = updateInformation.split(";")[1].split(",");
            int[] removed_cards_ID = new int[3];
            // remove the selected cards from the hashmap cardsID(which stands for the cards on table)
            for(int i=0; i< removed_cards_ID.length;i++){
                removed_cards_ID[i] = cardsID.remove(Integer.valueOf(removed_cards[i]));
            }

            if(new_cards.length==6){
                // update 6 cards.
                for(int i=0; i< 3;i++){
                    ImageView img = findViewById(removed_cards_ID[i]);
                    img.invalidate();
                    img.setImageDrawable(new CardDrawable(Integer.valueOf(new_cards[i])));
                    cardsID.put(Integer.valueOf(new_cards[i]),removed_cards_ID[i]);
                }
                imageViews[12].invalidate();
                imageViews[12].setImageDrawable(new CardDrawable(Integer.valueOf(new_cards[3])));
                imageViews[12].setOnClickListener(listener);
                cardsID.put(Integer.valueOf(new_cards[3]),imageViews[12].getId());
                imageViews[13].invalidate();
                imageViews[13].setImageDrawable(new CardDrawable(Integer.valueOf(new_cards[4])));
                imageViews[13].setOnClickListener(listener);
                cardsID.put(Integer.valueOf(new_cards[4]),imageViews[13].getId());
                imageViews[14].invalidate();
                imageViews[14].setImageDrawable(new CardDrawable(Integer.valueOf(new_cards[5])));
                imageViews[14].setOnClickListener(listener);
                cardsID.put(Integer.valueOf(new_cards[5]),imageViews[14].getId());

            }
            else if(new_cards.length==3){
                // update 3 cards
                for(int i=0; i< 3;i++){
                    ImageView img = findViewById(removed_cards_ID[i]);
                    img.invalidate();
                    img.setImageDrawable(new CardDrawable(Integer.valueOf(new_cards[i])));
                    cardsID.put(Integer.valueOf(new_cards[i]),removed_cards_ID[i]);
                }
            }
            else if(new_cards.length==1){ // String.split() on a " "string gives a table with length = 1
                // the server does not give new cards.
                if(cardsID.size()==12){
                    int i=0;
                    for(Integer key: cardsID.keySet()){
                        imageViews[i].invalidate();
                        imageViews[i].setImageDrawable(new CardDrawable(key));
                        cardsID.put(key, imageViews[i++].getId());
                    }
                    for(i =12; i< 15;i++){
                        imageViews[i].invalidate();
                        imageViews[i].setImageDrawable(null);
                        imageViews[i].setOnClickListener(null);
                    }
                }
                else{
                    for(int i=0; i< 3;i++){
                        ImageView img = findViewById(removed_cards_ID[i]);
                        img.invalidate();
                        img.setImageDrawable(null);
                        img.setOnClickListener(null);
                    }
                }

            }
            else{
                throw new RuntimeException("The number of IdCards received from the server is not 0,3 or 6");
            }
            textView.setText("Others succeeded");
        }
    };

    private Thread check = new Thread(){
        public void run(){
            while(true){
                ImageView img1;
                ImageView img2;
                ImageView img3;
                lock.lock();
                try{
                    while(cards_selected.size()<3) lessThan3Cards.awaitUninterruptibly();
                    img1 = cards_selected.poll();
                    img2 = cards_selected.poll();
                    img3 = cards_selected.poll();
                }
                finally{
                    lock.unlock();
                }
                cards_toBeUpdate.addLast(img1);
                cards_toBeUpdate.addLast(img2);
                cards_toBeUpdate.addLast(img3);
                CardDrawable i = (CardDrawable) img1.getDrawable();
                CardDrawable j = (CardDrawable) img2.getDrawable();
                CardDrawable k = (CardDrawable) img3.getDrawable();
                int card1 = i.getCard();
                int card2 = j.getCard();
                int card3 = k.getCard();
                String message = "SELECT:";
                message += (card1+",");
                message += (card2+",");
                message += (card3);
                out.println(message);
            }
        }
    };

    private Thread fromServer = new Thread(){
        public void run(){
            String host = "10.0.2.2";
            int port = 9900;

            socket = Server.establishConnection(host,port);
            out = Server.connectionOut(socket);
            in = Server.connectionIn(socket);

            String message;
            // receive the first login message(i.e. 12 or 15 cards on table)
            try {
                message = in.readLine();
                String[] message_words = message.split(":");
                if(message_words[0].equals("LOGIN")){
                    String[] tmp = message_words[1].split(",");
                    for(int i =0; i< tmp.length; i++){
                        int card = Integer.valueOf(tmp[i]);
                        imageViews[i].setImageDrawable(new CardDrawable(card));
                        cardsID.put(card,imageViews[i].getId());
                        imageViews[i].setOnClickListener(listener);
                    }
                }
                else{
                    throw new RuntimeException("Received an expected message when login to server");
                }
            } catch (IOException e) {
                throw new RuntimeException("Unexpected errors when login to server");
            }
            // receive update information from the server and update according to different information
            while(true){
                try {
                    messageFromServer = in.readLine();
                    System.out.println(messageFromServer);
                    String[] receivedWords = messageFromServer.split(":");
                    if(receivedWords[0].equals("SUCCESS")){
                        // if the player's selection succeeded(i.e. the selection is a set).
                        ImageView img1 = cards_toBeUpdate.get(0);
                        ImageView img2 = cards_toBeUpdate.get(1);
                        ImageView img3 = cards_toBeUpdate.get(2);
                        CardDrawable i = (CardDrawable) img1.getDrawable();
                        CardDrawable j = (CardDrawable) img2.getDrawable();
                        CardDrawable k = (CardDrawable) img3.getDrawable();
                        int card1 = i.getCard();
                        int card2 = j.getCard();
                        int card3 = k.getCard();
                        img1.invalidate();
                        i.setSelected(2);
                        img2.invalidate();
                        j.setSelected(2);
                        img3.invalidate();
                        k.setSelected(2);
                        img1.setImageDrawable(i);
                        img2.setImageDrawable(j);
                        img3.setImageDrawable(k);
                        callback.postDelayed(success_update,1000);
                    }
                    else if(receivedWords[0].equals("FAIL")){
                        // if the player's selection failed(i.e. the selection is not a set)
                        ImageView img1 = cards_toBeUpdate.get(0);
                        ImageView img2 = cards_toBeUpdate.get(1);
                        ImageView img3 = cards_toBeUpdate.get(2);
                        CardDrawable i = (CardDrawable) img1.getDrawable();
                        CardDrawable j = (CardDrawable) img2.getDrawable();
                        CardDrawable k = (CardDrawable) img3.getDrawable();
                        int card1 = i.getCard();
                        int card2 = j.getCard();
                        int card3 = k.getCard();
                        img1.invalidate();
                        i.setSelected(3);
                        img2.invalidate();
                        j.setSelected(3);
                        img3.invalidate();
                        k.setSelected(3);
                        img1.setImageDrawable(i);
                        img2.setImageDrawable(j);
                        img3.setImageDrawable(k);
                        callback.postDelayed(fail_update,1000);
                    }
                    else if(receivedWords[0].equals("OTHERSUCCEEDED")){
                        // other players succeeded, so need to update the cards on table
                        while(!cards_selected.isEmpty()){
                            ImageView img = cards_selected.poll();
                            CardDrawable card = (CardDrawable) img.getDrawable();
                            img.invalidate();
                            card.setSelected(0);
                            img.setImageDrawable(card);
                        }
                        while(!cards_toBeUpdate.isEmpty()){
                            cards_toBeUpdate.poll();
                        }
                        callback.post(otherSucceed_update);
                    }
                    else if(receivedWords[0].equals("END")){
                        textView.setText(receivedWords[1]);
                        break;
                    }
                    else{
                        throw new RuntimeException("unexpected errors when receiving messages from the server");
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    };

    private View.OnClickListener listener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            ImageView img = findViewById(view.getId());
            CardDrawable card = (CardDrawable) img.getDrawable();
            img.invalidate();
            if(card.getSelected()==0){ // select
                card.setSelected(1);
                lock.lock();
                try{
                    if(!cards_selected.contains(img))
                        cards_selected.addLast(img);
                    if(cards_selected.size()>=3) lessThan3Cards.signalAll();
                }
                finally{
                    lock.unlock();
                }
                img.setImageDrawable(card);
            }
            else{ // unselect
                card.setSelected(0);
                lock.lock();
                try{
                    cards_selected.remove(img);
                }
                finally{
                    lock.unlock();
                }
                img.setImageDrawable(card);
            }


        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_two_player);
        Intent intent = getIntent();

        tableLayout = findViewById(R.id.TableLayout);
        tableRows  = new TableRow[4];
        tableRows[0] = findViewById(R.id.TableRow1);
        tableRows[1] = findViewById(R.id.TableRow2);
        tableRows[2] = findViewById(R.id.TableRow3);
        tableRows[3] = findViewById(R.id.TableRow4);
        imageViews = new ImageView[15];
        imageViews[0] = findViewById(R.id.imageView1);
        imageViews[1] = findViewById(R.id.imageView2);
        imageViews[2] = findViewById(R.id.imageView3);
        imageViews[3] = findViewById(R.id.imageView4);
        imageViews[4] = findViewById(R.id.imageView5);
        imageViews[5] = findViewById(R.id.imageView6);
        imageViews[6] = findViewById(R.id.imageView7);
        imageViews[7] = findViewById(R.id.imageView8);
        imageViews[8] = findViewById(R.id.imageView9);
        imageViews[9] = findViewById(R.id.imageView10);
        imageViews[10] = findViewById(R.id.imageView11);
        imageViews[11] = findViewById(R.id.imageView12);
        imageViews[12] = findViewById(R.id.imageView13);
        imageViews[13] = findViewById(R.id.imageView14);
        imageViews[14] = findViewById(R.id.imageView15);
        lastSuccessViews = new ImageView[3];
        lastSuccessViews[0] = findViewById(R.id.imageView16);
        lastSuccessViews[1] = findViewById(R.id.imageView17);
        lastSuccessViews[2] = findViewById(R.id.imageView18);
        chronometer = findViewById(R.id.chronometer);
        for(int i=0; i< lastSuccessViews.length; i++){
            lastSuccessViews[i].setImageDrawable(null);
        }
        textView = findViewById(R.id.textView);
        textView.setTextColor(0xff000000);
        textView.setText("Score: "+ score);
        chronometer.start();

        // start the thread
        fromServer.start();
        check.start();
    }

}
