package fluidity;

import bftsmart.demo.counter.CounterClient;
import bftsmart.demo.counter.CounterServer;

import java.io.IOException;

/**
 * Created by philipp on 22.05.17.
 */
public class CounterTest {

    public static void main(String[] args) {
        int[] serverIds = {0,1,2,3,4,5};
        int[] clientIds = {0,1};

        for (int i: serverIds) {
            String[] arguments = {String.valueOf(i)};
            CounterServer.main(arguments);
        }

//        for (int i: clientIds) {
//            String[] param = {String.valueOf(i) , "1"};
//            try {
//                CounterClient.main(param);
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }



    }
}
