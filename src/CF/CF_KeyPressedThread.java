/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package CF;

/**
 *
 * @author joeldem1
 */
public class CF_KeyPressedThread extends Thread {
    private final CF_KeyboardListener KL_CF;
    private boolean abort;
    private int playerSpeed;
    
    //Tweaks
    private final int tweakCheckKeysTime = 50;
    
    public CF_KeyPressedThread(CF_KeyboardListener CF, Object playerSpeed) {
        this.KL_CF = CF;
        this.playerSpeed = Integer.parseInt(playerSpeed+"");
    }
    
    public void run() {
        while(!abort) {
            try {
                Thread.sleep(tweakCheckKeysTime);
                KL_CF.checkKeys();
            } catch (InterruptedException ex) { System.err.println("KeyPressedThread: run(): "+ex); }
        }
    }
}
