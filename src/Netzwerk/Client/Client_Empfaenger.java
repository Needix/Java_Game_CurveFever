/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package Netzwerk.Client;

import GUI.StartGUI;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.Thread.UncaughtExceptionHandler;
import java.lang.reflect.InvocationTargetException;
import java.net.Socket;
import javax.swing.DefaultListModel;
import javax.swing.SwingUtilities;

/**
 * Client Empfaenger, der auf Nachrichten vom Server wartet
 * @author Jonas Oeldemann
 */
public class Client_Empfaenger extends Thread implements UncaughtExceptionHandler {
    ////Konstanten
    //Objekte
    private final Client_ConnectorSender CConSender;
    private final Socket Verbindung;
    private final DefaultListModel<String> dlm_spielerListe;
    private final StartGUI GUI;
    BufferedReader Input;
    
    ////Variabeln
    private boolean ende;
    private int anzahlSpieler;
    
    /**
     * Client_Empfaenger Konstruktor
     * @param GUI Die Oberflaeche des Clients
     * @param Sender Sender, der die Nachrichten an den Server sendet
     * @param Client Verbindung zum Server
     * @param dlm SpielerListe, die auf der GUI angezeigt wird
     */
    public Client_Empfaenger(StartGUI GUI, Client_ConnectorSender Sender, Socket Client, DefaultListModel<String> dlm) {
        this.GUI = GUI;
        this.dlm_spielerListe = dlm;
        this.CConSender = Sender;
        this.Verbindung = Client;
        try { Input = new BufferedReader(new InputStreamReader(Verbindung.getInputStream())); } catch (IOException ex) { System.err.println("Client_Empfaenger: Konstr: "+ex); }
        ende=false;
    }
    
    /**
     * Wartet auf Nachrichten, bis Server oder Client beendet werden
     */
    @Override
    public void run() {
//        GUI.TextArea_Chatverlauf.append("Verbindung mit "+ (char)34 +Client.getInetAddress()+ (char)34 +" wurde hergestellt. \n");
        System.out.println("Client_Empfaenger: Verbindung mit "+ (char)34 +Verbindung.getInetAddress()+":"+Verbindung.getPort()+ (char)34 +" wurde hergestellt.");
        while(!ende) {
            warteAufNachricht();
        }
        //GUI.dispatchEvent(new WindowEvent(GUI, WindowEvent.WINDOW_CLOSING));
        //Client_GUIe.beende("");
    }
    
    /**
     * Wartet auf eine Nachricht
     * Falls ein Befehl vorhanden ist, handle entsprechend
     *      falls nicht, teile dem Benutzer die Nachricht mit (TextArea)
     */
    public void warteAufNachricht() {
        String message;
        try {
            //System.out.println("Client ("+Sender.getNutzername()+"): WarteAufNeueNachricht.");
            message=Input.readLine();
            if(message==null) { ende = true; return ; }
            //System.out.println("Client ("+CConSender.nutzername+"): Nachricht empfangen: "+message);
            if(!sucheKommando(message)) {   //Kein Kommando enthalten
                System.err.println("WARNING: Client hat kein Kommando gefunden. Nachricht: "+message);
            }
        } catch (IOException ex) {
            System.err.println("Client_Empfaenger: warteAufNachricht: "+ex);
            ende = true;
            //System.exit(0);
        }
    }
    
