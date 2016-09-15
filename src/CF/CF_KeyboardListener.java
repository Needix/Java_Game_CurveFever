/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package CF;

import java.awt.event.*;
import java.util.ArrayList;
        
/**
 *
 * @author Need
 */
public class CF_KeyboardListener implements KeyListener {
    private final ArrayList <CF_Player> player;
    private final ArrayList <CF_Key> pressed = new ArrayList<>();
    private final CFS_CurveFeverSingleplayer CF;
    private final CF_KeyPressedThread KPT;
    
    //Tweaks:
    public int rotationSpeed = 8; //Grad; je höher, desto schneller
    public int squareRotationSpeed = 200; //Millisekunden; je niedriger, desto schneller
    public int squareRotationAngle= 90; //Grad; je höher, desto steiler
    
    public CF_KeyboardListener(ArrayList <CF_Player>player, CFS_CurveFeverSingleplayer CFS, Object playerSpeed) {
        this.player = player;
        this.CF = CFS;
        KPT = new CF_KeyPressedThread(this, playerSpeed);
        KPT.start();
    }

    
    public void checkKeys() {
        long curTime = System.currentTimeMillis();
        for (int pressedIndex = 0; pressedIndex < pressed.size(); pressedIndex++) {
            KeyEvent e = pressed.get(pressedIndex).e;
            if(e.getKeyCode()==KeyEvent.VK_SPACE) {
                long tPause = CF.getPause();
                if(tPause==0 || (tPause<0 && tPause+curTime>=100)) {
                    CF.sichereItems();
                    CF.setPause(curTime);
                } else if(curTime-tPause >=100) {
                    CF.setPause(2);
                }
            } else if(e.getKeyCode()==KeyEvent.VK_ESCAPE) {
                System.exit(0);
            }
            
            for (int playerIndex = 0; playerIndex < player.size(); playerIndex++) {
                CF_Player tPlayer = player.get(playerIndex);
                
                if(e.getKeyCode()==player.get(playerIndex).getLinks() && !"NoControl".equals(player.get(playerIndex).getEffektName())) {      //Left Key
                    if("Square".equals(player.get(playerIndex).getEffektName())) {
                        if(pressed.get(pressedIndex).lastTimeChecked+squareRotationSpeed<curTime) {
                            player.get(playerIndex).setAngle(player.get(playerIndex).getAngle()-squareRotationAngle);
                            pressed.get(pressedIndex).lastTimeChecked = curTime;
                        }
                    } else {
                        if("ReverseControls".equals(tPlayer.getEffektName()))
                            player.get(playerIndex).setAngle(player.get(playerIndex).getAngle()+rotationSpeed);
                        else 
                            player.get(playerIndex).setAngle(player.get(playerIndex).getAngle()-rotationSpeed);
                    }
                } else if(e.getKeyCode()==player.get(playerIndex).getRechts() && !"NoControl".equals(player.get(playerIndex).getEffektName())) { //Right Key
                    if("Square".equals(player.get(playerIndex).getEffektName())) {
                        if(pressed.get(pressedIndex).lastTimeChecked+squareRotationSpeed<curTime) {
                            player.get(playerIndex).setAngle(player.get(playerIndex).getAngle()+squareRotationAngle);
                            pressed.get(pressedIndex).lastTimeChecked = curTime;
                        }
                    } else {
                        if("ReverseControls".equals(tPlayer.getEffektName()))
                            player.get(playerIndex).setAngle(player.get(playerIndex).getAngle()-rotationSpeed);
                        else
                            player.get(playerIndex).setAngle(player.get(playerIndex).getAngle()+rotationSpeed);
                    }
                }
                
                if(player.get(playerIndex).getAngle()>=360) {
                    player.get(playerIndex).setAngle(player.get(playerIndex).getAngle()-360);
                } else if(player.get(playerIndex).getAngle()<0) {
                    player.get(playerIndex).setAngle(player.get(playerIndex).getAngle()+360);
                }
            }
        }
    }
    
    @Override
    public void keyPressed(KeyEvent tempE) {
        if(tempE!=null) {
            boolean alreadyInAL = false;
            for (int i = 0; i < pressed.size(); i++) {
                if(pressed.get(i).e.getKeyCode() == tempE.getKeyCode()) {
                    alreadyInAL = true;
                }
            }
            if(!alreadyInAL) {  pressed.add(new CF_Key(tempE,System.currentTimeMillis())); }
        }
    }
    
    /**
     * Key e released
     * Remove e from pressed
     */
    @Override
    public void keyReleased(KeyEvent e) {
        for (int i = 0; i < pressed.size(); i++) {
            if(pressed.get(i).e.getKeyCode() == e.getKeyCode()) {
                pressed.remove(i);
            }
        }
    }

    @Override
    public void keyTyped(KeyEvent e) { /*Wird nicht benutzt*/ }
}
