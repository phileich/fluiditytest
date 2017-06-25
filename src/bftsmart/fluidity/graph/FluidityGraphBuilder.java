package bftsmart.fluidity.graph;

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

/**
 * Created by philipp on 25.06.17.
 */
public class FluidityGraphBuilder {
    private FluidityGraph fluidityGraph;

    public FluidityGraphBuilder() {
        fluidityGraph = new FluidityGraph();
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
                            nodeElement.getAttribute("datacenterid"));

                    int maxNumOfRep = Integer.parseInt(
                            nodeElement.getElementsByTagName("maximumNumberOfElements")
                                    .item(0).getTextContent());

                    fluidityGraph.addNode(datacenterId, null, maxNumOfRep);
                }
            }

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


        return fluidityGraph;
    }
}