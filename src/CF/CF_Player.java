/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package CF;

import java.awt.Color;
import java.util.ArrayList;
import java.awt.*;

/**
 *
 * @author Need
 */
public class CF_Player {
    public CF_Item effekt;
    public CF_PlayerResetThread resetEffektThread;
    
    public String name;
    public int playerNummer;
    public int angle;
    public int keyL;
    public int keyR;
    public int points;
    public double x;
    public double y;
    public long dontDraw;
    public boolean alive;
    public boolean KI;
    public boolean timeout;
    public Color color;
    
    public ArrayList <Shape>shapes;
    public ArrayList <Stroke>shapeSizes;
    
    public CF_Player(String name, double x, double y, Color color, int keyR, int keyL, int playerNummer, boolean KI) {
        this.name = name;
        this.playerNummer = playerNummer;
        this.x = x;
        this.y = y;
        this.color = color;
        this.keyR = keyR;
        this.keyL = keyL;
        this.KI = KI;
        this.timeout = false;
        angle = new java.util.Random().nextInt(360);
        shapes = new ArrayList<>();
        shapeSizes = new ArrayList<>();
        alive=true;
        points=0;
        dontDraw = 0;
        effekt = null;
    }
    
    public void changeEffekt(CF_Item item) {
        effekt = item;
        if(resetEffektThread!=null) {   //Verhindere Behinderung durch evtl schon laufende resetEffektThreads
           resetEffektThread.reset = false;
        }

        CF_PlayerResetThread t = new CF_PlayerResetThread(item.dauer, this);
        resetEffektThread = t;
        t.start();
    }
    
    //<editor-fold defaultstate="collapsed" desc="GetterSetter">
    public String getEffektName() {
        if(effekt!=null)
            return effekt.name;
        else
            return null;
    }
    
    public String getEffektString() {
        if(effekt!=null)
            return effekt.name+":"+effekt.effectedPlayer+":"+effekt.dauer+":"+effekt.effekt;
        else
            return null;
    }
    
    public int getLinks() { return keyL; }
    
    public int getRechts() { return keyR; }
    
    public void setAngle(int pAngle) { this.angle = pAngle; }
    
    public int getAngle() { return angle; }
    
    public void setName(String name) { this.name = name; }
    
    public String getName() { return name; }
    //</editor-fold>
}
