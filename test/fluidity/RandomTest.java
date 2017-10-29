package fluidity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

/**
 * Created by philipp on 31.07.17.
 */
public class RandomTest {

    public static void main(String[] args) {
        RandomTest r = new RandomTest();
    }

    public RandomTest() {
        ArrayList<NodeWeights> nodeWeights = new ArrayList<>();
        nodeWeights.add(new NodeWeights(0, 1.0d));
        nodeWeights.add(new NodeWeights(1, 0.0d));
        nodeWeights.add(new NodeWeights(2, 3.0d));

        Collections.sort(nodeWeights, new Comparator<NodeWeights>() { //TODO Check if correct
            @Override
            public int compare(NodeWeights lhs, NodeWeights rhs) {
                // -1 - less than, 1 - greater than, 0 - equal, all inversed for descending
                return lhs.getWeight() > rhs.getWeight() ? 1 : (lhs.getWeight() < rhs.getWeight() ) ? -1 : 0;
            }
        });

        for (NodeWeights nW : nodeWeights) {
            System.out.println("Node Id: " + nW.getNodeId());
        }
    }

    private class NodeWeights{
        private int nodeId;
        private double weight;

        public NodeWeights(int nodeId, double weight) {
            this.nodeId = nodeId;
            this.weight = weight;
        }

        public int getNodeId() {
            return nodeId;
        }

        public double getWeight() {
            return weight;
        }
    }
}
