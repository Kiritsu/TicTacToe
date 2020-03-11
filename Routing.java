import org.graphstream.algorithm.Dijkstra;
import org.graphstream.graph.Edge;
import org.graphstream.graph.Graph;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.Graphs;
import org.graphstream.graph.implementations.SingleGraph;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class Routing {
    public Graph graph;
    private BufferedReader reader;

    public Routing() {
        this.graph = new SingleGraph("Graph");
        this.reader = new BufferedReader(new InputStreamReader(System.in));
    }

    /**
     * Ajoute des nouvelles nodes au graphe.
     */
    public void addNodes() throws IOException {
        String lastCommand;
        do {
            System.out.println("=== TP4 Réseau | Allan Mercou ~ Léon Souffes | Routage ===");
            System.out.println("Entrez le nom de la node à ajouter. Pour arrêter, appuyez sur entrée sans rien entrer.");
            lastCommand = this.reader.readLine();
            this.graph.addNode(lastCommand);
        } while (!lastCommand.equals(""));
    }

    /**
     * Ajoute un nouveau lien entre deux nodes et le poids de la distance entre elles.
     */
    public void addEdge() throws IOException {
        String first, second;
        String poids;
        System.out.println("=== TP4 Réseau | Allan Mercou ~ Léon Souffes | Routage ===");
        System.out.println("Entrez le nom de la première node.");
        first = this.reader.readLine();
        System.out.println("Entrez le nom de la seconde node.");
        second = this.reader.readLine();
        System.out.println("Entrez le poids de la distance entre ces deux nodes.");
        poids = this.reader.readLine();
        this.graph.addEdge(first + second, first, second).addAttribute("length", Integer.parseInt(poids));
    }

    /**
     * Ajoute visuellement le nom de chaque node et le poids de chaque distance entre chaque node.
     */
    public void prepareGraph() {
        for (Node node : graph.getNodeSet()) {
            node.addAttribute("label", node.getId());
        }

        for (Edge edge : graph.getEachEdge()) {
            edge.addAttribute("label", "" + (int) edge.getNumber("length"));
        }
    }

    /**
     * Retourne un string de la table de routage de la node courante et de ses voisines.
     * Doit être appelé avant "getRouteNeighborDijkstraFor(Node)"
     *
     * @param node Node courante.
     */
    public StringBuilder getRouteNodeNeighborsFor(Node node) {
        StringBuilder builder = new StringBuilder();
        builder.append(node).append(" | ");
        for (Iterator<Node> it = node.getNeighborNodeIterator(); it.hasNext(); ) {
            Node neighbor = it.next();
            builder.append(neighbor).append(", ");
        }
        builder.append("\n").append("_".repeat(20)).append("\n");
        return builder;
    }

    /**
     * Trie et retourne sous forme de chaine de caractères dans l'ordre du plus court chemin au plus long,
     * les nodes voisines à la node spécifiée en paramètres pour chaque nodes accessibles du graphe.
     *
     * @param node Node courante.
     */
    public StringBuilder getRouteNeighborDijkstraFor(Node node) {
        StringBuilder builder = new StringBuilder();

        // On retire la node actuelle pour pas retourner sur notre pas lors du calcul du plus court chemin.
        Graph copy = Graphs.clone(this.graph);
        copy.removeNode(node.toString());

        Dijkstra dijkstra = new Dijkstra(Dijkstra.Element.EDGE, null, "length");
        dijkstra.init(copy);
        HashMap<Node, HashMap<Node, Integer>> dijkstraDistance = new HashMap<>();
        for (Node copyNode : copy.getNodeSet()) {
            dijkstraDistance.put(copyNode, new HashMap<>());
        }

        for (Iterator<Node> it = node.getNeighborNodeIterator(); it.hasNext(); ) {
            Node neighbor = it.next();

            Edge edge = graph.getEdge(node.toString() + neighbor.toString());
            if (edge == null) {
                edge = graph.getEdge(neighbor.toString() + node.toString());
            }

            neighbor = copy.getNode(neighbor.toString());
            dijkstra.setSource(neighbor);
            dijkstra.compute();

            for (Node neighborCopy : copy) {
                int poids = (int) edge.getAttribute("length") + (int) dijkstra.getPathLength(neighborCopy);
                dijkstraDistance.get(neighborCopy).put(neighbor, poids);
            }
        }

        ArrayList<String> nodeNames = new ArrayList<>();
        dijkstraDistance.forEach((k, v) -> nodeNames.add(k.toString()));
        Collections.sort(nodeNames); // trié en ordre alphabétique

        for (String s : nodeNames) {
            builder.append(s).append(" | ");

            HashMap<Node, Integer> map = sortByValues(dijkstraDistance.get(copy.getNode(s)));
            Map.Entry<Node, Integer>[] entries = map.entrySet().toArray(new Map.Entry[0]);

            for (int i = 0; i < entries.length; ++i) {
                Map.Entry<Node, Integer> now = entries[i];
                Map.Entry<Node, Integer> previous = null;
                Map.Entry<Node, Integer> next = null;

                if (i > 0) {
                    previous = entries[i - 1];
                }
                if (i < entries.length - 1) {
                    next = entries[i + 1];
                }

                if ((previous == null || !previous.getValue().equals(now.getValue())) && next != null && now.getValue().equals(next.getValue())) {
                    builder.append("(");
                }
                builder.append(now.getKey());
                if (previous != null && previous.getValue().equals(now.getValue()) && (next == null || !next.getValue().equals(now.getValue()))) {
                    builder.append(")");
                }

                if (next != null) {
                    builder.append(",");
                }
            }

            builder.append("\n");
        }

        return builder;
    }

    public void getRoutes() {
        for (Node node : this.graph) {
            StringBuilder builder = getRouteNodeNeighborsFor(node);
            builder.append(getRouteNeighborDijkstraFor(node));
            builder.append("\n\n");
            System.out.println(builder);
        }
    }

    public static void main(String[] args) {
        Routing routing = new Routing();

        routing.graph.addNode("C1");
        routing.graph.addNode("C2");
        routing.graph.addNode("C3");
        routing.graph.addNode("C4");
        routing.graph.addNode("C5");
        routing.graph.addNode("C6");
        routing.graph.addEdge("C1C2", "C1", "C2").addAttribute("length",2);
        routing.graph.addEdge("C1C5", "C1", "C5").addAttribute("length",2);
        routing.graph.addEdge("C1C6", "C1", "C6").addAttribute("length",4);
        routing.graph.addEdge("C2C3", "C2", "C3").addAttribute("length",1);
        routing.graph.addEdge("C3C4", "C3", "C4").addAttribute("length",1);
        routing.graph.addEdge("C3C5", "C3", "C5").addAttribute("length",3);
        routing.graph.addEdge("C4C6", "C4", "C6").addAttribute("length",1);
        routing.graph.addEdge("C5C6", "C5", "C6").addAttribute("length",1);

        routing.prepareGraph();

        routing.getRoutes();
    }

    private static HashMap sortByValues(HashMap map) {
        List list = new LinkedList(map.entrySet());
        // Defined Custom Comparator here
        Collections.sort(list, new Comparator() {
            public int compare(Object o1, Object o2) {
                return ((Comparable) ((Map.Entry) (o1)).getValue())
                        .compareTo(((Map.Entry) (o2)).getValue());
            }
        });

        // Here I am copying the sorted list in HashMap
        // using LinkedHashMap to preserve the insertion order
        HashMap sortedHashMap = new LinkedHashMap();
        for (Iterator it = list.iterator(); it.hasNext(); ) {
            Map.Entry entry = (Map.Entry) it.next();
            sortedHashMap.put(entry.getKey(), entry.getValue());
        }
               
        return sortedHashMap;
    }
}
