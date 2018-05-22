import numpy as np
import tensorflow as tf
from tensorflow.contrib import learn

class DQN:
    def __init__(self, ndim, ydim, num_nodes=10, num_layers=1, learning_rate=10e-4, epsilone=1.0, min_epsilon=1e-4, gamma=0.1, labmda=0.1, source=None, dest=None):
        '''

        '''
        self.ndim = ndim
        self.ydim = ydim
        self.num_nodes = num_nodes
        self.num_layers = num_layers
        self.dest = dest

        self.session = tf.Session()

        self.x_placehold = tf.placeholder(shape=[None, self.ndim], dtype=tf.float64, name="input")
        self.h_placehold = tf.placeholder(shape=[None, self.ndim], dtype=tf.float64, name="hidden_state")
        self.y_placehold = tf.placeholder(shape=[None, self.ydim], dtype=tf.float64, name="target_Q")

        self.y, self.h = self._forward_graph()
        self.loss_tensor = self._loss(self,y self.y_placehold)
        self.update_op = self._optimize(self.loss_tensor, learning_rate)

        self.saver = None
        if source:
            self.restore(source)
    

    def _forward_graph(self):
        '''
        TODO: work on model; curennt model: multiple LSTM cells w/out fully connected layers
        '''
        def lstm_cell():
            return tf.contrib.rnn.BasicLSTMCell(self.num_nodes)
        with tf.name_scope("forward_model"):
            model = tf.contrib.rnn.MultiRNNCell([lstm_cell() for _ in range(self.num_layers)])
            y, h = model(self.x, self.h)

        return y, h


    def _loss(self, y, y_gt):
        '''
        TODO: maximize output Q value MATH IS WRONG HERE
        '''
        return tf.reduce_mean(tf.square(y - y_gt), name="loss")


    def _optimize(self, loss, learning_rate):
        '''

        Simple Adam optimizer for backpropagation. Private funciton.<p>
        @param loss {Tensor} loss tensor to minimize<br>
        @param learning_rate {float} initial learning_rate for adam optimizer<br>
        @return {Tensor} optimizer<br>

        '''
        return tf.train.AdamOptimizer(learning_rate=learning_rate).minimize(loss, name="optimizer")

    
    def saveModel(self, global_step):
        '''

        Saves current model to the dest (that is specified in init)<p>
        @param global_step {int} global step to keep track of evolving<br>
        @return {Stgring} full path to stored model<br>

        '''
        if not self.saver:
            self.saver = tf.train.Saver()
        return self.saver.save(self.session, self.dest, global_step=global_step)


    def restore(self, path):
        '''

        Restore the model from the file in {path} to tf graph<p>
        @param path {String} to file with pre-trained model<br>

        '''
        if not self.saver:
            self.saver = tf.train.Saver()
        try:
            self.saver.restore(self.session, path)
        except:
            print("Failed to restore model at '%s'" % path)