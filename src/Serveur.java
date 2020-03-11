import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Arrays;

public class Serveur {
    /**
     * Server socket.
     */
    private DatagramSocket dg;

    /**
     * Players.
     */
    public Joueur[] joueurs;

    /**
     * Current amount of players.
     */
    private int nbJoueur;

    /**
     * Player number's turn.
     */
    private int joueurCourant;

    /**
     * Table of tic tac toe.
     */
    private char[][] cases;

    /**
     * Creates a new tic tac toe server.
     * @param port Port on which to listen.
     */
    public Serveur(int port) {
        joueurs = new Joueur[2];
        cases = new char[3][3];
        joueurCourant = 0;

        try {
            dg = new DatagramSocket(port);
        } catch (Exception e) {
            System.out.println("Constructeur : " + e);
        }
    }

    /**
     * Starts the server and starts waiting for new players.
     */
    public void start() {
        DatagramPacket dpr;
        System.out.println("Démarrage du serveur.");

        while (true) {
            byte[] buf = new byte[4096];
            dpr = new DatagramPacket(buf, buf.length);

            try {
                dg.receive(dpr);
            } catch (IOException e) {
                e.printStackTrace();
            }

            String packet = new String(dpr.getData(), 0, dpr.getLength());
            String[] packetContent = packet.split(" ");

            String sender = packetContent[0];
            String type = packetContent[1];

            Object[] paramsObj = Arrays.stream(packetContent).skip(2).toArray();
            String[] params = new String[paramsObj.length];
            for (int i = 0; i < params.length; ++i) {
                params[i] = paramsObj[i].toString();
            }

            System.out.println("< " + packet);
            if (HandlePacket(sender, type, params, dpr)) {
                break;
            }
        }

        System.out.println("Fermeture du serveur car la partie est terminée.");
    }

    /**
     * Handles a packet received.
     * @param sender Sender id. -1 when not authenticated then 0 or 1.
     * @param type Header of the packet.
     * @param params Parameters of that packet.
     * @param dpr Raw datagram packet to get ip/port.
     * @return Returns a true boolean when the game is over.
     */
    private boolean HandlePacket(String sender, String type, String[] params, DatagramPacket dpr) {
        switch (type.toUpperCase()) {
            case "IDENTIFY":
                if (nbJoueur < 2) {
                    joueurs[nbJoueur] = new Joueur(dpr);
                    sendPacket(dpr, sender, "ACK " + nbJoueur);
                    nbJoueur++;
                } else {
                    sendPacket(dpr, sender, "TOO_MANY_PLAYERS");
                }

                if (nbJoueur == 2) {
                    sendPacket(joueurs[joueurCourant].address, joueurs[joueurCourant].port,
                            Integer.toString(joueurCourant), "TURN");
                }
                break;
            case "PLAY":
                if (joueurCourant != Integer.parseInt(sender)) {
                    sendPacket(dpr, sender, "NOT_YOUR_TURN");
                    break;
                }

                try {
                    int x = Integer.parseInt(params[0]);
                    int y = Integer.parseInt(params[1]);

                    if (x < 0 || x > 2 || y < 0 || y > 2) {
                            sendPacket(dpr, sender, "NOT_EMPTY");
                        break;
                    }

                    if (cases[x][y] != '\0') {
                        sendPacket(dpr, sender, "NOT_EMPTY");
                    } else {
                        cases[x][y] = joueurCourant == 0 ? 'X' : 'O';
                        int isVictory = isVictory();
                        if (isVictory != -1) {
                            sendPacket(joueurs[0].address, joueurs[0].port,
                                    "0", "VICTORY " + isVictory);
                            sendPacket(joueurs[1].address, joueurs[1].port,
                                    "1", "VICTORY " + isVictory);
                            return true;
                        }

                        joueurCourant = joueurCourant == 0 ? 1 : 0;

                        //sync
                        StringBuilder builder = new StringBuilder();
                        builder.append("SYNC ");
                        for (int i = 0; i < cases.length; ++i) {
                            for (int j = 0; j < cases[i].length; ++j) {
                                builder.append(cases[i][j]).append(",");
                            }
                            builder.deleteCharAt(builder.length() - 1);
                            builder.append("|");
                        }
                        builder.deleteCharAt(builder.length() - 1);

                        sendPacket(joueurs[0].address, joueurs[0].port,
                                "0", builder.toString());
                        sendPacket(joueurs[1].address, joueurs[1].port,
                                "1", builder.toString());

                        sendPacket(joueurs[joueurCourant].address,
                                joueurs[joueurCourant].port,
                                Integer.toString(joueurCourant), "TURN");

                        sendPacket(joueurs[joueurCourant == 0 ? 1 : 0].address,
                                joueurs[joueurCourant == 0 ? 1 : 0].port,
                                Integer.toString(joueurCourant == 0 ? 1 : 0), "NOT_YOUR_TURN");
                    }
                } catch (Exception e) {
                    e.printStackTrace();

                    sendPacket(joueurs[joueurCourant].address,
                            joueurs[joueurCourant].port,
                            Integer.toString(joueurCourant), "TURN");
                    sendPacket(joueurs[joueurCourant == 0 ? 1 : 0].address,
                            joueurs[joueurCourant == 0 ? 1 : 0].port,
                            Integer.toString(joueurCourant == 0 ? 1 : 0), "NOT_YOUR_TURN");
                }

                break;
            default: // unhandled packet
                System.out.println(
                        "Unhandled packet received: " + type + "(" + String.join(", ", params) + ")");
                break;
        }

        return false;
    }

