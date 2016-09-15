/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package CF;

import java.awt.*;
import java.awt.geom.Line2D;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.File;
import java.io.FileReader;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Random;
import javax.imageio.ImageIO;
import javax.swing.*;
import java.sql.DriverManager;

/**
 *
 * @author Need
 */
public class CFS_CurveFeverSingleplayer extends JPanel {
     ////Konstanten
        //Objekte
        public final JFrame GUI;
        private final Random random;
        public CF_KeyboardListener CF_KL;

        //Arrays/Listen
        private final ArrayList<Shape> walls = new ArrayList<>();
        public final ArrayList<CF_Player> player;
        public final ArrayList<CF_Item> items = new ArrayList<>();
        public final Object[][] coords;
        private final Color[][] colorCoords;

        //Netzwerk
        public long lastNetworkDelay = 0;
        private final boolean netzwerkClient;
        private Statement stmt = null;
        private ResultSet rs = null;
        private Connection conn;

        //SpielOptionen
        public final int playerSpeed;
        private final int necWins;
        private final boolean pauseMoeglich;
        private final boolean itemsMoeglich;
        private final boolean gewinnfoto;
        private final String startPositionen;

        //Tweak Konstanten
        public int tweakGeschwindigkeit = 101; //Geschwindigkeit mit dem das Spiel läuft (zu wenig = Player können nicht reagieren; zu viel = Spiel ruckelt) (DEFAULT: 100)
        public int tweakGroeßeCollRadius = 2; //DEFAULT (3 = 6x6 Radius)
        public int tweakWahrscheinlichkeitItem = 400; //Je höher, desto unwahrscheinlicher (DEFAULT: 400)
        public int tweakGroeßePlayer = 5; //Je höher, desto größere StandardLinien (DEFAULT: 5)
        public int tweakWahrscheinlichkeitDontDrawShape = 70; //Je höher, desto unwahrscheinlicher, dass Shape des Spieler nicht gezeichnet wird ; niedrieger = mehr Lücken
        public int startStartingPause = 1500;
        public int startingRunningPause = 1500;
        public int runningStoppingPause = 3500;
        public int WinEXITPause = 5000;
        public int tweakItemFeldGroeße = 41;
        private int tweakFieldItemsSize = 8;
    
    //Variablen
        private long startTime;
        public ArrayList<CF_Item> fieldItems = new ArrayList<>();
        public String status;
        public long statusZeit;
        public boolean statusErsteMal = false;
        public long pause; //0<=keine Pause; 2=Pause im letztem Frame vorbei; 2>Pause (curTime)
        public final boolean abbruch;
        private int gewinnfotoNummer = 0;
        private long einSpielerRekord;
    
        //ColorChange Item
        private int r = 0;
        private int g = 0;
        private int b = 0;
        private boolean colorChangeRound2 = false;
        private static String RGBStatus = "END";
    
    public CFS_CurveFeverSingleplayer(JFrame GUI, ArrayList<CF_Player> player, Object playerSpeed, Object necWins, boolean pPausierenMoeglich, Object anzahlSpieler, Object gewinnfoto, boolean items, boolean netzwerk, String startPositionen) {
        this.GUI = GUI;
        this.player = player;
        this.status = "START";
        this.netzwerkClient = netzwerk;
        this.playerSpeed = Integer.parseInt(playerSpeed+"");
        this.necWins = Integer.parseInt(necWins+"");
        this.gewinnfoto = "Ja".equals(gewinnfoto.toString());
        this.startPositionen = startPositionen;
        pauseMoeglich = pPausierenMoeglich;
        itemsMoeglich = items;
        coords = new Object[GUI.getWidth()-380][GUI.getHeight()+20];
        colorCoords = new Color[GUI.getWidth()-380][GUI.getHeight()+20];
        
        this.pause = 0;
        abbruch = false;
        
        resetField(true);
        
        CF_KL = new CF_KeyboardListener(player, this, playerSpeed);

        if(!this.netzwerkClient && ladeDatenbank("host","database","username","password")) {
            ladeTweakWerte();
            setUpItems(true);
        } else if(!this.netzwerkClient) {
            setUpItems(false);
        }
        
        startTime = System.currentTimeMillis();
        random = new java.util.Random();
    }
    
    public void starte() {
        while(!abbruch) {   //Mehr oder weniger Endlosschleife
            GUI.repaint();
            long sleep = tweakGeschwindigkeit-playerSpeed;
            if(sleep<=0) sleep = 1;
            try { Thread.sleep(sleep); } catch (InterruptedException ex) {  }
        }
    }
    
