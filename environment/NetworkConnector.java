import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * NetworkConnector
 * Simple connector to python environment to exchange information
 * How it works:
 * python has network to output the action given current state (and hidden state from previous via DLSTM)
 * java has environment for wumpus world
 * use this class to send (s_t,r_t,t_t) to python and receive (a_t) in back
 */
public class NetworkConnector{
    static final private int itemSize = 4;
    
    private int _port;
    private ByteBuffer _SRTBuffer;
    private ByteBuffer _ABuffer;
    private int[] _buffer;
    private byte[] _bBuffer;

    private Socket socket;
    private InputStream inStream;
    private OutputStream outStream;

    public NetworkConnector(int port){
        this._port = port;
        this._SRTBuffer = ByteBuffer.allocate(3 * itemSize);
        this._SRTBuffer.order(ByteOrder.LITTLE_ENDIAN);
        this._ABuffer = ByteBuffer.allocate(1 * itemSize);
        this._buffer = new int[3];
        this._bBuffer = new byte[itemSize];

        try{
            socket = new Socket("localhost", this._port);
            inStream = new BufferedInputStream(socket.getInputStream());
            outStream = new BufferedOutputStream(socket.getOutputStream());
        }catch(Exception e){
            e.printStackTrace();
            System.exit(1);
        }
    }

    public int getAction(int state, int reward, boolean terminal){
        try{
            this.outStream.write(this.pack(state, reward, terminal));
            this.outStream.flush();
    
            this.inStream.read(this._bBuffer);
            this._ABuffer = ByteBuffer.wrap(this._bBuffer);
            this._ABuffer.order(ByteOrder.LITTLE_ENDIAN);
        }catch(Exception e){
            e.printStackTrace();
            System.exit(1);
        }
        return this._ABuffer.getInt();
    } 

    private byte[] pack(int state, int reward, boolean terminal){
        this._buffer[0] = state;
        this._buffer[1] = reward;
        this._buffer[2] = terminal ? 1 : 0;
        this._SRTBuffer.asIntBuffer().put(this._buffer, 0, 3);
        return this._SRTBuffer.array();
    }

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
    
}