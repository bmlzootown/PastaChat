package me.bmlzootown;

import org.spacehq.mc.auth.exception.AuthenticationException;
import org.spacehq.mc.protocol.MinecraftProtocol;
import org.spacehq.mc.protocol.ProtocolConstants;
import org.spacehq.mc.protocol.ProtocolMode;
import org.spacehq.mc.protocol.data.message.*;
import org.spacehq.mc.protocol.data.status.ServerStatusInfo;
import org.spacehq.mc.protocol.data.status.handler.ServerInfoHandler;
import org.spacehq.mc.protocol.data.status.handler.ServerPingTimeHandler;
import org.spacehq.mc.protocol.packet.ingame.client.ClientChatPacket;
import org.spacehq.mc.protocol.packet.ingame.server.ServerChatPacket;
import org.spacehq.mc.protocol.packet.ingame.server.ServerJoinGamePacket;
import org.spacehq.packetlib.Client;
import org.spacehq.packetlib.Session;
import org.spacehq.packetlib.event.session.*;
import org.spacehq.packetlib.tcp.TcpSessionFactory;
import java.io.Console;
import java.net.Proxy;
import java.util.Arrays;
import java.util.Scanner;


public class Main {

    private static final boolean VERIFY_USERS = true;
    private static String HOST;
    private static int PORT;
    private static final Proxy PROXY = Proxy.NO_PROXY;
    private static String USERNAME;
    private static String PASSWORD;
    private static final boolean LOOP = true;

    public static void main(String[] args) {

        System.out.println("\b\b\b");
        Scanner in = new Scanner(System.in);
        System.out.println("Username: ");
        USERNAME = in.nextLine();
        System.out.println("Password: (Hidden for security, of course)");
        Console console = System.console();
        char[] passString = console.readPassword();
        PASSWORD = new String(passString );
        System.out.println("Server: ");
        HOST = in.nextLine();
        System.out.println("Port: ");
        PORT = in.nextInt();

        System.out.println(" ");

        status();
        login();

    }

    private static void status() {
        MinecraftProtocol protocol = new MinecraftProtocol(ProtocolMode.STATUS);
        Client client = new Client(HOST, PORT, protocol, new TcpSessionFactory(PROXY));
        client.getSession().setFlag(ProtocolConstants.SERVER_INFO_HANDLER_KEY, new ServerInfoHandler() {
            @Override
            public void handle(Session session, ServerStatusInfo info) {
                System.out.println("Version: " + info.getVersionInfo().getVersionName() + ", Protocol " + info.getVersionInfo().getProtocolVersion());
                System.out.println("Player Count: " + info.getPlayerInfo().getOnlinePlayers() + " / " + info.getPlayerInfo().getMaxPlayers());
                System.out.println("Players: " + Arrays.toString(info.getPlayerInfo().getPlayers()));
                System.out.println("Description: " + info.getDescription().getFullText());
            }
        });

        client.getSession().setFlag(ProtocolConstants.SERVER_PING_TIME_HANDLER_KEY, new ServerPingTimeHandler() {
            @Override
            public void handle(Session session, long pingTime) {
                System.out.println("Server ping took " + pingTime + "ms");
            }
        });

        client.getSession().connect();
        while(client.getSession().isConnected()) {
            try {
                Thread.sleep(5);
            } catch(InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private static void login() {
        MinecraftProtocol protocol = null;
        if(VERIFY_USERS) {
            try {
                protocol = new MinecraftProtocol(USERNAME, PASSWORD, false);
                System.out.println("Successfully authenticated user.");
            } catch(AuthenticationException e) {
                e.printStackTrace();
                return;
            }
        } else {
            protocol = new MinecraftProtocol(USERNAME);
        }

        Client client = new Client(HOST, PORT, protocol, new TcpSessionFactory(PROXY));
        client.getSession().addListener(new SessionAdapter() {
            @Override
            public void packetReceived(PacketReceivedEvent event) {
                if(event.getPacket() instanceof ServerJoinGamePacket) {
                    event.getSession().send(new ClientChatPacket("Connected with PastaChat.")); //Connect message
                } else if(event.getPacket() instanceof ServerChatPacket) {
                    Message message = event.<ServerChatPacket>getPacket().getMessage();

                    if(message instanceof TranslationMessage) {
                        System.out.println(Arrays.toString(((TranslationMessage) message).getTranslationParams())); //command response (including /tell's)
                    } else {
                        System.out.println(message.getFullText()); //received message
                    }

                    //event.getSession().disconnect("Finished");
                }

            }

            @Override
            public void packetSent(PacketSentEvent event) {
                Scanner in = new Scanner(System.in);
                if(event.getPacket() instanceof  ServerChatPacket) {
                    event.getSession().send((new ClientChatPacket(in.nextLine())));
                }
            }

            @Override
            public void disconnected(DisconnectedEvent event) {
                System.out.println("Disconnected: " + Message.fromString(event.getReason()).getFullText());
                login();
            }
        });

        client.getSession().connect();

        while(LOOP) {
            Scanner input = new Scanner(System.in);
            try {
                String stuff = input.nextLine();
                if (stuff == null || stuff.trim().length() == 0) {
                    continue;
                }
                client.getSession().send(new ClientChatPacket(stuff));
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

    }

}