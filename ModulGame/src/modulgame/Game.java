/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package modulgame;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.image.BufferStrategy;

/**
 *
 * @author Fauzan
 */
public class Game extends Canvas implements Runnable{
    Window window;
    public static final int WIDTH = 800;
    public static final int HEIGHT = 600;
    
    private int score = 0;
    
    private int time = 10;
    
    private Thread thread;
    private boolean running = false;
    
    private Handler handler;
    private Sound bgm = new Sound("/strings3.wav");
    private Sound eat = new Sound("/eat.wav");
    public enum STATE{
        Game,
        GameOver
    };
    dbConnection dbcon = new dbConnection();
    String nama;
    String pem;
    String level;
    int lama = 0;
    
    public STATE gameState = STATE.Game;
    
    public Game(String user, String pemain, String level){
        window = new Window(WIDTH, HEIGHT, "Modul praktikum 5", this);
        
        handler = new Handler();
        nama = user;
        pem = pemain;
        level = level;
        
        this.addKeyListener(new KeyInput(handler, this));
        
        if(gameState == STATE.Game){
            handler.addObject(new Items(100,150, ID.Item));
            handler.addObject(new Items(200,350, ID.Item));
            handler.addObject(new Player(200,200, ID.Player));
            handler.addObject(new musuh(300,300, ID.musuh));
            if(pemain == "dua"){
                handler.addObject(new Player(500,200, ID.Player2));   
            }
        }
        System.out.println("user" +user+" "+pemain+" "+level);
        if(level == "Easy"){
            time = 20;
        }else if(level == "Normal"){
            time = 10;
        }else{
            time = 5;
        }
        
    }

    public synchronized void start(){
        thread = new Thread(this);
        thread.start();
        running = true;
    }
    
