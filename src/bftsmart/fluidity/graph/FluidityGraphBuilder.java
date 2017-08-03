package bftsmart.fluidity.graph;

import bftsmart.reconfiguration.views.View;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by philipp on 25.06.17.
 */
public class FluidityGraphBuilder {
    private FluidityGraph fluidityGraph;

    public FluidityGraphBuilder(View view) {
        fluidityGraph = new FluidityGraph(view);
    }

    public FluidityGraph generateGraphFromXML(String filePath) {

        try {
            File xmlFile = new File(filePath);
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(xmlFile);
            doc.getDocumentElement().normalize();
            Element root = doc.getDocumentElement();

            NodeList nodes = doc.getElementsByTagName("node");
            for (int i = 0; i < nodes.getLength(); i++) {
                Node tempNode = nodes.item(i);
                if (tempNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element nodeElement = (Element) tempNode;
                    int datacenterId = Integer.parseInt(
                            nodeElement.getAttribute("datacenterId"));

                    int numOfReplicas = nodeElement.getElementsByTagName("replicaId").getLength();
                    ArrayList<Integer> replicaList = new ArrayList<>();

                    for (int j = 0; j < numOfReplicas; j++) {
                        int replicaId = Integer.parseInt(
                                nodeElement.getElementsByTagName("replicaId")
                                        .item(j).getTextContent());
                        replicaList.add(replicaId);
                    }

                    int maxNumOfRep = Integer.parseInt(
                            nodeElement.getElementsByTagName("maximumNumberOfReplicas")
                                    .item(0).getTextContent());

                    fluidityGraph.addNode(datacenterId, replicaList, maxNumOfRep);
                }
            }

            //TODO Make edges between all nodes default and only specify known latencies in the xml file
            NodeList edges = doc.getElementsByTagName("edge");
            for (int i = 0; i < edges.getLength(); i++) {
                Node tempEdge = edges.item(i);
                if (tempEdge.getNodeType() == Node.ELEMENT_NODE) {
                    Element edgeElement = (Element) tempEdge;
                    int nodeFromId = Integer.parseInt(edgeElement
                            .getElementsByTagName("nodeFrom")
                            .item(0).getTextContent());

                    int nodeToId = Integer.parseInt(edgeElement
                            .getElementsByTagName("nodeTo")
                            .item(0).getTextContent());

                    double latency = Double.parseDouble(edgeElement
                            .getElementsByTagName("latency")
                            .item(0).getTextContent());

                    fluidityGraph.addEdge(nodeFromId, nodeToId, latency);

                }
            }


        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (fluidityGraph.checkForConsistencyWithRules()) {
            return fluidityGraph;
        }
        return null;
    }
}
