import socket
import numpy as np

class simpleServer:
    def __init__(self, port, num_games=1):
        self._socket = socket.socket(family=socket.AF_INET, type=socket.SOCK_STREAM)
        self._socket.bind(("localhost", port))
        self._socket.listen(1)
        self._games = num_games

    def accept(self):
        c, _ = self._socket.accept()
        self._client = c


    def send_action(self, action):
        '''

        Sends 4 * number of games bytes to client (binary representation of state and action)<p>
        @param action {np.array, shape=(num_games)} as in MPD definition<br>

        '''
        data = np.array(action, dtype=np.int32).tobytes()
        self._client.send(data)


    def recvsrt(self):
        '''

        Waits untill client send 12 * number of games bytes of binary representation of tuple (state, reward, terminal_state)<p>
        @return np.array({new state, reward, terminal state}), shape=(num_games, 3)

        '''
        data = self._client.recv(12 * self._games)
        return np.fromstring(data, dtype=np.int32).reshape(-1, 3)
        
    
    def close(self):
        self._client.close()
        self._socket.close()

