package bftsmart.fluidity;

import bftsmart.communication.server.ServerConnection;
import bftsmart.fluidity.graph.FluidityGraph;
import bftsmart.reconfiguration.*;
import bftsmart.reconfiguration.views.View;

import java.io.*;
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

    public FluidityViewManager() {
        this("");
    }

    public FluidityViewManager(String configHome) {
        this.id = loadID(configHome);
        this.controller = new ServerViewController(id, configHome);
        this.rec = new Reconfiguration(id);
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

        if (args.length > 0) {
            viewManager = new FluidityViewManager(args[0]);
        } else {
            viewManager = new FluidityViewManager("");
        }

        Scanner scan = new Scanner(System.in);
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

        } while (!str.equals("exit"));
        viewManager.close();
        System.exit(0);
    }
}
