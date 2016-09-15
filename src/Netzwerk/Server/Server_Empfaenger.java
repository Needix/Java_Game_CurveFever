/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package Netzwerk.Server;

import CF.CF_Player;
import GUI.StartGUI;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.Thread.UncaughtExceptionHandler;
import java.lang.reflect.InvocationTargetException;
import java.net.Socket;
import java.util.ArrayList;
import javax.swing.DefaultListModel;
import javax.swing.SwingUtilities;

/**
 * Fuer jede neue Verbindung wird Serverseitig ein Server_Empfaenger erstellt.
 * Dieser wartet NUR auf Nachrichten, die von seinem Clienten kommen und leitet sie an die GUI, den Sender oder andere weiter
 * @author Jonas Oeldemann
 */
public class Server_Empfaenger extends Thread implements UncaughtExceptionHandler {
    ////Konstanten
    // Objekte
    private final Server_Sender Sender;
    private final Server_Connector SCon;
    private final DefaultListModel<String> dlm_spielerListe;
    private final ArrayList<CF_Player> player;
    private final StartGUI GUI;
    private Socket verbindung;
    private final int nummerEmpfaenger;
    BufferedReader Input;
    
    //Variabeln
    public boolean verbindungsEnde;
    public boolean bereitFuerSpiel;
    
    /**
     * Server_Empfaenger Konstruktor
     * @param Sender Eindeutiger Sender. Sender kann Nachrichten an alle Clients, oder einzelne Clients senden
     * @param Connector Connector verwaltet die Clients
     * @param dlm DefaultListModel, zum verwalten der SpielerListe
     * @param player
     * @param nummerEmpfaenger
     * @param verbindung
     * @param GUI
     */
    public Server_Empfaenger(Server_Sender Sender, Server_Connector Connector, DefaultListModel<String> dlm, ArrayList<CF_Player> player, int nummerEmpfaenger, Socket verbindung, StartGUI GUI) {
        this.dlm_spielerListe = dlm;
        this.GUI = GUI;
        this.SCon = Connector;
        this.Sender = Sender;
        this.player = player;
        this.nummerEmpfaenger = nummerEmpfaenger;
        verbindungsEnde=false;
        this.verbindung = verbindung;
        try { Input = new BufferedReader(new InputStreamReader(verbindung.getInputStream())); } catch (IOException ex) {System.err.println("Server_Empfaenger: Konstr: "+ex); }
    }
    
    /**
     * Wartet auf Nachrichten vom Client
     * Wird solange ausgefuehrt, bis Client oder Server beendet wird
     */
    @Override
    public void run() {
        while(!verbindungsEnde) {
            warteAufMessage();
        }
        try {
            if(verbindung!=null) verbindung.close();
            verbindung=null;
        } catch (IOException ex) {
            System.err.println("Server_Empfaenger: InputOutput-Fehler @RUN: "+ex);
        }
    }
    
    /**
     * Wartet auf eine Message
     * Falls eine Nachricht empfangen wird ueberpruefe, ob sie ein Kommando enthaelt (z.B. Private Nachricht (PN))
     * Sende Nachricht an entsprechende Clients weiter
     */
    public void warteAufMessage() {
        if(verbindung!=null) {
            String message;
            try {
                //System.out.println("Server: Warte auf Message.");
                message=Input.readLine();
                //System.out.println("Server: Nachricht empfangen: "+message);
                
                if(!sucheKommando(message)) {   //Kein Kommando enthalten
                    System.err.println("WARNING: Server hat kein Kommando gefunden. Nachricht: "+message);
//                        message += "\n";
//                        //AppendMessageAnChatverlauf
//                        Sender.sendeNachrichtAnClients(message);
                }
            } catch (IOException ex) {
                if("Connection reset".equals(ex.getMessage())) {
                    verbindung = null;
                    verbindungsEnde = true;
                }
                System.err.println("Server_Empfaenger: InputOutput-Fehler @warteAufMessage: "+ex);
            } catch(NullPointerException ex) {    
                System.err.println("Server_Empfaenger: NullPointer @warteAufMessage: "+ex);
                //SGUI.TextArea_Chatverlauf.append("Jemand hat den Chatraum verlassen.\n");
                verbindungsEnde=true;
            }
        }
    }
    
