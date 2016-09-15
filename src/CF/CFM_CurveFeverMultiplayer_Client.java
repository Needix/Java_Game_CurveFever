/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package CF;

import Netzwerk.Client.Client_ConnectorSender;
import java.awt.Image;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import javax.imageio.ImageIO;
import javax.swing.JFrame;

/**
 *
 * @author Need
 */
public class CFM_CurveFeverMultiplayer_Client extends CFS_CurveFeverSingleplayer {
    private final Client_ConnectorSender CConSender;
    private final int spielerNummer;
    
    public CFM_CurveFeverMultiplayer_Client(JFrame GUI, ArrayList<CF_Player> player, Object playerSpeed, Object necWins, boolean pPausierenMoeglich, Object anzahlSpieler, Object gewinnfoto, boolean items, Client_ConnectorSender CConSend, int spielerNummer, String startPositionen) {
        super(GUI, player, playerSpeed, necWins, pPausierenMoeglich, anzahlSpieler, gewinnfoto, items, true, startPositionen);
        this.CConSender = CConSend;
        this.spielerNummer = spielerNummer;
    }
    
    public void update(String pMessage) { 
        String[] splitOptions = pMessage.split("___");
        splitOptions[0] = "";
        String[] players = splitOptions[1].split("_:_");
        String[] tempFieldItems = splitOptions[2].split("###");
        String[] allgemeinInfos = splitOptions[3].split(":");
        players[0] = "";
        tempFieldItems[0] = "";
            
        for (int i = 0; i < player.size(); i++) {
            String[] playerInfoItemSplit = players[i+1].split("--");
            String[] playerInfos = playerInfoItemSplit[0].split(":");
            String[] playerItemInfos = playerInfoItemSplit[1].split(":");
            if(playerInfos.length == 6) {
                if(!"".equals(playerInfos[0])) { 
                    player.get(i).x = Double.parseDouble(playerInfos[0]); 
                    player.get(i).y = Double.parseDouble(playerInfos[1]); 
                    player.get(i).angle = Integer.parseInt(playerInfos[2]); 
                    player.get(i).dontDraw = Long.parseLong(playerInfos[3]); 
                    player.get(i).alive = "true".equals(playerInfos[4]);
                    player.get(i).points = Integer.parseInt(playerInfos[5]);
                }
                
                if(playerItemInfos.length==4) { 
                    String name = playerItemInfos[0];
                    String effectedPlayer = playerItemInfos[1];
                    int dauer = Integer.parseInt(playerItemInfos[2]);
                    int effekt = Integer.parseInt(playerItemInfos[3]);
                    
                    CF_Item curItemOfPlayer = player.get(i).effekt;
                    if(curItemOfPlayer!=null && !curItemOfPlayer.name.equals(name) && curItemOfPlayer.dauer != dauer && curItemOfPlayer.effekt != effekt && !curItemOfPlayer.effectedPlayer.equals(effectedPlayer)) {
                        player.get(i).changeEffekt(new CF_Item(name, effekt, dauer, effectedPlayer, null, ""));
                    }
                } else {
                    player.get(i).effekt = null;
                }
            }
        }
        
        if(Integer.parseInt(tempFieldItems[1])>fieldItems.size()) {
            int tempSize = fieldItems.size();
            for (int i = tempSize; i < Integer.parseInt(tempFieldItems[1]); i++) {
                String[] fieldItemInfos = tempFieldItems[i+2].split(":");
                if(fieldItemInfos.length >= 7) {
                    String name = fieldItemInfos[0];
                    int effekt = Integer.parseInt(fieldItemInfos[1]);
                    int dauer = Integer.parseInt(fieldItemInfos[2]);
                    int x = Integer.parseInt(fieldItemInfos[3]);
                    int y = Integer.parseInt(fieldItemInfos[4]);
                    String effectedPlayer = fieldItemInfos[5];
                    String urlS = fieldItemInfos[6];
                    for (int j = 7; j < fieldItemInfos.length; j++) {urlS+=":"+fieldItemInfos[j];}
                    Image image = null;
                    try { image = ImageIO.read(new URL(urlS)); } catch(IOException ex) { }
                    CF_Item addItem = new CF_Item(name, effekt, dauer, effectedPlayer, x, y, image, urlS);
                    fieldItems.add(addItem);
                    for (int xCoord = 0; xCoord < tweakItemFeldGroeße; xCoord++) {
                        for (int yCoord = 0; yCoord < tweakItemFeldGroeße; yCoord++) {
                            coords[x+xCoord][y+yCoord] = addItem;
                        }
                    }
                }
            }
        }
        long tempPause = Long.parseLong(allgemeinInfos[0]);
        this.status = allgemeinInfos[1];
        this.statusZeit = Long.parseLong(allgemeinInfos[2]);
        this.statusErsteMal = "true".equals(allgemeinInfos[3]);
        if(tempPause!=0 && tempPause!=this.pause) { this.setPause(tempPause); } else if(tempPause==0) { this.pause = tempPause; }
        GUI.repaint();
        CConSender.sendeNachricht("+Spiel_SpielUpdateFertig");
    }
    
    public void setItems(String itemsS) {
        String[] itemArray = itemsS.split("_");
        for (int i = 0; i < itemArray.length-1; i++) {
            String[] itemInfos = itemArray[i].split(":");
            Image image = null;
            try {
                image = ImageIO.read(new File(itemInfos[4]));
            } catch(IOException ex) { System.err.println("CFS_MultiplayerClient: setItems: "+ex); }
            this.items.add(new CF_Item(itemInfos[0], Integer.parseInt(itemInfos[1]), Integer.parseInt(itemInfos[2]), itemInfos[3], image, itemInfos[4]));
        }
    }
    
    public void setTweakWerte(String werte) {
        String[] wertArray = werte.split(":");
        tweakGeschwindigkeit = Integer.parseInt(wertArray[0]);
        tweakGroeßeCollRadius = Integer.parseInt(wertArray[1]);
        tweakWahrscheinlichkeitItem = Integer.parseInt(wertArray[2]);
        tweakGroeßePlayer = Integer.parseInt(wertArray[3]);
        tweakWahrscheinlichkeitDontDrawShape = Integer.parseInt(wertArray[4]);
        startStartingPause = Integer.parseInt(wertArray[5]);
        startingRunningPause = Integer.parseInt(wertArray[6]);
        runningStoppingPause = Integer.parseInt(wertArray[7]);
        WinEXITPause = Integer.parseInt(wertArray[8]);
        tweakItemFeldGroeße = Integer.parseInt(wertArray[9]);
    }
    
    public void sendAngle() {
        CConSender.sendeNachricht("+SetAngle:"+player.get(spielerNummer).angle);
    }
}
