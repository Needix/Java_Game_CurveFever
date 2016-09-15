/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package CF;

import java.awt.Image;

/**
 *
 * @author Need
 */
public class CF_Item {
    public final String name;
    public final int effekt;
    public final int dauer;
    public final int x;
    public final int y;
    public final String effectedPlayer;
    public Image itemImage;
    public String itemFile;
    
    /**
     * Erstellt ein Item
     * @param name Name des Items
     * @param effekt Effekt des Items (-1 = Negativ; 0 = Neutral; 1 = Positiv)
     * @param dauer Dauer des Effekts in Millisekunden
     * @param effectedPlayer Spieler, die von diesem Item beeinflusst werden (alle/nur der Einsammler/alle außer der Einsammler)
     * @param itemImage Image des Items
     * @param file Datei des Items
     */
    public CF_Item(String name, int effekt, int dauer, String effectedPlayer, Image itemImage, String file) {
        this.itemFile = file;
        this.itemImage = itemImage;
        this.name = name;
        this.effekt = effekt;
        this.dauer = dauer*1000;
        this.effectedPlayer = effectedPlayer;
        this.x = -50;
        this.y = -50;
    }
    
    /**
     * Erstellt ein Item
     * @param name Name des Items
     * @param effekt Effekt des Items (-1 = Negativ; 0 = Neutral; 1 = Positiv)
     * @param dauer Dauer des Effekts in Millisekunden
     * @param effectedPlayer Spieler, die von diesem Item beeinflusst werden (alle/nur der Einsammler/alle außer der Einsammler)
     * @param x X-Koordinate des Spielers
     * @param y Y-Koordinate des Spielers
     * @param itemImage Image des Items
     * @param file Datei des Items
     */
    public CF_Item(String name, int effekt, int dauer, String effectedPlayer, int x, int y, Image itemImage, String file) {
        this.itemFile = file;
        this.itemImage = itemImage;
        this.name = name;
        this.effekt = effekt;
        this.dauer = dauer*1000;
        this.effectedPlayer = effectedPlayer;
        this.x = x;
        this.y = y;
    }
}
