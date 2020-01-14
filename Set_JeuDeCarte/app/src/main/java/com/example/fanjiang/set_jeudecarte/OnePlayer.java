package com.example.fanjiang.set_jeudecarte;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.content.Intent;
import android.media.Image;
import android.os.Handler;
import android.view.View;
import android.widget.Chronometer;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import java.util.Random;
import java.util.Stack;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class OnePlayer extends AppCompatActivity {

    TableLayout tableLayout;
    TableRow[] tableRows;
    ImageView[] imageViews;
    ImageView[] lastSuccessViews;
    TextView textView;
    final Handler callback = new Handler();
    Chronometer chronometer;

    Random rand = new Random();
    final int num_cards = 81;
    int score=0;
    Stack<Integer> cards = new Stack<>(); // card stack
    ArrayList<Integer> cardsOnTable = new ArrayList<>(); // cards on table
    LinkedList<ImageView> cards_selected = new LinkedList<ImageView>(); // cards selected by player
    LinkedList<ImageView> cards_toBeUpdate = new LinkedList<ImageView>(); // cards need to be update

    private final Lock lock = new ReentrantLock();
    private final Condition lessThan3Cards = lock.newCondition();

    // test if exist one set for a card list
    private boolean existSet(ArrayList<Integer> cardsOnTable){
        for(int i = 0; i < cardsOnTable.size()-2;i++){
            for(int j =i+1; j< cardsOnTable.size()-1;j++){
                for(int k = j+1; k< cardsOnTable.size();k++){
                    if(Cards.isSet(cardsOnTable.get(i),cardsOnTable.get(j), cardsOnTable.get(k))) return true;
                }
            }
        }
        return false;
    }
    // initialize card stack(shuffle)
    class InitCards extends Thread{
        @Override
        public void run(){
            ArrayList<Integer> cards_tmp = new ArrayList<>();
            int count = 0;
            for(int i =1; i < 4;i++){
                for(int j =1; j<4;j++){
                    for(int k=1; k<4;k++){
                        for(int l =1;l<4;l++){
                            cards_tmp.add( Cards.valueOf(i, j, k, l));
                        }
                    }
                }
            }
            int tmp;
            while(count < num_cards) {
                tmp = rand.nextInt(cards_tmp.size());
                cards.push(cards_tmp.get(tmp));
                count++;
                cards_tmp.remove(tmp);

            }

            for(int i = 0; i< 12; i++){
                cardsOnTable.add(cards.pop());
            }
            if(!existSet(cardsOnTable)){
                for(int i=0; i<3;i++){
                    cardsOnTable.add(cards.pop());
                }
            }
        }
    }

    private Runnable success_update = new Runnable() {

        public void run() {
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

            int newCard1=0,newCard2=0,newCard3 = 0;
            if(cardsOnTable.size()==15){ // if there were 15 cards on table
                cardsOnTable.remove((Integer) card1.getCard());
                cardsOnTable.remove((Integer) card2.getCard());
                cardsOnTable.remove((Integer) card3.getCard());
                if(!existSet(cardsOnTable)){
                    if(cards.isEmpty()){
                        img1.invalidate();
                        img1.setImageDrawable(null);
                        img1.setOnClickListener(null);
                        img2.invalidate();
                        img2.setImageDrawable(null);
                        img2.setOnClickListener(null);
                        img3.invalidate();
                        img3.setImageDrawable(null);
                        img3.setOnClickListener(null);
                        // end of the game
                        textView.setText("End of the game");
                    }
                    else{
                        newCard1 = cards.pop();
                        newCard2 = cards.pop();
                        newCard3 = cards.pop();
                        cardsOnTable.add(newCard1);
                        cardsOnTable.add(newCard2);
                        cardsOnTable.add(newCard3);
                        //~~~~~~~~~~ pop out new cards until there exist one set on the table
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
                        //~~~~~~~~~~~~
                        if(!existSet(cardsOnTable)) textView.setText("End of the game");
                        img1.invalidate();
                        img1.setImageDrawable(new CardDrawable(newCard1));
                        img2.invalidate();
                        img2.setImageDrawable(new CardDrawable(newCard2));
                        img3.invalidate();
                        img3.setImageDrawable(new CardDrawable(newCard3));
                    }
                }
                else{
                    // if the left 12 cards have one set, shuffle 12 cards
                    for(int i =0; i< cardsOnTable.size(); i++){
                        imageViews[i].invalidate();
                        imageViews[i].setImageDrawable(new CardDrawable(cardsOnTable.get(i)));
                        imageViews[i].setOnClickListener(listener);
                    }
                    for(int i =12; i< 15; i++){
                        imageViews[i].invalidate();
                        imageViews[i].setImageDrawable(null);
                        imageViews[i].setOnClickListener(null);
                    }
                }
            }
            else{
                cardsOnTable.remove((Integer) card1.getCard());
                cardsOnTable.remove((Integer) card2.getCard());
                cardsOnTable.remove((Integer) card3.getCard());
                if(cards.isEmpty()){
                    img1.invalidate();
                    img1.setImageDrawable(null);
                    img1.setOnClickListener(null);
                    img2.invalidate();
                    img2.setImageDrawable(null);
                    img2.setOnClickListener(null);
                    img3.invalidate();
                    img3.setImageDrawable(null);
                    img3.setOnClickListener(null);
                    if(!existSet(cardsOnTable)){
                        // end of the game
                        textView.setText("End of the game");

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
                            textView.setText("End of the game");

                        }
                        else{
                            int newCard4 = cards.pop();
                            int newCard5 = cards.pop();
                            int newCard6 = cards.pop();
                            cardsOnTable.add(newCard4);
                            cardsOnTable.add(newCard5);
                            cardsOnTable.add(newCard6);
                            ArrayList<Integer> tmp = new ArrayList<>();
                            // pop out new cards untill there exists at least one set
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
                            if(cards.isEmpty()) textView.setText("End of the game");
                            imageViews[12].invalidate();
                            imageViews[12].setImageDrawable(new CardDrawable(newCard4));
                            imageViews[12].setOnClickListener(listener);
                            imageViews[13].invalidate();
                            imageViews[13].setImageDrawable(new CardDrawable(newCard5));
                            imageViews[13].setOnClickListener(listener);
                            imageViews[14].invalidate();
                            imageViews[14].setImageDrawable(new CardDrawable(newCard6));
                            imageViews[14].setOnClickListener(listener);
                            img1.invalidate();
                            img1.setImageDrawable(new CardDrawable(newCard1));
                            img2.invalidate();
                            img2.setImageDrawable(new CardDrawable(newCard2));
                            img3.invalidate();
                            img3.setImageDrawable(new CardDrawable(newCard3));
                        }
                    }
                    else{
                        img1.invalidate();
                        img1.setImageDrawable(new CardDrawable(newCard1));
                        img2.invalidate();
                        img2.setImageDrawable(new CardDrawable(newCard2));
                        img3.invalidate();
                        img3.setImageDrawable(new CardDrawable(newCard3));
                    }
                }
            }
        }
    };

    private Runnable fail_update = new Runnable() {

        public synchronized void run() {
            for(int i=0; i< 3;i++){
                ImageView img = cards_toBeUpdate.poll();
                CardDrawable card= (CardDrawable) img.getDrawable();
                img.invalidate();
                card.setSelected(0);
                img.setImageDrawable(card);
            }
            textView.setText("Score: "+ (--score));
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
                    while(cards_selected.size()<3)
                        lessThan3Cards.awaitUninterruptibly();
                    img1 = cards_selected.poll();
                    img2 = cards_selected.poll();
                    img3 = cards_selected.poll();
                }
                finally {
                    lock.unlock();
                }
                cards_toBeUpdate.addLast(img1);
                cards_toBeUpdate.addLast(img2);
                cards_toBeUpdate.addLast(img3);
                CardDrawable i = (CardDrawable) img1.getDrawable();
                CardDrawable j = (CardDrawable) img2.getDrawable();
                CardDrawable k = (CardDrawable) img3.getDrawable();
                if(Cards.isSet(i.getCard(),j.getCard(),k.getCard())){
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
                else{
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

    private View.OnClickListener stop = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            chronometer.stop();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_one_player);

        Intent intent = getIntent();

        InitCards initCards = new InitCards();
        initCards.start();
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
        chronometer.setOnClickListener(stop);

        try {
            initCards.join();
        } catch (InterruptedException e) {
            throw new RuntimeException("Initialize cards failed");
        }

        for(int i =0; i< cardsOnTable.size(); i++){
            imageViews[i].setImageDrawable(new CardDrawable(cardsOnTable.get(i)));
            imageViews[i].setOnClickListener(listener);
        }

        check.start();
        chronometer.start();
    }

}
