package ch.fhnw.kvan.chat.socket.client;

import ch.fhnw.kvan.chat.general.ChatRoom;
import ch.fhnw.kvan.chat.general.Participants;
import ch.fhnw.kvan.chat.gui.ClientGUI;
import ch.fhnw.kvan.chat.interfaces.IChatRoom;
import ch.fhnw.kvan.chat.utils.In;
import ch.fhnw.kvan.chat.utils.Out;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by btemperli on 24.03.15.
 */
public class Client {

    private static Logger logger;
    private static Socket socket;
    private static In in;
    private static Out out;
    private static Boolean running;
    private static ClientGUI gui;
    private static ChatRoom chatRoom;
    private static String name;


    public static void main(String args[]) {

        logger = Logger.getLogger(Client.class);

        logger.info("Start a client with " + args.length + " arguments");

        name = args[0];

        try {
            socket = new Socket("localhost", 6666);
            in = new In(socket);
            out = new Out(socket);

            // do the login, send the name to the server.
            out.println("name=" + name);

            chatRoom = ChatRoom.getInstance();

            ClientWindowListener listener = new ClientWindowListener() {
                @Override
                public void callListenerWindowClose() {
                    exitWindow();
                }
            };

            gui = new ClientGUI(chatRoom, args[0], listener);

            running = true;

            startListener();

        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void startListener() {

        new Thread() {
            public void run() {

                String input = in.readLine();

                while(running && input != null) {
                    handleInput(input);
                    input = in.readLine();
                }
            }
        }.start();
    }

    public static void handleInput(String input) {
        logger.info(input);

        String key = input.split("=")[0];
        String value = input.split("=")[1];

        if (key.equals("participants")) {
            addParticipants(value);
        } else {
            logger.error("Sorry, but the key (" + key + ") could not be handled.");
        }
    }

    public static void exitWindow() {
        System.out.println("GUI is now closed...");
        out.println("remove_name=" + name);
    }

    /**
     * Add Participants
     * - to the gui
     * - to the client's chatroom
     * @param value String, comes directly from the server
     */
    private static void addParticipants(String value) {
        try {
            String[] names = value.split(";");
            List<String> namesList = new ArrayList<String>(Arrays.asList(names));
            namesList.remove(name); // remove own name from list.

            String participants = chatRoom.getParticipants();

            if (!participants.equals("participants=")) {

                // there are participants yet. remove the old from the new list
                String[] oldNames = participants.split("=")[1].split(";");

                if (oldNames.length < names.length) {
                    for (String oldName : oldNames) {
                        namesList.remove(oldName);
                    }

                    // now add the new ones.
                    for (String name : namesList) {
                        gui.addParticipant(name);
                        chatRoom.addParticipant(name);
                    }
                } else {
                    for (String oldName : oldNames) {
                        if (!oldName.equals(name) && namesList.indexOf(oldName) == -1) {
                            gui.removeParticipant(oldName);
                            chatRoom.removeParticipant(oldName);
                        }
                    }
                }
            } else {

                // there are no participants. add all.
                for (String name : namesList) {
                    gui.addParticipant(name);
                    chatRoom.addParticipant(name);
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}