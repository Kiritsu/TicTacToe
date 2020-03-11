import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Arrays;

public class Client {
    /**
     * Client socket to communicate with the server.
     */
    private DatagramSocket ds;

    /**
     * Address of the remote server.
     */
    private String serverAddress;

    /**
     * Port of the remote server.
     */
    private int serverPort;

    /**
     * Reader from the console input.
     */
    private BufferedReader reader;

    /**
     * Id of the player. -1 before receiving ACK packet.
     */
    private int playerId = -1;

    /**
     * Creates a new client.
     * @param serverAddress IP address of the remote server.
     * @param serverPort Port of the remote server.
     */
    public Client(String serverAddress, int serverPort) {
        reader = new BufferedReader(new InputStreamReader(System.in));

        this.serverAddress = serverAddress;
        this.serverPort = serverPort;

        // Takes a random port because of hardcoded port.
        try {
            this.ds = new DatagramSocket(5000 + ((int) (Math.random() * 1000)));
        } catch (Exception e) {
            System.out.println("Constructeur : " + e);
        }
    }

    /**
     * Starts the client and tries to connect/identify to the server.
     */
    public void start() {
        DatagramPacket dpr;
        sendPacket("IDENTIFY", "");

        while (true) {
            byte[] buf = new byte[4096];
            dpr = new DatagramPacket(buf, buf.length);

            try {
                ds.receive(dpr);
            } catch (IOException e) {
                e.printStackTrace();
            }

            String packet = new String(dpr.getData(), 0, dpr.getLength());
            String[] packetContent = packet.split(" ");

            String type = packetContent[1];

            Object[] paramsObj = Arrays.stream(packetContent).skip(2).toArray();
            String[] params = new String[paramsObj.length];
            for (int i = 0; i < params.length; ++i) {
                params[i] = paramsObj[i].toString();
            }

            if (HandlePacket(type, params)) {
                break;
            }
        }

        System.out.println("End of the game.");
    }

    /**
     * Handles a received packet.
     * @param type Header of the packet.
     * @param params Content space separated after header of the packet.
     * @return Returns true when the game is over.
     */
    private boolean HandlePacket(String type, String[] params) {
        switch (type.toUpperCase()) {
            case "ACK":
                System.out.println("You are player " + params[0]);
                playerId = Integer.parseInt(params[0]);
                break;
            case "TURN":
                System.out.println("Your turn!");
                System.out.print(playerId == 0 ? 'X' : 'O');
                System.out.print(" > ");

                try {
                    String txt = reader.readLine();
                    sendPacket("PLAY", txt);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                System.out.flush();
                break;
            case "SYNC":
                System.out.print("\033[H\033[2J");
                System.out.flush();
                System.out.println("Joueur " + playerId + "\nSynchronizing:");

                String[] lines = params[0].split("\\|");
                System.out.println("   -------------");
                System.out.println("   | 0 | 1 | 2 |");
                System.out.println("----------------");
                int cnt = 0;
                for (String line : lines) {
                    String[] cases = line.split(",");
                    System.out.print(" " + cnt++ + " ");
                    for (String c : cases) {
                        if (c.equals("\0")) {
                            c = " ";
                        }

                        System.out.print("| " + c + " ");
                    }
                    System.out.println("|");
                    System.out.println("----------------");
                }
                break;
            case "NOT_EMPTY":
                System.out.println("This coords are not empty, try again.");
                System.out.println("Your turn!");
                System.out.print(playerId == 0 ? 'X' : 'O');
                System.out.print(" > ");

                try {
                    String txt = reader.readLine();
                    sendPacket("PLAY", txt);
                } catch (IOException e) {
                    e.printStackTrace();
                }


                System.out.flush();
                break;
            case "NOT_YOUR_TURN":
                System.out.println("It's the turn of the other player!");
                break;
            case "VICTORY":
                int winner = Integer.parseInt(params[0]);
                if (winner == -2) {
                    System.out.println("Nobody won...");
                    return true;
                }

                System.out.println(winner == playerId ? "You won!" : "You lose... :(");
                return true;
            case "TOO_MANY_PLAYERS":
                System.out.println("A game is already running... Please try again later or restart the remote server.");
                return true;
        }
        return false;
    }

    public void sendPacket(String header, String content) {
        try {
            byte[] bytes = (playerId + " " + header + " " + content).getBytes();
            DatagramPacket dps = new DatagramPacket(bytes, bytes.length, InetAddress.getByName(serverAddress), serverPort);
            ds.send(dps);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String... args) {
        Client client = new Client("localhost", 5000);
        client.start();
    }
}
