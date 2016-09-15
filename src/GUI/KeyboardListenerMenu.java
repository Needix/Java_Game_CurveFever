/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package GUI;
import java.awt.event.*;
import javax.swing.JToggleButton;

/**
 *
 * @author Need
 */
public class KeyboardListenerMenu implements KeyListener {
    StartGUI GUI;
    private final JToggleButton button;
    
    public KeyboardListenerMenu(JToggleButton button) {
        this.button = button;
    }
    
     @Override
    public void keyPressed(KeyEvent tempE) {
        if(button.isSelected()) {
            int keyCode = tempE.getKeyCode();
            if((keyCode>=65 && keyCode<=90)||(keyCode>=97 && keyCode<=122)) {
                button.setText(tempE.getKeyChar()+"");
                button.setSelected(false);
            }
        }
    }
    
    @Override
    public void keyReleased(KeyEvent e) {  /*Wird nicht benutzt*/  }

    @Override
    public void keyTyped(KeyEvent e) { /*Wird nicht benutzt*/ }
}