    /**
     * Checks for a victory and returns the winner. -1 when no winner yet.
     */
    private int isVictory() {
        for (int i = 0; i < 3; ++i) {
            if (cases[i][0] == cases[i][1] && cases[i][1] == cases[i][2]) {
                if (cases[i][0] != '\0') {
                    return cases[i][0] == '0' ? 1 : 0;
                }
            }
        }

        for (int i = 0; i < 3; ++i) {
            if (cases[0][i] == cases[1][i] && cases[1][i] == cases[2][i]) {
                if (cases[0][i] != '\0') {
                    return cases[0][i] == '0' ? 1 : 0;
                }
            }
        }

        if (cases[0][0] == cases[1][1] && cases[1][1] == cases[2][2]) {
            if (cases[1][1] != '\0') {
                return cases[1][1] == '0' ? 1 : 0;
            }
        }

        if (cases[2][0] == cases[1][1] && cases[1][1] == cases[0][2]) {
            if (cases[1][1] != '\0') {
                return cases[1][1] == '0' ? 1 : 0;
            }
        }

        int nbEmpty = 0;
        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 3; ++j) {
                if (cases[i][j] == '\0') {
                    nbEmpty++;
                }
            }
        }

        if (nbEmpty == 0) {
            return -2;
        }

        return -1;
    }

    /**
     * Sends a packet from a datagram packet.
     * @param dpr DatagramPacket containing the ip and port.
     * @param senderId Id of the person to send the packet too. It ensures the right client receives the right packet.
     * @param content Content of the packet, including the packet header.
     */
    private void sendPacket(DatagramPacket dpr, String senderId, String content) {
        sendPacket(dpr.getAddress(), dpr.getPort(), senderId, content);
    }

    /**
     * Sends a packet.
     * @param address Address to send the packet to.
     * @param port Port to send the packet to.
     * @param senderId Id of the person to send the packet too. It ensures the right client receives the right packet.
     * @param content Content of the packet, including the packet header.
     */
    private void sendPacket(InetAddress address, int port, String senderId, String content) {
        System.out.println("> " + senderId + " " + content);
        byte[] bytes = (senderId + " " + content).getBytes();
        DatagramPacket dps = new DatagramPacket(bytes, bytes.length, address, port);

        try {
            dg.send(dps);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String... args) {
        int port = 5000;
        if (args.length == 1) {
            port = Integer.parseInt(args[0]);
        }

        Serveur serveur = new Serveur(port);
        serveur.start();
    }

    /**
     * Static player class holding address and port.
     * They are put in a table which 0 index mean player 1 and 1 index mean player 2.
     */
    private static class Joueur {
        /**
         * IP address of the player.
         */
        public InetAddress address;

        /**
         * Port of the player.
         */
        public int port;

        /**
         * Creates a new player instance from a datagram packet.
         * @param dpr Datagram packet holding IP address and port.
         */
        public Joueur(DatagramPacket dpr) {
            address = dpr.getAddress();
            port = dpr.getPort();
        }
    }
}
