/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package CF;

import java.util.ArrayList;
import javax.swing.JFrame;
import Netzwerk.Server.*;

/**
 *
 * @author Need
 */
public class CFM_CurveFeverMultiplayer_Server extends CFS_CurveFeverSingleplayer {
    private final Server_Sender SSender;
    private final Server_Connector SConnector;
    
    public CFM_CurveFeverMultiplayer_Server(JFrame GUI, ArrayList<CF_Player> player, Object playerSpeed, Object necWins, boolean pPausierenMoeglich, Object anzahlSpieler, Object gewinnfoto, boolean items, Server_Sender SSender, Server_Connector SCon, String startPositonen) {
        super(GUI,player,playerSpeed,necWins,pPausierenMoeglich,anzahlSpieler,gewinnfoto,items, false, startPositonen);
        this.SSender = SSender;
        this.SConnector = SCon;
        
        String tweakWerte = super.getTweakWerte();
        SSender.sendeNachrichtAnClients("Spiel_LadeTweakWerte:"+tweakWerte);
        try { Thread.sleep(100); } catch (InterruptedException ex) { }
        String itemString = super.getItems();
        SSender.sendeNachrichtAnClients("Spiel_LadeItems:"+itemString);
        try { Thread.sleep(100); } catch (InterruptedException ex) { }
    }
    
    @Override
    public void starte() {
        while(!abbruch) {
            GUI.repaint();
            sendeUpdateToPlayer();
            long sleep = tweakGeschwindigkeit-playerSpeed-lastNetworkDelay;
            if(sleep<=0) sleep = 1;
            try { Thread.sleep(sleep); } catch (InterruptedException ex) {  }
        }
    }
    
    public void sendeUpdateToPlayer() {
        long timeOne = System.currentTimeMillis();
        
        SSender.sendeNachrichtAnClients("GetAngle");
        warteAufSpieler();
        
        long delay1 = System.currentTimeMillis()-timeOne;
        //System.out.println("Benötigte Zeit für GetAngle: "+delay1);
        
        //<editor-fold defaultstate="collapsed" desc="Create UpdateString">
        //___ Trennt verschiedene HauptOptionen (Spieler___FieldItems___AllgemeineOptionen
        //### Trennt verschiedene FieldItems (FieldItem1###FieldItem2###...)
        //_:_ Trennt verschiedene Spieler (Spieler1_:_Spieler2_:_Spieler3...)
        //-- Trennt allgemeine SpielerInfos von Item SpielerInfos (Spieler1Infos--Spieler1ItemInfos)
        //: Trennt verschiedene UnterOptionen (Spieler1Info1:Spieler1Info2:Spieler1Info3... / FieldItem1Info1:FieldItem1Info2:...)
        //Beispiel: ____:_SP1x:SP1y:SP1 ... -- SP1ItemInfos_:_SP2x:SP2y ... ___###FieldItemGröße###Item1Name:Item1Effekt ... ###Item2Name:Item2Effekt ... ___Info1:Info2
        //Echtes Beispiel: "____:_479.71901117042876:621.3899909168948:118:0:true:0--null_:_409.6679118506031:451.24832505726465:78:0:true:0--null___###4###ReverseControls:1:7000:369:81:die anderen:red_reverse.png###NoControl:1:2000:9:941:die anderen:red_x.png###ColorChange:0:1000:841:836:alle:blue_colorchange.png###ColorChange:0:1000:439:450:alle:blue_colorchange.png___0:RUNNING:1387101661679:true"
        SSender.sendeNachrichtAnClients("UpdateSpielInformationen:");
        for (int j = 0; j < player.size(); j++) {
            CF_Player curPlayer = player.get(j);
            SSender.sendeNachrichtAnClients(curPlayer.x+":"+curPlayer.y+":"+curPlayer.angle+":"+curPlayer.dontDraw+":"+curPlayer.alive+":"+curPlayer.points+"--"+curPlayer.getEffektString());
        }
        SSender.sendeNachrichtAnClients("___");
        SSender.sendeNachrichtAnClients(fieldItems.size()+"");
        for (int k = 0; k < fieldItems.size(); k++) {
            CF_Item curItem = fieldItems.get(k);
            SSender.sendeNachrichtAnClients(curItem.name+":"+curItem.effekt+":"+curItem.dauer+":"+curItem.x+":"+curItem.y+":"+curItem.effectedPlayer+":"+curItem.itemFile);
        }
        SSender.sendeNachrichtAnClients("###");
        SSender.sendeNachrichtAnClients(this.pause+"");
        SSender.sendeNachrichtAnClients(this.status+"");
        SSender.sendeNachrichtAnClients(this.statusZeit+"");
        SSender.sendeNachrichtAnClients(this.statusErsteMal+"");  
        SSender.sendeNachrichtAnClients("*_*END*_*");
        //</editor-fold>
        
        long timeTwo = System.currentTimeMillis();
        
        //SSender.sendeNachrichtAnClients("UpdateSpielInformationen:"+update);
        warteAufSpieler();
        
        long delay2 = System.currentTimeMillis()-timeTwo;
        lastNetworkDelay = delay1 + delay2;
        //System.out.println("Benötigte Zeit für Update: "+delay2);
        //System.out.println();
    }
    
    private void warteAufSpieler() {
        for (int i = 0; i < SConnector.ALEmpfaenger.size(); i++) {
            long curTime = System.currentTimeMillis();
            if(SConnector.ALEmpfaenger.get(i).verbindungsEnde) kickeSpieler(i);
            while(!SConnector.ALEmpfaenger.get(i).bereitFuerSpiel && !SConnector.ALEmpfaenger.get(i).verbindungsEnde) {
                if(System.currentTimeMillis()-curTime>5000) { //Timeout
                    System.err.println("Server (UpdateToPlayer2): Kicke Spieler #"+(i+1)+" wegen Timeout!");
                    kickeSpieler(i);
                    return;
                }
                try { Thread.sleep(1); } catch (InterruptedException ex) { System.err.println("StartGUI: starteSpielServer: "+ex); }
            }   
            SConnector.ALEmpfaenger.get(i).bereitFuerSpiel = false;
        }
    }
    
    private void kickeSpieler(int i) {
        //Kicke Spieler
        player.get(i+1).alive = false;
        player.get(i+1).timeout = true;
        //player.remove(i);
        SSender.sendeNachrichtAnClient(SConnector.ALVerbindungen.get(i), "Beende");
        SConnector.ALEmpfaenger.get(i).bereitFuerSpiel = true;
        SConnector.ALEmpfaenger.get(i).verbindungsEnde = true;
        SConnector.ALTeilnehmer.set(i, SConnector.ALTeilnehmer.get(i) + " (Timeout)");
        
//        status = "START";
//        statusErsteMal = true;
//        resetPlayers();
//        resetField(true);
    }
}
