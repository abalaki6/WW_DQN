import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Main class to run training simulator with wumpus world
 * This class is called internally in C code wrapper to run model in python
 * and environment in Java.
 * If specified infinity number of trial (or any other),
 * to stop and save the model send SIGINT via Ctrl+C in terminal 
 * TODO: implement signal handler
 * @author Artsion Balakir
 * @since 05/26/2018
 */
public class SimulatorApplication {

    private static boolean run = true;            // status of program, changed by catching SIGINT

    private final int totalWorkers;               // total number of parallel simulators
    private int joinedCounter;                    // number completed simulations' steps
    private WorldApplication[] pool;              // pool with all simulators, includes handler

    private Lock sharedMtx;                       // mutex shared between all workers
    private Condition sharedCV;                   // cv shared between workers
    private NetworkConnector networkConnector;    // wrapper to communicate with model
    private int[] actionsReference;               // array with all actions to given (s,r,t)

    public Condition cv;                          // cv to wake main thread to update actions


    public static void main(String[] args) {
        // TODO: read command line arguments
        int numberWorkers = 5;
        int port = 2727;
        
        // add hook for SIGINT
        Runtime.getRuntime().addShutdownHook(new Thread()
        {
            @Override
            public void run()
            {
                SimulatorApplication.signalHandler();
            }
        });

        // run simulators
        SimulatorApplication sa = new SimulatorApplication(numberWorkers, port);
        sa.interact();
    }

    /**
     * Creates pool of simulators with comminicater to model
     * @param numberWorkers total number of parallel simulators
     * @param port port to connect to communicate with model
     */
    public SimulatorApplication(int numberWorkers, int port){
        this.totalWorkers = numberWorkers;
        this.joinedCounter = 0;
        this.sharedMtx = new ReentrantLock();
        this.sharedCV = this.sharedMtx.newCondition();
        this.actionsReference = new int[this.totalWorkers];
        this.networkConnector = new NetworkConnector(port, this.totalWorkers);
        this.pool = createPool(this.totalWorkers);
    }

    /**
     * Runs circular loop observe->act->observe as specified in command line 
     */
    public void interact(){
        try{
            while(SimulatorApplication.run){
                // wait while workers are working
                this.sharedMtx.lock();
                while(!this.allJoined()){
                    this.cv.awaitUninterruptibly();
                }
                // now send info and receive actions
                this.networkConnector.getActions(this.actionsReference);
                this.joinedCounter = 0;
                // notify all workers
                this.sharedCV.signalAll();
                this.sharedMtx.unlock();
            }
            // catched signall, close connection and exit
            this.networkConnector.close();
        }catch(Exception e){
            e.printStackTrace();
        }
    }
    

    /**
     * Creates pool of simulators that includes simulator handler wrapper to communicate.
     * All communications with main thread are done internally
     * @param size the size of pool
     * @return WorldApplication[]: pool of simulators
     */
    private WorldApplication[] createPool(int size){
        WorldApplication[] pool = new WorldApplication[size];
        for(int i = 0; i < size; i++){
            pool[i] = createSimulator(i);
        }
        return pool;
    }

    /**
     * Creates single simulator with specified internal id
     * @param id simulator id, uses internally
     * @return WorldApplication: simulator with wrapper
     */
    private WorldApplication createSimulator(int id){
        SimulatorHandler simulatorHandler = new SimulatorHandler(this, id, this.networkConnector, this.sharedMtx, this.sharedCV, this.actionsReference);
        // TODO: more general simulator; threading
        return new WorldApplication(simulatorHandler);
    }

    /**
     * Signal handler, changes program status to stop
     */
    public static void signalHandler(){
        SimulatorApplication.run = false;
    }

    /**
     * increment number of completed tasks
     */
    public void increment(){
        joinedCounter++;
    }

    /**
     * This method is used as flag for conditional variables to wake up main thread or all workers
     * @return true if all workers have finished the task, otherwise false.
     */
    public boolean allJoined(){
        return joinedCounter == totalWorkers;  
    }
}