    /**
     * Sucht ein Kommando/Befehl in "pMessage"
     * Falls ein Befehl enthalten, handle entsprechend des Befehls
     * @param pMessage Die Nachricht, die ueberprueft werden soll
     * @return true, falls ein Befehl enthalten, ansonsten false
     */
    public boolean sucheKommando(String pMessage) {
        String[] kommando;
        kommando = pMessage.split(":");     //0 = Name; 1 = Kommando; 2..n = Parameter  ///  z.B. "Heinz:register:Musik"
        
        //Durchsuche Kommando[1] (wo der Befehl moeglicherweise drinsteht) nach einem gueltigen befehl
        switch (kommando[0]) {
            //Jemand hat auf "Bereit" geklickt: Markiere Namen mit " (Fertig)" und schicke geupdatete Spielerliste an alle
            case "Menu_Ready":
                int anzahlReady = 0;
                for (int i = 0; i < SCon.ALTeilnehmer.size(); i++) {
                    if(SCon.ALTeilnehmer.get(i).equals(kommando[1])) { 
                        SCon.ALTeilnehmer.set(i, kommando[1] + " (Fertig)");
                        anzahlReady++;
                    } else if(SCon.ALTeilnehmer.get(i).contains(" (Fertig)")) { 
                        anzahlReady++;
                    }
                }
                
                if(anzahlReady+1==SCon.ALTeilnehmer.size()) {
                    GUI.erstelleServer_b_start.setEnabled(true);
                } else {
                    GUI.erstelleServer_b_start.setEnabled(false);
                }
                
                updateSpielerDLM();
                break;
            
            //case "1SpielBeginnt_Bereit_PingEmpfangen":
            case "+Spiel_ServerStartetSpiel_PINGEmpfangen":    
                bereitFuerSpiel = true;
                Sender.sendeNachrichtAnClient(verbindung, "+ok");
                break;
            
            case "+Spiel_SpielFertigErstellt":
                bereitFuerSpiel = true;
                Sender.sendeNachrichtAnClient(verbindung, "+ok");
                break;
            
            case "+Spiel_SpielUpdateFertig":
                bereitFuerSpiel = true;
                break;
                
            case "+SetAngle":
                player.get(nummerEmpfaenger).setAngle(Integer.parseInt(kommando[1]));
                bereitFuerSpiel = true;
                //Sender.sendeNachrichtAnClient(verbindung, "+ok");
                break;
                
            case "WindowClosing":
                Sender.sendeNachrichtAnClients("ClientBeendet:"+kommando[1]);
                for (int i = 0; i < SCon.ALTeilnehmer.size(); i++) {
                    if(SCon.ALTeilnehmer.get(i).contains(kommando[1])) {
                        try {
                            dlm_spielerListe.remove(i);
                            SCon.ALTeilnehmer.remove(i);
                            
                            Sender.sendeNachrichtAnClient(verbindung, "Beende");
                            SCon.ALVerbindungen.get(i-1).close();
                            SCon.ALVerbindungen.remove(i-1);
                            
                            verbindungsEnde = true;
                            SCon.ALEmpfaenger.remove(i-1);
                            
                            updateSpielerDLM();
                            break;
                        } catch (IOException ex) { System.err.println("Server_Empfaenger: sucheKommado: WindowClosing: "+ex);}
                    }
                }
                //dlm_spielerListe.addElement("Unbesetzt");
                break;
            
            case "+ClientHatVollständigBeendet":
                verbindungsEnde = true;
                Sender.sendeNachrichtAnClient(verbindung, "+ok");
                break;
                
            
            default:    //Kein Befehl gefunden
                return false;
        }
        return true; //Befehl ausgeführt
    }
    
    private void updateSpielerDLM() {
        try {
            SwingUtilities.invokeAndWait(new Runnable() { public void run() {
                int count = dlm_spielerListe.getSize();
                dlm_spielerListe.clear();

                for (int i = 0; i < SCon.ALTeilnehmer.size(); i++) {
                    dlm_spielerListe.addElement(SCon.ALTeilnehmer.get(i));
                }
                for (int i = 0; i < count-SCon.ALTeilnehmer.size(); i++) {
                    dlm_spielerListe.addElement("Unbesetzt");
                }
            }});
        } catch (InterruptedException | InvocationTargetException ex) {
            System.err.println("Client_Empfaenger: sucheKommando: UpdateTeilnehmerliste: "+ex);
        }
        
        String nutzernamen = "+UpdateTeilnehmerListe";
        for (int i = 0; i < SCon.ALTeilnehmer.size(); i++) { nutzernamen+=":"+SCon.ALTeilnehmer.get(i); }
        Sender.sendeNachrichtAnClients(nutzernamen);
    }
    
    // <editor-fold defaultstate="collapsed" desc="Ent- u. Verschluesselung des Passworts fuer Textdatei">
     public String verschluesslePasswort(String pPasswort) {
        String verschluesseltesPasswort="";
        for(int i=0;i<pPasswort.length();i++) {
            char buchstabe = pPasswort.charAt(i);
            int temp = ((int)buchstabe)+12;
            int neu;
            if(temp>127) {
                neu = (temp-127)+33;
            } else {
                neu = (char)temp;
            }
            verschluesseltesPasswort += (char)neu; 
        }
        return verschluesseltesPasswort;
    }
    
    public String entschluesslePasswort(String pPasswort) {
        String entschluesseltesPasswort="";
        for(int i=0;i<pPasswort.length();i++) {
            char buchstabe = pPasswort.charAt(i);
            int temp = ((int)buchstabe)-12;
            int neu;
            if(temp<33) {
                 neu = 127-(33-temp);
            } else {
                neu = (char)temp;
            }
            entschluesseltesPasswort += (char)neu; 
        }
        return entschluesseltesPasswort;
    }
    // </editor-fold>
    
    // <editor-fold  defaultstate="collapsed" desc="GetterSetter">
    public void setEnde(boolean pEnde) {
        verbindungsEnde = pEnde;
    }
    // </editor-fold>

    @Override
    public void uncaughtException(Thread t, Throwable e) {
        throw new UnsupportedOperationException(t+" has crashed: "+e); 
    }
}
