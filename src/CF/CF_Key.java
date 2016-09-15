/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package CF;

import java.awt.event.*;

/**
 *
 * @author Need
 */
public class CF_Key {
    KeyEvent e;
    long lastTimeChecked;
    
    public CF_Key(KeyEvent kc, long ltc) {
        e = kc;
        lastTimeChecked = ltc;
    }
}
