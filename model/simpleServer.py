import socket
import numpy as np

class simpleServer:
    def __init__(self, port):
        self.socket = socket.socket(family=socket.AF_INET, type=socket.SOCK_STREAM)
        self.socket.bind(("localhost", port))
        self.socket.listen(1)

    def accept(self):
        c, _ = self.socket.accept()
        self.client = c


    def send_action(self, action):
        '''

        Sends 8 bytes to client (binary representation of state and action)<p>
        @param action {int} as in MPD definition<br>

        '''
        data = np.array(action, dtype=np.int32).tobytes()
        self.client.send(data)


    def recvsrt(self):
        '''

        Waits untill client send 24 bytes of binary representation of tuple (state, reward, terminal_state)<p>
        @return tuple (new state, reward, terminal state)

        '''
        data = self.client.recv(12)
        return tuple(np.fromstring(data, dtype=np.int32).astype(np.int64))

    def close(self):
        self.client.close()
        self.socket.close()