    /**
     * Suche nach einem Kommando
     * @param pNachricht Die Nachricht, die nach einem Kommando durchsucht werden soll
     * @return true, falls ein Befehl vorhanden ist, ansonsten false
     */
    public boolean sucheKommando(String pNachricht) {
        final String[] kommando;
        kommando = pNachricht.split(":");
        
        //Ein neuer Teilnehmer ist hinzugekommen, fuege an TextArea und TeilnehmerListe an
        switch(kommando[0]) {
            case "+UpdateTeilnehmerListe": 
                try {
                    SwingUtilities.invokeAndWait(new Runnable() { public void run() {
                        CConSender.teilnehmerNamen.clear();
                        dlm_spielerListe.clear();

                        for (int i = 1; i < kommando.length; i++) {
                            String teilnehmer=kommando[i];
                            if(CConSender.nutzername.equals(teilnehmer)) {
                                GUI.joinServer_toggleb_links.setBackground(GUI.colors.get(i));
                                GUI.joinServer_toggleb_rechts.setBackground(GUI.colors.get(i));
                            }
                            CConSender.teilnehmerNamen.add(teilnehmer);
                            dlm_spielerListe.addElement(teilnehmer);
                        }
                        for (int i = 0; i < anzahlSpieler-CConSender.teilnehmerNamen.size(); i++) {
                            dlm_spielerListe.addElement("Unbesetzt");
                        }
                        if(!GUI.joinServer_toggleb_links.isEnabled()) 
                            GUI.joinServer_b_ready.setEnabled(true);
                        GUI.joinServer_toggleb_links.setEnabled(true);
                        GUI.joinServer_toggleb_rechts.setEnabled(true);
                    }});
                } catch (InterruptedException | InvocationTargetException ex) {
                    System.err.println("Client_Empfaenger: sucheKommando: UpdateTeilnehmerliste: "+ex);
                }
                break;
             
            case "+VerbindungsAufbau_ServerEinstellungen":
                GUI.setServerEinstellungen(kommando[1],kommando[2],kommando[3],kommando[4],kommando[5],kommando[6],kommando[7],kommando[8]);
                anzahlSpieler = Integer.parseInt(kommando[5]);
                
                for (int i = 0; i < anzahlSpieler-CConSender.teilnehmerNamen.size(); i++) {
                    dlm_spielerListe.addElement("Unbesetzt");
                }
                break;
                
            case "Spiel_ServerStartetSpiel_PING":
                CConSender.sendeNachricht("+Spiel_ServerStartetSpiel_PINGEmpfangen");
                break;
                
            case "Spiel_SpielWirdErstellt_ErstelleSpiel":
                GUI.erstelleSpiel(kommando[1]);
                break;
                
            case "Spiel_SpielSTART":
                GUI.newGUI.setVisible(true);
                GUI.frame_serverBeitreten.setVisible(false);
                //GUI.CFM.starte();
                break;
                
            case "Spiel_LadeTweakWerte":
                GUI.CFM_Client.setTweakWerte(pNachricht.substring(pNachricht.indexOf(":")+1));
                break;
                
            case "Spiel_LadeItems":
                if(kommando.length==1)
                    GUI.CFM_Client.setItems(kommando[1]);
                break;
            
            case "UpdateSpielInformationen": 
                String message;
                String fortschritt = "player";
                String updateString = "UpdateSpielInformationen:___";
                for(int i=0; true; i++) {
                    try {
                        message = Input.readLine();
                        if("*_*END*_*".equals(message)) { break; }
                        
                        if("___".equals(message)) {
                            fortschritt = "items";
                            i=-1;
                        } else if("###".equals(message)) {
                            fortschritt = "ending";
                            i=-1;
                        } else {
                            switch(fortschritt) {
                                case "player":
                                    updateString+="_:_"+message;
                                    break;

                                case "items":
                                    if(i==0) { updateString+="___###"+message; } else
                                        updateString+="###"+message;
                                    break;

                                case "ending":
                                    if(i==0) { updateString += "___"+message; } else { updateString += ":"+message; }
                                    break;
                            }
                        }
                    } catch (IOException ex) { System.err.println("Client_Empfaenger: sucheKommando: UpdateSpielInformationen: "+ex); }
                }
                
                GUI.CFM_Client.update(updateString);
                break;
                
            case "GetAngle":
                GUI.CFM_Client.sendAngle();
                break;
                        
            case "ClientBeendet":
                for (int i = 0; i < CConSender.teilnehmerNamen.size(); i++) {
                    if(CConSender.teilnehmerNamen.get(i).contains(kommando[1])) {
                        dlm_spielerListe.remove(i);
                        CConSender.teilnehmerNamen.remove(i);
                        break;
                    }
                }
                for (int i = 0; i < CConSender.teilnehmerNamen.size(); i++) {
                    if(CConSender.nutzername.equals(CConSender.teilnehmerNamen.get(i))) {
                            GUI.joinServer_toggleb_links.setBackground(GUI.colors.get(i));
                            GUI.joinServer_toggleb_rechts.setBackground(GUI.colors.get(i));
                        break;
                    }
                }
                dlm_spielerListe.addElement("Unbesetzt");
                break;
            
            case "Beende":
                ende = true;
                break;
             
            case "BeendeServer": 
                ende = true;
                CConSender.sendeNachricht("+ClientHatVollständigBeendet");
                GUI.showErrorDialog("Server beendet", "Der Server wurde beendet!", GUI);
                dlm_spielerListe.clear();
                dlm_spielerListe.addElement("SERVER BEENDET!");
                GUI.joinServer_b_ready.setEnabled(false);
                GUI.joinServer_toggleb_links.setEnabled(false);
                GUI.joinServer_toggleb_rechts.setEnabled(false);
                break;
                
            case "+ok":
                break;
                
                
        default:
            return false;
        } 
        return true;
    }

    @Override
    public void uncaughtException(Thread t, Throwable e) {
        throw new UnsupportedOperationException(t+" has crashed: "+e); 
    }
}
