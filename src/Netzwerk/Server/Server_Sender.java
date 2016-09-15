/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package Netzwerk.Server;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;

/**
 * Sender, der alle Nachrichten an Clients sendet kann
 * @author Jonas Oeldemann
 */
public class Server_Sender {
    private final Server_Connector Connector; 
    
    /**
     * Server_Sender Konstruktor
     * @param Connector Connector, der die Verbindung managt (ALs von Connector werden benoetigt)
     */
    public Server_Sender(Server_Connector Connector) {
        this.Connector = Connector;
    }
    
    //<editor-fold defaultstate="collapsed" desc="Sende Nachricht">
    /**
     * Sende "pNachricht" an alle Clients
     * @param pNachricht Die Nachricht die gesendet werden soll
     */
    public void sendeNachrichtAnClients(String pNachricht) {
        for(int i=0;i<Connector.ALVerbindungen.size();i++) {
            sendeNachrichtAnClient(Connector.ALVerbindungen.get(i),pNachricht);
        }
    }
    
    /**
     * Sendet "pNachricht" an einen bestimmten Client "pClient"
     * @param pClient Der Client, an den die Nachricht geschickt werden soll
     * @param pNachricht Die Nachricht, die geschickt werden soll
     */
    public void sendeNachrichtAnClient(Socket pClient, String pNachricht) {
        PrintWriter Output;
        try {
            Output = new PrintWriter(pClient.getOutputStream(),true);
            //System.out.println("Server: Sende Nachricht: "+pNachricht);
            Output.println(pNachricht);
        } catch (IOException ex) {
            System.err.println("Server_Sender: InputOutputExeption @sendeNachrichtAnClient: "+ex);
        } catch (NullPointerException ex) {
            System.err.println("Server_Sender: NullPointer @sendeNachrichtAnClient: "+ex);
        }
    }
    //</editor-fold>
}
