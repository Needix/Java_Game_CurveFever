/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package Netzwerk.Client;

import GUI.StartGUI;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import javax.swing.DefaultListModel;

/**
 *
 * @author Need
 */
public class Client_ConnectorSender {
    private int port;
    private String ip;
    public String nutzername;
    public final ArrayList<String> teilnehmerNamen;
    public StartGUI GUI;
    private Socket verbindung;
    private final DefaultListModel<String> dlm;
    private PrintWriter Output = null;  //1. HandShakeClient -> Client_Socket
    
    public Client_ConnectorSender(DefaultListModel<String> dlm) {
        this.dlm = dlm;
        this.teilnehmerNamen = new ArrayList<>();
    }
    
    public boolean stelleVerbindungHer(String ip, int pPort, String nutzername, StartGUI GUI) {
        try {
            this.GUI = GUI;
            this.port = pPort;
            this.ip = ip;
            this.nutzername = nutzername;
            Socket lVerbindung = new Socket(ip,pPort);
            new PrintWriter(lVerbindung.getOutputStream(),true).println("Nutzername:"+nutzername);
            new Client_Empfaenger(this.GUI, this, lVerbindung, dlm).start();
            System.out.println("Client: Verbindung: "+lVerbindung);
            try { Output = new PrintWriter(lVerbindung.getOutputStream(),true); } catch (IOException ex) {System.err.println("Client_Sender: stelleVerbindungHer: "+ex); }
            verbindung = lVerbindung;
            
            return true;
        } catch (IOException ex) {
            System.err.println("Client_Connector: stelleVerbindungHer: "+ex);
        } catch(NullPointerException ex) {
            System.err.println("Client_Connector: stelleVErbindungHer: Server hat abgelehnt: "+ex);
        }
        this.GUI.showErrorDialog("Verbindungsfehler","Nutzername wird schon benutzt oder ein Verbindungsfehler ist aufgetreten. Bitte einen anderen Nutzernamen waehlen und erneut versuchen!", this.GUI.frame_serverBeitreten);
        return false;
    }
    
    public void sendeNachricht(String pNachricht) {
        if(verbindung!=null && Output!=null) {
            //System.out.println("Client: Sende Nachricht: "+pNachricht);
            Output.println(pNachricht);
        } else {
            System.err.println("Client_Connector: sendeNachricht: Konnte Nachricht nicht senden: "+verbindung+":"+Output);
        }
    }
}
