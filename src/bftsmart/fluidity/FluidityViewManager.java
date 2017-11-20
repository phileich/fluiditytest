package bftsmart.fluidity;

import bftsmart.communication.server.ServerConnection;
import bftsmart.fluidity.cloudconnection.InternalServiceProxy;
import bftsmart.fluidity.graph.FluidityGraph;
import bftsmart.fluidity.graph.FluidityGraphNode;
import bftsmart.reconfiguration.*;
import bftsmart.reconfiguration.views.View;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.commons.math3.genetics.OnePointCrossover;
import org.opennebula.client.Client;
import org.opennebula.client.ClientConfigurationException;
import org.opennebula.client.vm.VirtualMachinePool;

import java.io.*;
import java.net.URI;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FluidityViewManager {

    private int id;
    private Reconfiguration rec = null;
    //private Hashtable<Integer, ServerConnection> connections = new Hashtable<Integer, ServerConnection>();
    private ServerViewController controller;
    //Need only inform those that are entering the systems, as those already
    //in the system will execute the reconfiguration request
    private List<Integer> addIds = new LinkedList<Integer>();
    private FluidityGraph currentfluidityGraph = null;
    private FluidityGraph newFluidityGraph = null;
    private Map<Integer, Double> currentWeights = null;
    private Map<Integer, Double> newWeights = null;
    // Oldrep replaced by newrep
    private Map<Integer, Integer> substitudeReplicas = null;
    private int proxyId;
    private Client oneClient;

    private static final String ONE_USERNAME = "oneadmin";
    private static final String ONE_PASSWORD = "opennebula";
    private static final String URI_ADDRESS = "http://132.231.173.228:2633/RPC2";

    private static final  String VM_TEMPLATE =
            "NAME     = ttylinux    CPU = 0.1    MEMORY = 64\n";
    // + "DISK     = [\n"
    // + "\tsource   = \"/home/user/vmachines/ttylinux/ttylinux.img\",\n"
    // + "\ttarget   = \"hda\",\n"
    // + "\treadonly = \"no\" ]\n"
    // + "FEATURES = [ acpi=\"no\" ]";

    public FluidityViewManager(int proxyId) {
        this("", proxyId);
    }

    public FluidityViewManager(String configHome, int proxyId) {
        this.id = loadID(configHome);
        this.controller = new ServerViewController(id, configHome);
        this.rec = new Reconfiguration(id);
        this.proxyId = proxyId;

        start();
    }

    public void start() {
        InternalServiceProxy internalClient = new InternalServiceProxy(id + 100);
        currentfluidityGraph = internalClient.getViewManager().getCurrentView().getFluidityGraph();
        currentWeights = internalClient.getViewManager().getCurrentView().getWeights();
        byte[] reply = null;

        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream(4);
            DataOutputStream dos = new DataOutputStream(out);


            // Execute consensus over FluidityGraph
            //byte[] serializedFluidityGraph = SerializationUtils.serialize(currentfluidityGraph);
            byte[] serializedFluidityGraph = (new String("FluidityGraph")).getBytes();
            dos.writeInt(serializedFluidityGraph.length);
            dos.write(serializedFluidityGraph);

            reply = internalClient.invokeInternal(out.toByteArray());
            if (reply != null) {
                //Logger.println("Received Internal Consensus: " + new String(reply));
                //TODO For all replicas get enough correct graphs before proceeding
                newFluidityGraph = SerializationUtils.deserialize(reply);
                System.out.println("Oldfl: " + currentfluidityGraph.toString());
                System.out.println("--------------------------------");
                System.out.println("replyfl: " + newFluidityGraph.toString());

            } else {
                bftsmart.tom.util.Logger.println("Received Internal Consensus: NULL");
            }

            // Execute consensus over weights
            byte[] serializedWeights = (new String("Weights")).getBytes();
            dos.writeInt(serializedWeights.length);
            dos.write(serializedWeights);

            reply = internalClient.invokeInternal(out.toByteArray());
            if (reply != null) {
                //Logger.println("Received Internal Consensus: " + new String(reply));
                //TODO For all replicas get enough correct graphs before proceeding
                newWeights = SerializationUtils.deserialize(reply);
                System.out.println("OldWeights: " + currentWeights.toString());
                System.out.println("--------------------------------");
                System.out.println("replyWeights: " + newWeights.toString());

            } else {
                bftsmart.tom.util.Logger.println("Received Internal Consensus: NULL");
            }

            ArrayList<Integer> idsOfNewReplicas = new ArrayList<>();
            ArrayList<Integer> idsOfRemovedReplicas = new ArrayList<>();
            //FluidityViewManager.main(null);
            //TODO Extend view manager to change currentWeights and fluidity graph
            // compare nodes and check for differences (relevant for cloud connection)
            for (FluidityGraphNode newNode : newFluidityGraph.getNodes()) {
                FluidityGraphNode oldNode = currentfluidityGraph.getNodeById(newNode.getNodeId());
                ArrayList<Integer> oldReplicaIds = currentfluidityGraph.getReplicasFromNode(oldNode);
                ArrayList<Integer> newReplicaIds = newFluidityGraph.getReplicasFromNode(newNode);

                for (int repId : newReplicaIds) {
                    if (!oldReplicaIds.contains(repId)) {
                        // new replica created
                        idsOfNewReplicas.add(repId);
                    }
                }
                //System.out.println("Ids of New: " + idsOfNewReplicas.get(0));

                for (int repId : oldReplicaIds) {
                    if (!newReplicaIds.contains(repId)) {
                        // old replica deleted
                        idsOfRemovedReplicas.add(repId);
                    }
                }

                //System.out.println("Ids of Removed: " + idsOfRemovedReplicas.get(0));
            }

            adoptWeights(idsOfNewReplicas, idsOfRemovedReplicas);

            //TODO Extract commands to remove servers and add new ones later


            updateFluidityGraph(newFluidityGraph);

            executeUpdates();
            //TODO First remove old replicas, then update weights and graph and finally start new instances and
            // add the new replicas to the view

            updateWeights(currentWeights);

            executeUpdates();

            removeServer(idsOfRemovedReplicas.get(0));

            executeUpdates();

            int port = (idsOfNewReplicas.get(0) * 10) + 11000;
            addServer(idsOfNewReplicas.get(0), "127.0.0.1", port);

            executeUpdates();




            //TODO Extend this client for giving cloud provider commands

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            internalClient.close();
        }
    }

    private void adoptWeights(ArrayList<Integer> newReplicas, ArrayList<Integer> oldReplicas) {
        substitudeReplicas = new HashMap<>();

        for (int i = 0; i < newReplicas.size(); i++) {
            int newRep = newReplicas.get(i);
            int replaceRep = oldReplicas.get(i);
            newWeights.remove(replaceRep);
            newWeights.put(newRep, 0.0d);
            substitudeReplicas.put(replaceRep, newRep);
        }
    }

    private void connectToCloud() {
        try {
            String credentials = ONE_USERNAME + ":" + ONE_PASSWORD;
            oneClient = new Client(credentials, URI.create(URI_ADDRESS).toString());

            VirtualMachinePool pool = new VirtualMachinePool(oneClient);

        } catch (ClientConfigurationException e) {
            e.printStackTrace();
        }
    }

    public void connect(){
        this.rec.connect();
    }

    private int loadID(String configHome) {
        try {
            String path = "";
            String sep = System.getProperty("file.separator");
            if (configHome == null || configHome.equals("")) {
                path = "config" + sep + "system.config";
            } else {
                path = configHome + sep + "system.config";
            }
            FileReader fr = new FileReader(path);
            BufferedReader rd = new BufferedReader(fr);
            String line = null;
            while ((line = rd.readLine()) != null) {
                if (!line.startsWith("#")) {
                    StringTokenizer str = new StringTokenizer(line, "=");
                    if (str.countTokens() > 1
                            && str.nextToken().trim().equals("system.ttp.id")) {
                        fr.close();
                        rd.close();
                        return Integer.parseInt(str.nextToken().trim());
                    }
                }
            }
            fr.close();
            rd.close();
            return -1;
        } catch (Exception e) {
            e.printStackTrace(System.out);
            return -1;
        }
    }

    public void addServer(int id, String ip, int port) {
        this.controller.getStaticConf().addHostInfo(id, ip, port);
        rec.addServer(id, ip, port);
        addIds.add(id);
    }

    public void removeServer(int id) {
        rec.removeServer(id);
    }

    public void setF(int f) {
        rec.setF(f);
    }

    public void reassignWeights(Map<Integer, Double> weightAssignment) {

    }

    public void updateFluidityGraph(FluidityGraph fluidityGraph)  {
        rec.updateFluidityGraph(fluidityGraph);
    }

    public void updateWeights(Map<Integer, Double> weights) {
        rec.updateVotingWeights(weights);
    }

    public void executeUpdates() {
        connect();
        ReconfigureReply r = rec.execute();
        View v = r.getView();
        System.out.println("New view f: " + v.getF());

        VMMessage msg = new VMMessage(id, r);

        if (addIds.size() > 0) {
            sendResponse(addIds.toArray(new Integer[1]), msg);
            addIds.clear();
        }


    }

    private ServerConnection getConnection(int remoteId) {
        return new ServerConnection(controller, null, remoteId, null, null);
    }

    public void sendResponse(Integer[] targets, VMMessage sm) {
        ByteArrayOutputStream bOut = new ByteArrayOutputStream();

        try {
            new ObjectOutputStream(bOut).writeObject(sm);
        } catch (IOException ex) {
            Logger.getLogger(ServerConnection.class.getName()).log(Level.SEVERE, null, ex);
        }

        byte[] data = bOut.toByteArray();

        for (Integer i : targets) {
            //br.ufsc.das.tom.util.Logger.println("(ServersCommunicationLayer.send) Sending msg to replica "+i);
            try {
                if (i.intValue() != id) {
                    getConnection(i.intValue()).send(data, true);
                }
            } catch (InterruptedException ex) {
                // ex.printStackTrace();
                System.err.println(ex);
            }
        }
        //br.ufsc.das.tom.util.Logger.println("(ServersCommunicationLayer.send) Finished sending messages to replicas");
    }

    public void close() {
        rec.close();
    }



    public static void main(String[] args) {

        FluidityViewManager viewManager = null;

//        if (args.length > 0) {
//            viewManager = new FluidityViewManager(args[0]);
//        } else {
//            viewManager = new FluidityViewManager("");
//        }

        if (args.length > 1) {
            viewManager = new FluidityViewManager(args[0], Integer.parseInt(args[1]));
        }

        /* Scanner scan = new Scanner(System.in);
        String str = null;
        do {
            str = scan.nextLine();
            String cmd = "";
            int arg = -1;
            try {
                StringTokenizer token = new StringTokenizer(str);
                cmd = token.nextToken();
                arg = Integer.parseInt(token.nextToken());
            } catch (Exception e) {
            }

            if (arg >= 0) {
                if (cmd.equals("add")) {

                    int port = (arg * 10) + 11000;
                    viewManager.addServer(arg, "127.0.0.1", port);
                } else if (cmd.equals("rem")) {
                    viewManager.removeServer(arg);
                }

                viewManager.executeUpdates();
            }

        } while (!str.equals("exit")); */
        //viewManager.close();
        //System.exit(0);
    }

    private void calculatePositionChanges(FluidityGraph oldGraph, FluidityGraph newGraph) {

    }
}