    //<editor-fold defaultstate="collapsed" desc="PaintComponent">
    /*
     * Calc and draw next movement
     */
    @Override
    protected void paintComponent( Graphics graphics )
    {
        long curTime = System.currentTimeMillis();
        Graphics2D g2 = (Graphics2D) graphics;
        g2.setRenderingHint( RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        
        //<editor-fold defaultstate="collapsed" desc="Zeichne Hintergrund ODER ColorChange Item">
        //Zeichne normalen(schwarzen) Hintergrund
        if("END".equals(RGBStatus)) { 
            graphics.setColor(Color.black); 
            graphics.fillRect(0, 0, getWidth(), getHeight()); 
        } else {        //ColorChange Effect
            iterateColor(graphics);
        }
        
        g2.setColor(Color.orange);
        g2.drawLine(GUI.getWidth()-400, 0, GUI.getWidth()-400, GUI.getHeight());
        //</editor-fold>
        
        boolean showLines = false;
        if(showLines) {
            g2.setColor(Color.GRAY);
            g2.setStroke(new BasicStroke(6));
            double width = GUI.getWidth()-400;
            double height = GUI.getHeight();
            double widthPerField = width/(GUI.getWidth()-380.00d);
            double heightPerField = height/(GUI.getHeight()+20.00d);
            System.out.println(width+"/"+GUI.getWidth()+"/"+widthPerField);
            System.out.println(height+"/"+GUI.getHeight()+"/"+heightPerField);
            System.out.println("");
            for (int y = 0; y < coords.length; y++) {
                g2.draw(new Line2D.Double(0, y*heightPerField, width, y*heightPerField));
                g2.draw(new Line2D.Double(y*widthPerField, 0, y*widthPerField, height));
            }
        }
        
        g2.setStroke(new BasicStroke(tweakGroeßePlayer));
        //<editor-fold defaultstate="collapsed" desc="PAUSEZUSTAND: Kein Pause / Spiel "läuft"">
        if(pause<=0) {
            //<editor-fold defaultstate="collapsed" desc="SPIELZUSTAND: Spiel wird gestartet">
            if("START".equals(status)) {    //Start Zustand, Programm startet zum ersten Mal
                resetField(true);
                if(statusErsteMal) { 
                    statusZeit = curTime; 
                    statusErsteMal = false; 
                }
                if(player.size()==2 && player.get(1).KI) {  //Falls Einzelspieler gegen KI
                    g2.setColor(player.get(0).color);
                    g2.setFont(new Font("Arial",30,30));
                    g2.drawString(player.get(0).name+": "+(einSpielerRekord/1000)+" Sekunden", GUI.getWidth()-390, 25); 
                } else { //Nicht gegen KI
                    ArrayList<CF_Player> sortiert = bubbleSort();
                    for (int playerIndex = 0; playerIndex < sortiert.size(); playerIndex++) { //playerIndex = jeder einzelnen Player
                        g2.setColor(sortiert.get(playerIndex).color);
                        g2.setFont(new Font("Arial",30,30));
                        g2.drawString(sortiert.get(playerIndex).name+": "+sortiert.get(playerIndex).points, GUI.getWidth()-390, 25+(playerIndex*30));
                        sortiert.get(playerIndex).shapes.add(new Line2D.Double(sortiert.get(playerIndex).x, sortiert.get(playerIndex).y, sortiert.get(playerIndex).x, sortiert.get(playerIndex).y));
                        sortiert.get(playerIndex).shapeSizes.add(g2.getStroke());
                        g2.draw(sortiert.get(playerIndex).shapes.get(0));
                    }
                }
                if(!netzwerkClient) if(System.currentTimeMillis()-statusZeit>startStartingPause) { //DEFAULT: Pause = 1,5 Sek.
                    status = "STARTING";
                    statusErsteMal = true;
                }
                //</editor-fold>
                
            //<editor-fold defaultstate="collapsed" desc="SPIELZUSTAND: Spiel wird gerade restartet">
            } else if("STARTING".equals(status)) {      //Starting Zustand, immer wenn neu gestartet wird
                resetField(true);
                if(netzwerkClient && statusErsteMal) {
                    resetPlayers();
                    resetField(true);
                } 
                if(statusErsteMal) { 
                    statusZeit = curTime; 
                    statusErsteMal = false; 
                }
                
                if(player.size()==2 && player.get(1).KI) {  //Falls Einzelspieler gegen KI
                    g2.setColor(player.get(0).color);
                    g2.setFont(new Font("Arial",30,30));
                    g2.drawString(player.get(0).name+": "+(einSpielerRekord/1000)+" Sekunden", GUI.getWidth()-390, 25); 
                } else { //Nicht gegen KI
                    ArrayList<CF_Player> sortiert = bubbleSort();
                    for (int playerIndex = 0; playerIndex < sortiert.size(); playerIndex++) { //playerIndex = jeder einzelnen Player
                        g2.setColor(sortiert.get(playerIndex).color);
                        g2.setFont(new Font("Arial",30,30));
                        g2.drawString(sortiert.get(playerIndex).name+": "+sortiert.get(playerIndex).points, GUI.getWidth()-390, 25+(playerIndex*30));
                        sortiert.get(playerIndex).shapes.add(new Line2D.Double(sortiert.get(playerIndex).x, sortiert.get(playerIndex).y, sortiert.get(playerIndex).x, sortiert.get(playerIndex).y));
                        sortiert.get(playerIndex).shapeSizes.add(g2.getStroke());
                        g2.draw(sortiert.get(playerIndex).shapes.get(0));
                    }
                }
                if(!netzwerkClient) if(System.currentTimeMillis()-statusZeit>startingRunningPause) { //DEFAULT: Pause = 1,5 Sek.
                    status = "RUNNING";
                    statusErsteMal = true;
                }
                //</editor-fold>
                
            //<editor-fold defaultstate="collapsed" desc="SPIELZUSTAND: Spiel läuft">
            } else if("RUNNING".equals(status)) {       //Running Zustand, CurveFeverProgramm läuft gerade
                double newXDouble; //Double der neuen X-Position des Spielers
                double newYDouble; //Double der neuen Y-Position des Spieler
                int newXInt; //Int der neuen X-Position des Spielers
                int newYInt; //Int der neuen Y-Position des Spielers
                int alivePlayers = 0; //Alle noch lebendigen Spieler in diesem Durchlauf
                
                //<editor-fold defaultstate="collapsed" desc="Zeichne vorhandene FeldItems">
                //Zeichnet alle auf dem Feld vorhandenen Items
                for (int fieldItem = 0; fieldItem < fieldItems.size(); fieldItem++) {
                    CF_Item curItem = fieldItems.get(fieldItem);
                    BufferedImage buffImage = null;
                    g2.setColor(Color.blue);
                    g2.setFont(new Font("Arial",15,15));
                        if(new File(System.getProperty("user.dir")+"\\src").exists()) { //Mit NetBeans gestartet
                            try {
                                File curFile = new File(curItem.itemFile);
                                int lastIndexSlash = curFile.getAbsolutePath().lastIndexOf("\\");
                                String newFilePath = curFile.getAbsolutePath().substring(0,lastIndexSlash)+"\\src\\Spiel\\Ressourcen\\"+curFile.getAbsolutePath().substring(lastIndexSlash);
                                buffImage = ImageIO.read(new File(newFilePath));
                            } catch (IOException ex) { System.err.println("CFS_SinglePlayer: paintComponent: RUNNING: fieldItemListe: "+ex); }
                        } else {    //JAR gestartet
                            Image itemImage;
                            Image itemsImage = Toolkit.getDefaultToolkit().getImage(getClass().getResource("/Spiel/Ressourcen/"+curItem.itemFile));
                            buffImage = new BufferedImage(itemsImage.getWidth(null),itemsImage.getHeight(null),BufferedImage.TYPE_INT_ARGB);
                            itemImage = itemsImage;
                            Graphics2D bGr = buffImage.createGraphics();
                            bGr.drawImage(itemImage, 0, 0, null);
                            bGr.dispose();
                        }
                        g2.drawImage(buffImage, curItem.x,curItem.y,null);
                }
                //</editor-fold>
                
                //<editor-fold defaultstate="collapsed" desc="Field Item wird erstellt (Wahrscheinlichkeit)">
                //Erstellt ein neues Item auf dem Bildschirm; Wahrscheinlichkeit (1/wahrscheinlichkeitItem)*100 % (DEFAULT: 1/750*100 = 0,133 %)
                if(itemsMoeglich && random.nextInt(tweakWahrscheinlichkeitItem)==0 && !netzwerkClient && fieldItems.size()<tweakFieldItemsSize) {
                    if(items.size()>0) {
                        int x = random.nextInt(GUI.getWidth() - 451);
                        int y = random.nextInt(GUI.getHeight() - 51);
                        CF_Item tempItem = items.get(random.nextInt(items.size())); //Suche ein Item aus vorhandenen Items
                        CF_Item item = new CF_Item(tempItem.name, tempItem.effekt, tempItem.dauer/1000, tempItem.effectedPlayer, x, y, tempItem.itemImage, tempItem.itemFile); //Erstelle neues Item
                        fieldItems.add(item);
                        for (int xCoord = 0; xCoord < tweakItemFeldGroeße; xCoord++) {
                            for (int yCoord = 0; yCoord < tweakItemFeldGroeße; yCoord++) {
                                coords[x+xCoord][y+yCoord] = item;
                            }
                        }
                    }
                }
                //</editor-fold>
                
                //Bearbeitet für jeden Spieler
                for (int playerIndex = 0; playerIndex < player.size(); playerIndex++) { //playerIndex = jeder einzelnen Player (Für jeden Player)
                    //Bearbeite evtl. Effekte
                    g2.setStroke(new BasicStroke(tweakGroeßePlayer));
                    int repeatTimes = 3;
                    
                    //<editor-fold defaultstate="collapsed" desc="Leicht programmierte Item Effekte">
                    if(player.get(playerIndex).effekt==null) {
                    } else if("SpeedUp".equals(player.get(playerIndex).effekt.name)) {
                        repeatTimes += 2;
                    } else if("SlowDown".equals(player.get(playerIndex).effekt.name) || player.get(playerIndex).KI) {
                        repeatTimes -= 2;
                    } else if("Bigger".equals(player.get(playerIndex).effekt.name)) {
                        g2.setStroke(new BasicStroke(tweakGroeßePlayer+4));
                    } else if("Smaller".equals(player.get(playerIndex).effekt.name)) {
                        g2.setStroke(new BasicStroke(tweakGroeßePlayer-4));
                    } else if("Reset".equals(player.get(playerIndex).effekt.name)) {
                        resetField(false);
                        for (int players = 0; players < player.size(); players++) {
                            player.get(players).shapes = new ArrayList<>();
                            player.get(players).shapeSizes = new ArrayList<>();
                        }
                    }
                    //</editor-fold>
                    
                    //<editor-fold defaultstate="collapsed" desc="Lücken">
                        //Wenn Wahrscheinlichkeit zutrifft, Spieler nicht gerade schon Lücke zeichnet und Spieler Server oder Lokal ist
                    //ODER
                        //Wenn Spieler Client ist und dontDraw zutrifft
                    if((random.nextInt(tweakWahrscheinlichkeitDontDrawShape)==1 && player.get(playerIndex).dontDraw==0 && !netzwerkClient) || (netzwerkClient && player.get(playerIndex).dontDraw!=0)) {
                        player.get(playerIndex).dontDraw = curTime;
                    } else if(player.get(playerIndex).dontDraw + (1.5*(random.nextInt(400)+200)/repeatTimes) < curTime) {
                        player.get(playerIndex).dontDraw = 0;
                    }
                    //</editor-fold>
                    
                    //<editor-fold defaultstate="collapsed" desc="KI-Berechnung">
                    if(player.get(playerIndex).KI) {
                        repeatTimes=2;
                        CF_Player spieler = player.get(0);
                        CF_Player kiSpieler = player.get(1);
                        
                        double entfX = spieler.x - kiSpieler.x;
                        double entfY = spieler.y - kiSpieler.y;
                        
                        double entfernung = Math.sqrt(Math.pow(Math.abs(entfX),2) + Math.pow(entfY,2)); //Pythagoras
                        double wunschX = entfX/entfernung;
                        double wunschY = entfY/entfernung;
                        double wunschAngleX = Math.toDegrees(Math.acos(wunschX));
                        double wunschAngleY = Math.toDegrees(Math.asin(wunschY));
                        
                        if(wunschAngleY<0) {   //Problem bei wunschAngle übergang 360/0 Grad
                            wunschAngleX = 360-wunschAngleX;
                        }
                        
                        if(kiSpieler.angle>wunschAngleX && (kiSpieler.angle+180<wunschAngleX || kiSpieler.angle-180 < wunschAngleX)) {
                            kiSpieler.angle-=2;
                        } else {
                            kiSpieler.angle+=2;
                        }
                        
//                        System.out.println(wunschAngleX);
//                        System.out.println(wunschAngleY);
//                        System.out.println(kiSpieler.angle);
//                        System.out.println();
                        
                        if(kiSpieler.getAngle()>=360) {
                            kiSpieler.setAngle(kiSpieler.getAngle()-360);
                        } else if(kiSpieler.getAngle()<0) {
                            kiSpieler.setAngle(kiSpieler.getAngle()+360);
                        }
                    }
                    //</editor-fold>
                    
                    //<editor-fold defaultstate="collapsed" desc="Zeichne vorhandene Punkte + Blackscreen Effect + WALLS">
                    //Zeichne vorhandenen Daten (Punktestand + vorhandene Playerlinien (Shapes))
                    g2.setColor(player.get(playerIndex).color);
                    g2.setFont(new Font("Arial",30,30));
                    boolean blackScreen = false;
                    for (int i = 0; i < player.size(); i++) {
                        if("BlackScreen".equals(player.get(i).getEffektName())) { blackScreen = true; break;}
                    }
                    if(!blackScreen) {
                        Stroke backupStroke = g2.getStroke();
                        for (int shapeIndex = 0; shapeIndex < player.get(playerIndex).shapes.size(); shapeIndex++) {   //jeder einzelne Shape (shapeIndex) jedes Spielers (playerIndex) ...
                            g2.setStroke(player.get(playerIndex).shapeSizes.get(shapeIndex));
                            g2.draw(player.get(playerIndex).shapes.get(shapeIndex)); //... zeichnen
                        }
                        g2.setStroke(backupStroke);
                    }
                    
                    Stroke backupStroke = g2.getStroke();
                    g2.setStroke(new BasicStroke(2));
                    g2.setColor(Color.orange);
                    for (int i = 0; i < walls.size(); i++) {
                        g2.draw(walls.get(i));
                    }
                    g2.setStroke(backupStroke);
                    //</editor-fold>
                    
                    if(player.get(playerIndex).alive) {     //Programm darf nur einmal alivePlayers erhöhen pro Spieler...
                        alivePlayers++; //Spieler lebt noch VOR aktuellem Zug
                    }
                    
                    //<editor-fold defaultstate="collapsed" desc="Berechnungs- und Zeichnungsschleife">
                    //ForSchleife wird benutzt für Effekte: Schneller(5)/Langsamer(1)/Normal(3) (in Klammern die Durchläufe bei Effekt)
                    for (int repeat = 0; repeat < repeatTimes; repeat++) {  
                        if(player.get(playerIndex).alive) { //...führe Berechnung nur aus, wenn Spieler noch lebt
                            
                            //<editor-fold defaultstate="collapsed" desc="Berechne nächste Bewegung (Einheitskreis)">
                            newXDouble = player.get(playerIndex).x +  Math.cos(Math.toRadians(player.get(playerIndex).angle));
                            newYDouble = player.get(playerIndex).y +  Math.sin(Math.toRadians(player.get(playerIndex).angle));
                            newXInt = (int)Math.round(newXDouble);
                            newYInt = (int)Math.round(newYDouble);
                            //</editor-fold>

                            boolean coll = false;  
                            //<editor-fold defaultstate="collapsed" desc="Überprüfe Kollision mit Fensterrand">
                            if(newXInt<=tweakGroeßeCollRadius || newXInt>=(GUI.getWidth()-400)-tweakGroeßeCollRadius || newYInt<=tweakGroeßeCollRadius || newYInt>=GUI.getHeight()-tweakGroeßeCollRadius) { //Player berührt einen Fensterrand
                                coll = true;      
                            //</editor-fold>
                            } else {         
                                boolean itemSchonGesammelt = false;
                                breakPoint:
                                for(int xCoordCollCheck=-tweakGroeßeCollRadius; xCoordCollCheck<tweakGroeßeCollRadius; xCoordCollCheck++) {          //Überprüfe für gesamten Collisionsradius des Spielers (DEFAULT=6x6 Pixel)...
                                    for (int yCoordCollCheck = -tweakGroeßeCollRadius; yCoordCollCheck < tweakGroeßeCollRadius; yCoordCollCheck++) {   //...ob....
                                        //<editor-fold defaultstate="collapsed" desc="Spieler sammelt Item auf">
                                        if(coords[newXInt+xCoordCollCheck][newYInt+yCoordCollCheck] instanceof CF_Item) {      //Spieler auf ein Item trifft...
                                            if(!itemSchonGesammelt) {
                                                //Entferne eingesammeltes item sicher aus dem Spielfeld
                                                itemSchonGesammelt = true;
                                                CF_Item curItem = (CF_Item)coords[newXInt+xCoordCollCheck][newYInt+yCoordCollCheck];
                                                int itemX = curItem.x;
                                                int itemY = curItem.y;
                                                System.out.println(player.get(playerIndex).name+" hat "+curItem.name+" für "+curItem.effectedPlayer+" eingesammelt.");
                                                for (int itemXColl = 0; itemXColl < tweakItemFeldGroeße; itemXColl++) {
                                                    for (int itemYColl = 0; itemYColl < tweakItemFeldGroeße; itemYColl++) {
                                                        coords[itemX+itemXColl][itemY+itemYColl] = (long)0; 
                                                        colorCoords[itemX+itemXColl][itemY+itemYColl] = Color.black;
                                                    }
                                                }


                                                //Füge Item Effect zu entsprechenden Spieler hinzu
                                                if("ColorChange".equals(curItem.name)) { 
                                                    if("END".equals(RGBStatus)) RGBStatus = "B1"; 
                                                } else if("newWall".equals(curItem.name)) {
                                                    int x1 = random.nextInt(GUI.getWidth()-400-20)+10;
                                                    int x2 = random.nextInt(GUI.getWidth()-400-20)+10;
                                                    int y1 = random.nextInt(GUI.getHeight()-20)+10;
                                                    int y2 = random.nextInt(GUI.getHeight()-20)+10;
                                                    int entfernung = (int)Math.round(Math.sqrt(Math.pow(Math.abs(x2-x1), 2)+Math.pow(Math.abs(y2-y1), 2)));
                                                    double xSchritte = Math.abs(x1-x2)/(double)entfernung;
                                                    double ySchritte = Math.abs(y1-y2)/(double)entfernung;
                                                    int newX1 = x1;
                                                    int newY1 = y1;
                                                    for (int i = 0; i < entfernung; i++) {
                                                        int newX = (int)Math.round(newX1+i*xSchritte);
                                                        if(x1>x2)
                                                            newX = (int)Math.round(newX1-i*xSchritte);
                                                        int newY = (int)Math.round(newY1+i*ySchritte);
                                                        if(y1>y2)
                                                            newY = (int)Math.round(newY1-i*ySchritte);
                                                        for(int x=0-(tweakGroeßeCollRadius/2);x<(tweakGroeßeCollRadius/2);x++) {
                                                            for (int y = 0-(tweakGroeßeCollRadius/2); y < (tweakGroeßeCollRadius/2); y++) {
                                                                if(newX+x>0 && newY+y>0 && newX+x<coords.length-1 && newY+y<coords.length-1) {
                                                                    coords[newX+x][newY+y] = curTime;
                                                                    colorCoords[newX+x][newY+y] = Color.orange;
                                                                }
                                                            }
                                                        }
                                                    }
                                                    walls.add(new Line2D.Double(x1,y1,x2,y2));
                                                } else {
                                                    if("sich selber".equals(curItem.effectedPlayer) || "alle".equals(curItem.effectedPlayer)) { //Alle oder nur sich selber
                                                        player.get(playerIndex).changeEffekt(curItem);
                                                    } else { //Alle anderen
                                                        for (int players = 0; players < player.size(); players++) {
                                                            CF_Player curPlayer = player.get(players);
                                                            if(curPlayer.alive && players!=playerIndex) { //Player am Leben UND aktueller Player ist nicht Player der Item eingesamelt hat
                                                                curPlayer.changeEffekt(curItem);
                                                            }
                                                        }
                                                    }
                                                }
                                                if(fieldItems.indexOf(curItem)!=-1) { fieldItems.remove(fieldItems.indexOf(curItem)); } //Entferne FieldItem
                                            }
                                        //</editor-fold>
                                        //<editor-fold defaultstate="collapsed" desc="Kollision mit anderem Spieler oder Fensterrand">
                                        } else {  //...oder einen anderen Spieler beruehrt
                                            //if("END".equals(RGBStatus)) {  //Nur CollisionsCheck, falls ColorChange Effekt nicht aktiviert
                                                if((long)coords[newXInt+xCoordCollCheck][newYInt+yCoordCollCheck]!=0 && colorCoords[newXInt+xCoordCollCheck][newYInt+yCoordCollCheck]!=new Color(r,g,b,255)) { //Feld ueberhaupt mit einem Spieler belegt?
                                                    if(colorCoords[newXInt+xCoordCollCheck][newYInt+yCoordCollCheck].getRGB()==player.get(playerIndex).color.getRGB()) { //Feld mit eigener Farbe belegt
                                                        if(curTime-(long)coords[newXInt+xCoordCollCheck][newYInt+yCoordCollCheck]>1000 && !player.get(playerIndex).KI) {    //minstestens 1000 Millisekunden her?
                                                            coll = true;
                                                            System.err.println("COLLISION "+player.get(playerIndex).name+"("+player.get(playerIndex).playerNummer+")"+"@: "+(newXInt+xCoordCollCheck)+"/"+(newYInt+yCoordCollCheck)+"  "+coords[newXInt+xCoordCollCheck][newYInt+yCoordCollCheck]+"_"+colorCoords[newXInt+xCoordCollCheck][newYInt+yCoordCollCheck]);
                                                            break breakPoint;   //Breake aus beiden For Schleifen
                                                        }
                                                    } else if(!player.get(playerIndex).KI) {    //Feld mit anderem Spieler belegt
                                                        coll = true;
                                                        System.err.println("COLLISION "+player.get(playerIndex).name+"("+player.get(playerIndex).playerNummer+")"+"@: "+(newXInt+xCoordCollCheck)+"/"+(newYInt+yCoordCollCheck)+"  "+coords[newXInt+xCoordCollCheck][newYInt+yCoordCollCheck]+"_"+colorCoords[newXInt+xCoordCollCheck][newYInt+yCoordCollCheck]);
                                                        break breakPoint;   //Breake aus beiden For Schleifen
                                                    }
                                                }
                                            //}
                                        }
                                        //</editor-fold>
                                    }
                                }
                            }

                            //<editor-fold defaultstate="collapsed" desc="Kollisionsergebnis: JA = Entferne Spieler; NEIN: Setze Kollisionsfeld">
                            if(coll) {  //Falls Collision in diesem Zug, entferne Spieler aus Spiel
                                if(!"Need".equals(player.get(playerIndex).name)) {
                                    if(!player.get(playerIndex).KI) {
                                        alivePlayers--;
                                        player.get(playerIndex).alive = false;
                                        for (int i = 0; i < player.size(); i++) {
                                            if(player.get(i).alive) {
                                                player.get(i).points++;
                                            }
                                        }
                                    }
                                    //Suche jemand, der gewonnen haben könnte
                                    for (int playerIndex2 = 0; playerIndex2 < player.size(); playerIndex2++) {
                                        if(player.get(playerIndex2).points>=necWins) {
                                            status = "WIN";
                                            for (int j = 0; j < player.size(); j++) {
                                                player.get(j).alive = j==playerIndex2;
                                            }
                                            return;
                                        }
                                    }
                                }
                            } else {    //Ansonsten belege Collisionsfelder des Spielers im Spielfeld (die Felder auf dem der neue Punkt ist)
                                for(int x=0-tweakGroeßeCollRadius;x<tweakGroeßeCollRadius;x++) {
                                    for (int y = 0-tweakGroeßeCollRadius; y < tweakGroeßeCollRadius; y++) {
                                        if(player.get(playerIndex).dontDraw==0) {
                                            coords[newXInt+x][newYInt+y] = curTime;
                                            colorCoords[newXInt+x][newYInt+y] = player.get(playerIndex).color;
                                        }
                                    }
                                }
                            }
                            //</editor-fold>
                            
                            //<editor-fold defaultstate="collapsed" desc="Update Spielerkoordinaten & zeichne neuen Punkt">
                            player.get(playerIndex).x = newXDouble; //Update X-Koordinaten und...
                            player.get(playerIndex).y = newYDouble; //Y-Koordinaten des Spielers
                            Shape newLine = new Line2D.Double(newXDouble,newYDouble,newXDouble,newYDouble);
                            if(player.get(playerIndex).dontDraw==0 ) { player.get(playerIndex).shapeSizes.add(g2.getStroke()); player.get(playerIndex).shapes.add(newLine); } //Füge naechste Bewegung hinzu...
                            g2.setColor(player.get(playerIndex).color);
                            g2.draw(newLine); //...und zeige sie
                            //</editor-fold>
                        }
                    }
                    //</editor-fold>
                }
                
                //<editor-fold defaultstate="collapsed" desc="Zustandübergangsabfrage">
                //Ueberpruefe, ob jemand in dieser Runde gewonnen hat
                if(alivePlayers==1 && (player.size()!=1 && !player.get(1).KI)) { //Wenn ein Spieler gewonnen hat...
                    for (int i = 0; i < player.size(); i++) {   //Suche den Spieler, der gewonnen hat
                        if(player.get(i).alive) { //Erhoehe die Punkte von jedem Spieler, ...
//                            player.get(i).points++; //... der noch lebt
                            if(player.get(i).points >=necWins) { //Wenn Spieler genug Punkte hat, um vollständig zu gewinnen...
                                status = "WIN";         //...lasse ihn gewinnen...
                            } else {                    //...ansonsten..
                                status = "STOPPING";    //...restarte
                            }
                        }
                    }
                    
                //Wenn kein Spieler mehr lebt (1 Spieler Modus)
                } else if(alivePlayers==1 && player.size()==2 && player.get(1).KI) {                  
                    status = "STOPPING";    //...restarte 
                    long rekord = System.currentTimeMillis()-startTime; //Setze neuen Rekord
                    if(einSpielerRekord<rekord) { einSpielerRekord = rekord; }
                    startTime = System.currentTimeMillis(); //Resette Starttime
                } else if(alivePlayers==0) { //Alle gleichzeitig tot
                    status= "STOPPING";
                }
                    
                //<editor-fold defaultstate="collapsed" desc="Zeichne Punktestand">
                    if(player.size()==2 && player.get(1).KI) {  //Spiel gegen KI
                        g2.setColor(player.get(0).color);
                        g2.setFont(new Font("Arial",30,30));
                        g2.drawString(player.get(0).name+": "+(einSpielerRekord/1000)+" Sekunden", GUI.getWidth()-390, 25); 
                    } else { //Spiel gegen Spieler
                        ArrayList<CF_Player> sortiert = bubbleSort();
                        
                        for (int playerIndex = 0; playerIndex < sortiert.size(); playerIndex++) {
                            g2.setColor(sortiert.get(playerIndex).color);
                            String spielerName = sortiert.get(playerIndex).name;
                            if(!sortiert.get(playerIndex).alive)  spielerName += " (CRASH)";
                            g2.drawString(spielerName+": "+sortiert.get(playerIndex).points, GUI.getWidth()-390, 25+(playerIndex*30));
                        }
                    }
                    //</editor-fold>
                //</editor-fold>
            //</editor-fold>
                
            //<editor-fold defaultstate="collapsed" desc="SPIELZUSTAND: Jemand hat eine Runde gewonnen; Restarte Spiel">
            //Jemand hat diese Runde gewonnen = Restarte nach 3,5 Sekunden
            } else if( "STOPPING".equals(status)) { //Stopping Zustand, jemand hat gewonnen, restarte nach 3,5 sec
                //Erstelle Gewinnfoto (falls eingestellt)
                if(statusErsteMal) {
                    statusZeit = curTime; 
                    statusErsteMal = false; 
                }
                if(gewinnfoto) {
                    try {
                        gewinnfotoNummer++;
                        ImageIO.write(new Robot().createScreenCapture(new Rectangle(GUI.getX(),GUI.getY(),GUI.getWidth(),GUI.getHeight())), "PNG", new java.io.File("foto"+gewinnfotoNummer+".png"));
                    } catch ( AWTException | IOException ex) { System.err.println("CFS_SinglePlayer: paintComp: STOPPING: "+ex); }
                }
                
                //Warte startingStoppingPause (DEFAULT: 3,5 Sekunden) und restarte Spiel
                if(player.size()==2 && player.get(1).KI) { //Spiel gegen KI
                    g2.setColor(player.get(0).color);
                    g2.setFont(new Font("Arial",30,30));
                    g2.drawString(player.get(0).name+": "+(einSpielerRekord/1000)+" Sekunden", GUI.getWidth()-390, 25);
                    g2.setStroke(player.get(0).shapeSizes.get(0));
                    g2.draw(player.get(0).shapes.get(0));  
                } else { //Spiel gegen andere Spieler
                    ArrayList<CF_Player> sortiert = bubbleSort();
                    
                    for (int playerIndex = 0; playerIndex < sortiert.size(); playerIndex++) { //i = jeder einzelnen Player 
                        g2.setColor(sortiert.get(playerIndex).color);
                        g2.setFont(new Font("Arial",30,30));
                        String name = sortiert.get(playerIndex).name;
                        if(!sortiert.get(playerIndex).alive) name += " (Crash)";
                        g2.drawString(name+": "+sortiert.get(playerIndex).points, GUI.getWidth()-390, 25+(playerIndex*30));
                        for (int shapeIndex = 0; shapeIndex < sortiert.get(playerIndex).shapes.size(); shapeIndex++) {
                            g2.setStroke(sortiert.get(playerIndex).shapeSizes.get(shapeIndex));
                            g2.draw(sortiert.get(playerIndex).shapes.get(shapeIndex));
                        }
                    }
                }
                if(System.currentTimeMillis()-statusZeit>runningStoppingPause) { //DEFAULT: Pause = 1,5 Sek.
                    status = "START";
                    statusErsteMal = true;
                    resetPlayers();
                    resetField(true);
                }
            //</editor-fold>
                
            //<editor-fold defaultstate="collapsed" desc="SPIELZUSTAND: Ein Spieler hat vollständig gewonnen; kein Restart">
            //Ein Spieler hat vollständig gewonnen = kein Restart
            } else if("WIN".equals(status)) {   //Win Zustand, jemand hat GANZ gewonnen
                ArrayList<CF_Player> sortiert = bubbleSort();
                for (int playerIndex = 0; playerIndex < sortiert.size(); playerIndex++) { //playerIndex = jeder einzelnen Player
                    g2.setColor(sortiert.get(playerIndex).color);
                    g2.setFont(new Font("Arial",30,30));
                    g2.drawString(sortiert.get(playerIndex).name+": "+sortiert.get(playerIndex).points, GUI.getWidth()-390, 25+(playerIndex*30));
                    if(sortiert.get(playerIndex).alive) {
                        g2.setFont(new Font("Arial",100,100));
                        g2.drawString(sortiert.get(playerIndex).name+" won!!!", (GUI.getWidth()-400)/2-50, GUI.getHeight()/2);
                    }
                }
            }
            //</editor-fold>
        //</editor-fold>
            
        //<editor-fold defaultstate="collapsed" desc="PAUSEZUSTAND: PAUSE">
        } else if(pause>2)  {   //Pause
            //Zeichne existierende Shapes/Linien jedes Spielers
            g2.setFont(new Font("Arial",30,30));
            g2.setColor(player.get(0).color);
            if(player.get(1).KI) { g2.drawString(player.get(0).name+": "+(einSpielerRekord/1000)+" Sekunden", GUI.getWidth()-390, 25); }
            
            //Zeichne vorhandenen Linien
            for (int playerIndex = 0; playerIndex < player.size(); playerIndex++) { //playerIndex = jeder einzelnen Player 
                g2.setColor(player.get(playerIndex).color);
                if(!player.get(1).KI) { g2.drawString(player.get(playerIndex).name+": "+player.get(playerIndex).points, GUI.getWidth()-390, 25+(playerIndex*30)); }
                for(int shapeNummer=0; shapeNummer<player.get(playerIndex).shapes.size();shapeNummer++) { //shapeNummer = jeder einzelne Punkt
                    g2.setStroke(player.get(playerIndex).shapeSizes.get(shapeNummer));
                    g2.draw(player.get(playerIndex).shapes.get(shapeNummer));
                }
            }
            
            //Zeichne existierende Items
            for (int fieldItem = 0; fieldItem < fieldItems.size(); fieldItem++) {
                CF_Item curItem = fieldItems.get(fieldItem);
                BufferedImage buffImage = null;
                g2.setColor(Color.blue);
                g2.setFont(new Font("Arial",15,15));
                    if(new File(System.getProperty("user.dir")+"\\src").exists()) { 
                        try {
                            //Mit NetBeans gestartet
                            File curFile = new File(curItem.itemFile);
                            int lastIndexSlash = curFile.getAbsolutePath().lastIndexOf("\\");
                            String newFilePath = curFile.getAbsolutePath().substring(0,lastIndexSlash)+"\\src\\Spiel\\Ressourcen\\"+curFile.getAbsolutePath().substring(lastIndexSlash);
                            buffImage = ImageIO.read(new File(newFilePath));
                        } catch (IOException ex) { System.err.println("CFS_CFSinglePlayer: paintComponent: RUNNING: fieldItemListe: "+ex); }
                    } else {    //JAR gestartet
                        Image itemImage;
                        Image itemsImage = Toolkit.getDefaultToolkit().getImage(getClass().getResource("/Spiel/Ressourcen/"+curItem.itemFile));
                        buffImage = new BufferedImage(itemsImage.getWidth(null),itemsImage.getHeight(null),BufferedImage.TYPE_INT_ARGB);
                        itemImage = itemsImage;
                        Graphics2D bGr = buffImage.createGraphics();
                        bGr.drawImage(itemImage, 0, 0, null);
                        bGr.dispose();
                    }
                    g2.drawImage(buffImage, curItem.x,curItem.y,null);
            }
            
            //Zeichne String "PAUSE"
            g2.setFont(new Font("Arial",30,100));
            g2.setColor(Color.WHITE);
            g2.drawString("!!PAUSE!!", (GUI.getWidth()-400)/2-200, GUI.getHeight()/2-50);
            //</editor-fold>
            
        //<editor-fold defaultstate="collapsed" desc="PAUSEZUSTAND: Pause vorbei: Setzte Spiel zum Zustand vor der Pause zurück">
        } else if(pause==2) {  //Pause = 2; im letztem Frame vorbeigegangen
            pause = 0-curTime;
            for (int playerIndex = 0; playerIndex < player.size(); playerIndex++) {
                if(player.get(playerIndex).effekt == null) {  
                } else if("Bigger".equals(player.get(playerIndex).effekt.name)) {
                    g2.setStroke(new BasicStroke(tweakGroeßePlayer+4));
                } else if("Smaller".equals(player.get(playerIndex).effekt.name)) {
                    g2.setStroke(new BasicStroke(tweakGroeßePlayer-4));
                }
                if(player.get(playerIndex).resetEffektThread!=null) player.get(playerIndex).resetEffektThread.pause = false;
            }
        }
        //</editor-fold>
    }
    //</editor-fold>

    //<editor-fold defaultstate="collapsed" desc="Datenbank">
    /**
     * Laed die Tweak Werte aus der Datenbank
     */
    private void ladeTweakWerte() {
        try {
            stmt = conn.createStatement();
            rs = stmt.executeQuery("SELECT * FROM tweakwerte");
            
            // Tue etwas mit dem ResultSet ...
            rs.next();
            int wert = rs.getInt("wert");
            tweakGeschwindigkeit = wert;
            rs.next();
            wert = rs.getInt("wert");
            tweakGroeßeCollRadius = wert;
            rs.next();
            wert = rs.getInt("wert");
            tweakWahrscheinlichkeitItem = wert;
            rs.next();
            wert = rs.getInt("wert");
            tweakGroeßePlayer = wert;
            rs.next();
            wert = rs.getInt("wert");
            startStartingPause = wert;
            rs.next();
            wert = rs.getInt("wert");
            startingRunningPause = wert;
            rs.next();
            wert = rs.getInt("wert");
            runningStoppingPause = wert;
            rs.next();
            wert = rs.getInt("wert");
            WinEXITPause = wert;
            rs.next();
            wert = rs.getInt("wert");
            tweakItemFeldGroeße = wert;
            rs.next();
            wert = rs.getInt("wert");
            tweakWahrscheinlichkeitDontDrawShape = wert;
            rs.next();
            wert = rs.getInt("wert");
            CF_KL.rotationSpeed = wert;
            rs.next();
            wert = rs.getInt("wert");
            CF_KL.squareRotationSpeed = wert;
            rs.next();
            wert = rs.getInt("wert");
            CF_KL.squareRotationAngle = wert;
            rs.next();
            wert = rs.getInt("wert");
            tweakFieldItemsSize = wert;
            
            if (stmt != null) {
                try { stmt.close(); } catch (SQLException sqlEx) { System.err.println("CFS_SinglePlayer: ladeTweakWerte: "+sqlEx); stmt = null;  }
            }
        } catch (SQLException ex) {
            System.err.println("CFS_CFSinglePlayer: setUpItems: SQLException: " + ex.getMessage());
            System.err.println("CFS_CFSinglePlayer: setUpItems: SQLState: " + ex.getSQLState());
            System.err.println("CFS_CFSinglePlayer: setUpItems: VendorError: " + ex.getErrorCode());
            System.err.println("CFS_CFSinglePlayer: setUpItems: " + ex);
        } finally {
            if (rs != null) {
                try { rs.close(); } catch (SQLException sqlEx) { System.err.println("CFS_SinglePlayer: ladeTweakWerte: "+sqlEx); rs = null; }
            }
        }
    }
    
    /**
     * Läd die Items aus der Datenbank, oder aus der Datei
     * @param mitDatenbank Aus Datenbank (true), oder aus Datei(false)?
     */
    private void setUpItems(boolean mitDatenbank) {
        items.clear();
        
        if(mitDatenbank) {
            try {
                stmt = conn.createStatement();
                rs = stmt.executeQuery("SELECT * FROM items");
                while(rs.next()) {
                    String name = rs.getString("name");
                    int effect = rs.getInt("effekt");
                    int dauer = rs.getInt("dauer");
                    String effectedPlayer = rs.getString("effectedPlayer");
                    String itemFile = rs.getString("itemDateiname");
                    Image image = null;
                    try {
                        image = ImageIO.read(new File(itemFile));
                    } catch(IOException ex) { }
                        items.add(new CF_Item(name,effect,dauer,effectedPlayer,image,itemFile));    
                }

                if (stmt != null) {
                    try { stmt.close(); } catch (SQLException sqlEx) { System.err.println("CFS_SinglePlayer: ladeItems: "+sqlEx);  stmt = null; }
                }
            } catch (SQLException ex) {
                // Fehler behandeln
                System.err.println("CFS_CFSinglePlayer: setUpItems: SQLException: " + ex.getMessage());
                System.err.println("CFS_CFSinglePlayer: setUpItems: SQLState: " + ex.getSQLState());
                System.err.println("CFS_CFSinglePlayer: setUpItems: VendorError: " + ex.getErrorCode());
                System.err.println("CFS_CFSinglePlayer: setUpItems: " + ex);
            } finally {
                if (rs != null) {
                    try { rs.close(); } catch (SQLException sqlEx) { System.err.println("CFS_SinglePlayer: ladeItems: "+sqlEx); rs = null; }
                }
            }
        } else { 
            try {
                //Lade Items ohne Datenbank
                BufferedReader BR = new BufferedReader(new FileReader("items.txt"));
                String zeile;
                while((zeile=BR.readLine())!=null) {
                    String[] itemInfos = zeile.split(":");
                    Image image = null;
                    //Image image = ImageIO.read(new File(itemInfos[4]));
                    items.add(new CF_Item(itemInfos[0],Integer.parseInt(itemInfos[1]),Integer.parseInt(itemInfos[2]),itemInfos[3],image,itemInfos[4]));
                }
            } catch (IOException ex) { System.err.println("CFS_SinglePlayer: setUpItems: ELSE: "+ex); } 
        }
    }
    
    /**
     * Stellt eine Connection mit der Datenbank her
     * @param host Host der Datenbank
     * @param datenbank Datenbank die geladen werden soll
     * @param username Username zur Datenbank
     * @param password Passwort zur Datenbank
     * @return true, falls Verbindung zu Parametern oder localhost erfolgreich war, ansonsten false
     */
    private boolean ladeDatenbank(String host, String datenbank, String username, String password) {
        try {
            Class.forName("com.mysql.jdbc.Driver").newInstance();   
            conn = DriverManager.getConnection("jdbc:mysql://"+host+"/"+datenbank+"?user="+username+"&password="+password);
        } catch (SQLException ex) {
            try {
                conn = DriverManager.getConnection("jdbc:mysql://localhost/curvefever?user=dom4&password=dom");
            } catch(SQLException ex1) {
                System.err.println("CFS_CFSinglePlayer: Datenbank konnte nicht geladen werden. Starte mit DEFAULT Werten und keinen Items");
                return false;
            }
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException ex) {
            System.err.println("CFS_CFSinglePlayer: ladeDatenbank: " + ex);
            System.exit(1);
        } finally {
            if (rs != null) {
                try {  rs.close();} catch (SQLException sqlEx) { System.err.println("CFS_SinglePlayer: ladeDatenbank: "+sqlEx); rs = null; }
            }
        }
        return true;
    }
    //</editor-fold>
    
    //<editor-fold defaultstate="collapsed" desc="RESET Methoden">
    /**
     * Resets the playing field
     * @param mitItems
     */
    protected void resetField(boolean mitItems) {
        if(mitItems) fieldItems = new ArrayList<>();
        for (int x = 0; x < GUI.getWidth()-380; x++) {
            for (int y = 0; y < GUI.getHeight()+20; y++) {
                if(!(coords[x][y] instanceof CF_Item) || mitItems) //Wenn es kein Item ist, oder wenn auch alle Items aufgeräumt werden sollen 
                    coords[x][y] = (long)0;
                colorCoords[x][y] = Color.black;
            }
        }
        walls.clear();
        r = 0; g = 0; b = 0; RGBStatus = "END"; colorChangeRound2 = false;
    }
    
    /**
     * Reset player Variables for restart
     */
    protected void resetPlayers() {
        int abstandZwischenPlayer = (GUI.getWidth()-400)/player.size();
        for (int i = 0; i < player.size(); i++) {
            int spielerX = abstandZwischenPlayer*i+abstandZwischenPlayer/2;
            int spielerY = GUI.getHeight()/2;
            if("Random".equals(startPositionen)) {
                spielerX = random.nextInt(GUI.getWidth()-500)+100;
                spielerY = random.nextInt(GUI.getHeight()-100)+100;
            }
            player.get(i).x = spielerX;
            player.get(i).y = spielerY;
            player.get(i).angle = new java.util.Random().nextInt(360);
            player.get(i).shapes = new ArrayList<>();
            player.get(i).shapeSizes = new ArrayList<>();
            if(!player.get(i).timeout) player.get(i).alive = true;
            player.get(i).effekt = null;
        }
    }
    
    public void sichereItems() {
        for (int playerIndex = 0; playerIndex < player.size(); playerIndex++) {
            if(player.get(playerIndex).resetEffektThread!=null) player.get(playerIndex).resetEffektThread.pause = true;
        }
    }
    //</editor-fold>
    
    //<editor-fold defaultstate="collapsed" desc="Andere Methden">
    public ArrayList<CF_Player> bubbleSort() {
        ArrayList<CF_Player> backupSpielerListe = new ArrayList<>();
        for (int i = 0; i < player.size(); i++) {
            backupSpielerListe.add(player.get(i));
        }
        ArrayList<CF_Player> sortiertePunkteliste = new ArrayList<>();
        for (int i = 0; i < player.size(); i++) {
            int hoechster = -1;
            int indexHoechster = -1;
            for (int j = 0; j < backupSpielerListe.size(); j++) {
                if(backupSpielerListe.get(j).points>hoechster) {
                    hoechster = backupSpielerListe.get(j).points;
                    indexHoechster = j;
                }
            }
            sortiertePunkteliste.add(backupSpielerListe.get(indexHoechster));
            backupSpielerListe.remove(backupSpielerListe.get(indexHoechster));
        }
        return sortiertePunkteliste;
    }
    
    private void iterateColor(Graphics graphics) {
        graphics.setColor(new Color(0,0,0,255));
        graphics.fillRect(getWidth()-400, 0, getWidth(), getHeight());
        graphics.setColor(new Color(r,g,b,255)); 
        graphics.fillRect(0, 0, getWidth()-400, getHeight()); 
        if(pause<=0) {
            switch(RGBStatus) {
                case "R0":
                    b-=10;
                    if(b<=10) {
                        b = 0;
                        g = 0;
                        if(colorChangeRound2) {
                            r-=10;
                            if(r<=10) {
                                r = 0;
                                RGBStatus = "END";
                                colorChangeRound2 = false;
                            }
                        } else {
                            RGBStatus = "R1";
                            colorChangeRound2 = true;
                        }
                    }
                    break;
                case "R1":
                    g+=10;
                    if(g>=245) {
                        RGBStatus = "G0";
                    }
                    break;
                case "G0":
                    r-=10;
                    if(r<=10) {
                        RGBStatus = "G1";
                    }
                    break;
               case "G1":
                    b+=10;
                    if(b>=245) {
                        RGBStatus = "B0";
                    }
                    break;
               case "B0":
                    g-=10;
                    if(g<=10) {
                        RGBStatus = "B1";
                    }
                    break;
               case "B1":
                    r+=10;
                    if(r>=245) {
                        RGBStatus = "R0";
                    }
                    break;
            } 
        }
    }
    //</editor-fold>
    
    //<editor-fold defaultstate="collapsed" desc="GetterSetter">
    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    public String getTweakWerte() {
        return tweakGeschwindigkeit+":"+
                tweakGroeßeCollRadius+":"+
                tweakWahrscheinlichkeitItem+":"+
                tweakGroeßePlayer+":"+
                tweakWahrscheinlichkeitDontDrawShape+":"+
                startStartingPause+":"+
                startingRunningPause+":"+
                runningStoppingPause+":"+
                WinEXITPause+":"+
                tweakItemFeldGroeße;
    }
    
    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    
    public String getItems() {
        String returnString = "";
        for (int i = 0; i < items.size(); i++) {
            CF_Item curItem = items.get(i);
            returnString += curItem.name+":"+curItem.effekt+":"+curItem.dauer+":"+curItem.effectedPlayer+":"+curItem.itemFile+"_";
        }
        return returnString;
    }
    
    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    
    public long getPause() { return pause; }
    public void setPause(long pPause) { 
        if(pauseMoeglich || netzwerkClient) { pause = pPause; } 
        for (int j = 0; j < player.size(); j++) { //Setze Coll Felder zurück, sonst Collision wenn Pause endet
            for (int x = -1-tweakGroeßeCollRadius; x < tweakGroeßeCollRadius+1; x++) {
                for (int y = -1-tweakGroeßeCollRadius; y < tweakGroeßeCollRadius+1; y++) {
                    int xPlayer = (int)player.get(j).x+x;
                    int yPlayer = (int)player.get(j).y+y;
                    if(xPlayer>0 && yPlayer>0 && xPlayer<coords.length-1 && yPlayer<coords.length-1)
                        coords[(int)player.get(j).x+x][(int)player.get(j).y+y] = (long)0;
                }
            }
        }
    }
    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //</editor-fold>
    
    /*
    * TODO: (Problem (Lösung)) (- = Muss noch gelöst werden; 0 = Test notwendig)
    * - KISpieler: Übergang 0/360 360/0 Grad komisch (< und > ändern sich schlagartig, da Angle auf 0 bzw 360 gesetzt wird)
    */
    
    /*
     * REMINDER: (- Event  - Was gemacht werden muss)
     * 
     * 
     */
}
