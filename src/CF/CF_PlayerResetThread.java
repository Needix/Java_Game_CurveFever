/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package CF;

/**
 *
 * @author Need
 */
public class CF_PlayerResetThread extends Thread { 
    public boolean reset = true; 
    public int sleep; 
    public CF_Player player;
    public boolean pause = false;
    
    public CF_PlayerResetThread(int sleep, CF_Player player) { 
        this.sleep = sleep; 
        this.player = player;
    } 
    
    public void setReset(boolean reset) { this.reset = reset; }
    
    public void run() { 
        int repeat = 100;
        for (int i = 0; i < repeat; i++) {
            if(!pause) {
                try { 
                    Thread.sleep(sleep/repeat); 
                } catch (InterruptedException ex) { System.err.println(ex); if(reset) player.effekt = null; }
            } else {
                i--;
            }
        }
        if(reset) player.effekt = null; 
    }
}
