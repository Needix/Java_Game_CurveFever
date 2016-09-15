/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package Netzwerk.Server;

import CF.CF_Player;
import GUI.*;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.Thread.UncaughtExceptionHandler;
import java.net.SocketTimeoutException;
import javax.swing.DefaultListModel;

/**
    * Der Connector ist ein Teil vom Server
    * Er wartet auf neue Verbindungen, 
    * ueberprueft welche Ports frei sind, 
    * weist der aufgebauten Verbindung einen Empfaenger zu
    * und verwaltet die drei wichtigen ALs (ArrayLists) (s. public ArrayList ...)
    * Erstellung: 
    *   1. Erstelle Server_Connector Objekt
    *   2. Objekt.starteServer(PORT)
    *   3. Objekt.start();
    * @author Jonas Oeldemann
 */
public class Server_Connector extends Thread implements UncaughtExceptionHandler {
    ////Konstanten
    //ArrayLists
    public final ArrayList <Socket>ALVerbindungen;     //ArrayList der Sockets; speichert die Sockets (die Verbindungen zu allen Clients)
    public final ArrayList <Server_Empfaenger>ALEmpfaenger;  //ArrayList der Empfaenger; speichert alle Empfaenger (die Empfaenger warten auf Nachrichten von ihren jeweiligen Verbindungen/Clients; s. Beschreibung Server_Empfaenger)
    public final ArrayList <String>ALTeilnehmer;  //ArrayList der Teilnehmer; speichert die Namen aller Teilnehmer (zum einfachem managen)
    
    //Objekte
    public final Server_Sender SSender;  //Der Sender, der Nachrichten an alle oder einzelne Clients senden kann
    private final DefaultListModel<String> dlm_spielerListe;
    private final StartGUI GUI;
    private final int anzahlSpieler;
    private final ArrayList<CF_Player> player;
    private ServerSocket Server;    //ServerSocket, auf dem das Verbindungssetup laeuft
    
    private final int port;   //Port, auf dem der Server laeuft
    
    //Variabeln
    private boolean ende;   //Wird der Server beendet? // Wichtig zum stoppen des Threads
    private boolean spielLaeuft;
    
    /**
     * Server_Connector Konstruktor
     * @param dlm DefaultListModel, welches die Liste der Teilnehmer auf der GUI anzeigt
     * @param GUI Die Oberflaeche
     * @param anzahlSpieler Die maximale Anzahl der Spieler, die teilnehmen sollen
     * @param port Der Port, auf dem der Server laufen soll
     */
    public Server_Connector(DefaultListModel<String> dlm, StartGUI GUI, int anzahlSpieler, int port, ArrayList<CF_Player> player) {
        this.anzahlSpieler = anzahlSpieler;
        this.GUI = GUI;
        this.dlm_spielerListe = dlm;
        this.port = port;
        this.SSender = new Server_Sender(this);
        this.player = player;
        ALVerbindungen = new ArrayList<>();
        ALEmpfaenger = new ArrayList<>();
        ALTeilnehmer = new ArrayList<>();
    }
    
    public boolean starteServer() {
        return starteServer(port);
    }
    
    /**
     * Startet den Server
     * @param pPort Port, auf dem Server laufen soll
     * @return true, falls Server erfolgreich gestartet werden konnte, ansonsten false
     */
    public boolean starteServer(int pPort) {
        try {
            Server = new ServerSocket(pPort);
            Server.setSoTimeout(500);
//            System.out.println("Server_Connector: Starte auf Port: "+port);
//            System.out.println("Server_Connector: Warte auf Clients...");
            return true;
        } catch(IOException e) {
            System.err.println("Server_Connector: StarteServer: Netzwerk-Fehler: "+e);
        } catch(IllegalArgumentException e) {
            System.err.println("Server_Connector: StarteServer: Unzulaessige Portnummer: "+e);
        }
        return false;
    }
    
    /**
     * Wartet immer wieder auf neue Verbindung. Falls Programm beendet wird, werden alle Empfaenger + Sockets benachrichtigt
     */
    @Override
    public void run() {
        while(!ende) {
            if(ALTeilnehmer.size()<=anzahlSpieler || !spielLaeuft) {
                if(Server==null) { starteServer(); }
                //warteAufConnection();
                verbindungAufbauen();
                try { Server.close(); Server=null; } catch (IOException ex) { System.err.println("Server_Connector: run(): "+ex);}
            } else {
                try {  Thread.sleep(500); } catch (InterruptedException ex) { System.err.println("Server_Connector: run(): "+ex); }
            }
        }
        SSender.sendeNachrichtAnClients("BeendeServer");
    }
    
    public void verbindungAufbauen() {
        Socket verbindung;
        try {
            //HandShake
            Socket lVerbindung = Server.accept();
            if(lVerbindung==null) return ;
            
            //Nutzername
            BufferedReader BR = new BufferedReader(new InputStreamReader(lVerbindung.getInputStream()));
            String nutzername = BR.readLine();
            nutzername = nutzername.substring(nutzername.indexOf(":")+1);
            for (int j = 0; j < ALTeilnehmer.size(); j++) {
                if(ALTeilnehmer.get(j).contains(nutzername)) {
                    lVerbindung.close();
                    return;
                }
            }
            
            //Richtige Verbindung + Init
            System.out.println("Server: Neue Verbindung: "+lVerbindung);
            ALVerbindungen.add(lVerbindung);
            Server_Empfaenger SEmpf = new Server_Empfaenger(SSender,this,dlm_spielerListe,player,ALVerbindungen.size(),lVerbindung,this.GUI);
            ALTeilnehmer.add(nutzername);
            ALEmpfaenger.add(SEmpf);
            SEmpf.start();
            
            dlm_spielerListe.clear();
            for (int i = 0; i < ALTeilnehmer.size(); i++) {
                dlm_spielerListe.addElement(ALTeilnehmer.get(i));
            }
            for (int i = 0; i < 8-ALTeilnehmer.size(); i++) {
                dlm_spielerListe.addElement("Unbesetzt");
            }
            GUI.erstelleServer_b_start.setEnabled(false);
            
            
            String serverEinstellungen = GUI.getServerOptions()+":"+ALTeilnehmer.size();
            SSender.sendeNachrichtAnClient(lVerbindung, "+VerbindungsAufbau_ServerEinstellungen:"+serverEinstellungen);
            String nutzernamen = "+UpdateTeilnehmerListe";
            for (int i = 0; i < ALTeilnehmer.size(); i++) { nutzernamen+=":"+ALTeilnehmer.get(i);}
            SSender.sendeNachrichtAnClients(nutzernamen);
        } catch(SocketTimeoutException ex) {
        } catch (IOException ex) { System.err.println("Server_Connector: verbindungAufbauen: IOE: "+ex); }
    }
    
    private int sucheFreienPort() {
        for(int i=port;true;i++) {
            if(i>=65535) return -1;
            try {
                new ServerSocket(i).close();
                return i;
            } catch (IOException ex) { } //Port schon belegt, suche weiter
        }
    }
    
    //<editor-fold defaultstate="collapsed" desc="GetterSetter">
    public void setEnde(boolean pEnde) {
        ende = pEnde;
    }
    
    public void setSpielLaeuft(boolean pSpielLaeuft) {
        spielLaeuft = pSpielLaeuft;
    }
    //</editor-fold>

    @Override
    public void uncaughtException(Thread t, Throwable e) {
        throw new UnsupportedOperationException(t+" has crashed: "+e); 
    }
}
