import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

/**
 * SimulatorHandler. Wrapper to collect and send data about the simulator.
 * Also receives and send to simulator new action.
 * TODO: complete class; debug; add documentation
 */
public class SimulatorHandler {

    private int id;                                // personal id of simulator's handler
    private int state;                             // int representation of state of the simulator
    private int reward;                            // reward for the last action
    private boolean terminal;                      // is current state terminal
    private int[] actionsReference;                // reference to array of received actions for each simulator

    private Lock mtx;                              // mutex, to update completed workers counter
    private Condition cv;                          // conditional variables to wake handler when actions arrived
    private NetworkConnector networkConnector;     // wrapper to update (s,r,t) for future package
    private SimulatorApplication self;             // applications that holds cv to inform about completeness

    
    public SimulatorHandler(SimulatorApplication self, int simulator_id, NetworkConnector networkConnector, Lock mtx, Condition cv, int[] actionsReference){
        this.self = self;
        this.id = simulator_id;
        this.networkConnector = networkConnector;
        this.mtx = mtx;
        this.cv = cv;
        this.actionsReference = actionsReference;
    }

    public void updateReward(int reward){
        this.reward = reward;
    }

    public void updateTerminal(boolean terminal){
        this.terminal = terminal;
    }

    public int process(TransferPercept tp){
        this.state = 0;
        this.state += (tp.getBreeze() ? 1 : 0) << 0;
        this.state += (tp.getBump() ? 1 : 0) << 1;
        this.state += (tp.getGlitter() ? 1 : 0) << 2;
        this.state += (tp.getStench() ? 1 : 0) << 3;
        this.state += (tp.getScream() ? 1 : 0) << 4;
        // TODO: add number of arrows
        requiestAction();
        return this.actionsReference[id];
    }

    private void requiestAction(){
        this.mtx.lock();
        try{
            // update data
            networkConnector.setSRT(this.state, this.reward, this.terminal, this.id);
            // increment number of joined workers
            self.increment();
            // if all joined -- wake up main thread to send data
            if(self.allJoined()){
                self.cv.signal();
            }
            // wait untill all joined and main thread gets the result 
            while(self.allJoined()){
                cv.awaitUninterruptibly();
            }
            // as done updated actionsReference has to be updated
        }catch(Exception e){
            ///
        }finally{
            this.mtx.unlock();
        }
    }
}