    public synchronized void stop(){
        try{
            thread.join();
            running = false;
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        long lastTime = System.nanoTime();
        double amountOfTicks = 60.0;
        double ns = 1000000000 / amountOfTicks;
        double delta = 0;
        long timer = System.currentTimeMillis();
        int frames = 0;
        
        while(running){
            long now = System.nanoTime();
            delta += (now - lastTime) / ns;
            lastTime = now;
            
            while(delta >= 1){
                tick();
                delta--;
            }
            if(running){
                render();
                frames++;
            }
            
            if(System.currentTimeMillis() - timer > 1000){
                timer += 1000;
                //System.out.println("FPS: " + frames);
                frames = 0;
                if(gameState == STATE.Game){
                    if(time>0){
                        time--;
                        lama++;
                    }else{
                        gameState = STATE.GameOver;
                    }
                }
            }
        }
        stop();
    }
    
    private void tick(){
        handler.tick();
        if(gameState == STATE.Game){
            GameObject playerObject = null;
            GameObject playerObject2 = null;
            
            for(int i=0;i< handler.object.size(); i++){
                if(handler.object.get(i).getId() == ID.Player){
                   playerObject = handler.object.get(i);
                }
                if(pem == "dua"){
                    if(handler.object.get(i).getId() == ID.Player2){
                        playerObject2 = handler.object.get(i);
                    }
                }
            }
            if(playerObject != null){
                for(int i=0;i< handler.object.size(); i++){
                    if(handler.object.get(i).getId() == ID.Item){
                        if(checkCollision(playerObject, handler.object.get(i))){
                            eat.play();
                            handler.removeObject(handler.object.get(i));
                            score = (int)(score + (Math.random() * 10));
                            time = (int)(time + (Math.random() * 5));
                            break;
                        }
                        
                        if(pem == "dua"){
                            if(checkCollision(playerObject2, handler.object.get(i))){
                                eat.play();
                                handler.removeObject(handler.object.get(i));
                                score = (int)(score + (Math.random() * 10));
                                time = (int)(time + (Math.random() * 5));
                                break;
                            }
                        }
                    }
                
                    if(handler.object.get(i).getId() == ID.musuh){
                        if(checkCollision(playerObject, handler.object.get(i))){
                            handler.removeObject(handler.object.get(i));
                            gameState = STATE.GameOver;
                            break;
                        }
                        
                        if(pem == "dua"){
                            if(checkCollision(playerObject2, handler.object.get(i))){
                                handler.removeObject(handler.object.get(i));
                                gameState = STATE.GameOver;
                                break;
                            }
                        }
                    }
                    
                }
                
                if(pem == "dua"){
                    if(handler.object.size() == 3){
                        int x = 1,y = 1;
                        for(int i=0;i<(Math.random()*10);i++){
                            x = (int)(Math.random()*700);
                            y = (int)(Math.random()*500);
                            handler.addObject(new Items(x,y, ID.Item));
                        }
                    }
                }else{
                    if(handler.object.size() == 2){
                        int x = 1,y = 1;
                        for(int i=0;i<(Math.random()*10);i++){
                            x = (int)(Math.random()*700);
                            y = (int)(Math.random()*500);
                            handler.addObject(new Items(x,y, ID.Item));
                        }
                    }
                }
            }
        }
    }
    
    public static boolean checkCollision(GameObject player, GameObject item){
        boolean result = false;
        
        int sizePlayer = 50;
        int sizeItem = 20;
        
        int playerLeft = player.x;
        int playerRight = player.x + sizePlayer;
        int playerTop = player.y;
        int playerBottom = player.y + sizePlayer;
        
        int itemLeft = item.x;
        int itemRight = item.x + sizeItem;
        int itemTop = item.y;
        int itemBottom = item.y + sizeItem;
        
        if((playerRight > itemLeft ) &&
        (playerLeft < itemRight) &&
        (itemBottom > playerTop) &&
        (itemTop < playerBottom)
        ){
            result = true;
        }
        
        return result;
    }
    
    public void input(){
        try{
           
            String sql = "INSERT INTO highscore (Username, Score, waktu, score_akhir) values('"+nama+"','"+Integer.toString(score)+"','"+lama+"','"+(score + lama)+"')";
            java.sql.PreparedStatement pst = dbcon.con.prepareStatement(sql);
            pst.execute();
            
        }catch(Exception e){
            System.err.println("input gagal " +e.getMessage());
        }
        
    }
    
    private void render(){
        BufferStrategy bs = this.getBufferStrategy();
        if(bs == null){
            this.createBufferStrategy(3);
            return;
        }
        
        Graphics g = bs.getDrawGraphics();
        
        g.setColor(Color.decode("#F1f3f3"));
        g.fillRect(0, 0, WIDTH, HEIGHT);
        
        if(gameState ==  STATE.Game){
               bgm.loop();
//            soundBGM.loop();
            handler.render(g);
            Font currentFont = g.getFont();
            Font newFont = currentFont.deriveFont(currentFont.getSize() * 1.4F);
            g.setFont(newFont);

            g.setColor(Color.BLACK);
            g.drawString("Score: " +Integer.toString(score), 20, 20);

            g.setColor(Color.BLACK);
            g.drawString("Time: " +Integer.toString(time), WIDTH-120, 20);
        }else{
            Font currentFont = g.getFont();
            Font newFont = currentFont.deriveFont(currentFont.getSize() * 3F);
            g.setFont(newFont);

            g.setColor(Color.BLACK);
            g.drawString("GAME OVER", WIDTH/2 - 120, HEIGHT/2 - 30);

            currentFont = g.getFont();
            Font newScoreFont = currentFont.deriveFont(currentFont.getSize() * 0.5F);
            g.setFont(newScoreFont);

            g.setColor(Color.BLACK);
            g.drawString("Score: " +Integer.toString(score), WIDTH/2 - 50, HEIGHT/2 - 10);
            
            g.setColor(Color.BLACK);
            g.drawString("Press Space to Continue", WIDTH/2 - 100, HEIGHT/2 + 30);

            bgm.stop();
        }
                
        g.dispose();
        bs.show();
    }
    
    public static int clamp(int var, int min, int max){
        if(var >= max){
            return var = max;
        }else if(var <= min){
            return var = min;
        }else{
            return var;
        }
    }
    
    public void close(){
        window.CloseWindow();
    }
    
}
