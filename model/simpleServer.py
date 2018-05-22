import socket
import numpy as np

class simpleServer:
    def __init__(self, port):
        self.socket = socket.socket(family=socket.AF_INET, type=socket.SOCK_STREAM)
        self.socket.bind((socket.gethostname(), port))
        self.socket.listen(1)

    def accept(self):
        c, _ = self.socket.accept()
        self.client = c


    def sendsa(self, sa):
        '''

        Sends 16 bytes to client (binary representation of state and action)<p>
        @param sa {tuple} (state, action) as in MPD definition<br>

        '''
        data = np.array(sa, dtype=np.int64).tobytes()
        self.client.send(data)


    def recvsrt(self):
        '''

        Waits untill client send 24 bytes of binary representation of tuple (state, reward, terminal_state)<p>
        @return tuple (new state, reward, terminal state)

        '''
        data = self.client.recv(24)
        return tuple(np.fromstring(data, dtype=np.int64))

    def close(self):
        self.client.close()
        self.socket.close()

