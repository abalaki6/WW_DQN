import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;

/**
 * NetworkConnector
 * Simple connector to python environment to exchange information
 * How it works:
 * python has network to output the action given current state (and hidden state from previous via DLSTM)
 * java has environment for wumpus world
 * use this class to send (s_t,r_t,t_t) to python and receive (a_t) in back
 * @author Artsiom Balakir
 * @since 05/25/2018
 */
public class NetworkConnector{
    static final private int itemSize = 4;
    
    private ByteBuffer SRTBuffer;      // buffer to store set of (s_it, r_it, t_it)
    private ByteBuffer ABuffer;        // buffer to store set of (a_it)
    private IntBuffer SRTIntBuffer;    // wrapper for SRTBuffer to work with int
    private byte[] byteBuffer;         // buffer to receive data
    
    private Socket socket;             // communication socket
    private InputStream inStream;      // input stream from socket
    private OutputStream outStream;    // output stream from socket
    
    private int simulators;            // number of parallel simulators
    

    /**
     * Creates all buffers for interal communication
     * @param port port to connect 
     * @param number_parallel_simulators how many parallel runs network has to expect
     */
    public NetworkConnector(int port, int number_parallel_simulators){
        this.simulators = number_parallel_simulators;
        
        this.SRTBuffer = ByteBuffer.allocate(3 * itemSize * this.simulators);
        this.SRTBuffer.order(ByteOrder.LITTLE_ENDIAN);
        
        this.byteBuffer = new byte[itemSize * this.simulators];

        try{
            socket = new Socket("localhost", port);
            inStream = new BufferedInputStream(socket.getInputStream());
            outStream = new BufferedOutputStream(socket.getOutputStream());
        }catch(Exception e){
            e.printStackTrace();
            System.exit(1);
        }

        this.clean();
    }

    /**
     * Update tuple (s, r, t) for specified simulator
     * @param state new state of the simulator
     * @param reward reward for previous action
     * @param terminal is the state terminal
     * @param simulator_id id of the simulator to update information
     */
    public void setSRT(int state, int reward, boolean terminal, int simulator_id){
        this.SRTIntBuffer.put(simulator_id * 3, state).
            put(simulator_id * 3 + 1, reward).
            put(simulator_id * 3 + 2, (terminal ? 1 : 0));
    }

    /**
     * Block untill other side of simulator runs the model and return calculated actions
     * @param buffer pre-allocated buffer for actions
     * @return int[] array of actions
     */
    public int[] getActions(int[] buffer){
        try{
            // send collected information from each simulator
            this.outStream.write(this.SRTBuffer.array());
            this.outStream.flush();
            // wait untill model computes the corresponding actions, read them
            this.inStream.read(this.byteBuffer);
            // get bytes, parse and convert to int
            IntBuffer ABuffer = ByteBuffer.wrap(this.byteBuffer).
                order(ByteOrder.LITTLE_ENDIAN).
                asIntBuffer().
                get(buffer);
    
        }catch(Exception e){
            e.printStackTrace();
            System.exit(1);
        }
        this.clean();
        return buffer;
    } 
    
    /**
     * Closes connection to server, cleans up streams
     */
    public void close(){
        try{
            this.inStream.close();
            this.outStream.close();
            this.socket.close();
        }catch(Exception e){
            e.printStackTrace();
            System.exit(1);
        }
    }
    
    /**
     * Cleans up buffer for being reusable
     */
    private void clean(){
        this.SRTIntBuffer = this.SRTBuffer.asIntBuffer();
    }
}