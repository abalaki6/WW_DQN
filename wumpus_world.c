#include <unistd.h>
#include <signal.h>
#include <stdio.h>
#include <stdlib.h>
#include <sys/types.h>
#include <sys/wait.h>
#include <string.h>

/**
 * Simple C integrator to run both environments in one call
 * All hyperparameters for environment are provided via command line here
 * To safely stop training send SIGINT via Ctrl+C, signal is handled in all calls
 * @author Artsiom Balakir
 * @since 05/26/2018
 */

static int server_is_ready;
static int interrupted; 
static pid_t PYTHON_PID;
static pid_t JAVA_PID;

// TODO gemeralize with parameters
static const char* PYTHON_CALL = "python3 model/dqn/q_learning.py";
static const char* JAVA_CALL = "make run";

/** 
 * handler for SIGINT and SIGALARM
 */
void signal_handler(int signal){
    switch(signal){
        case SIGINT:
            interrupted = 1;
            break;
        case SIGALRM:
            server_is_ready = 1;
            break;
        default:
            fprintf(stderr, "Unexpected signal was catched: %d\n", signal);
    }
}


int main(int argc, char** argv){
    // TODO: read command line arguments

    // mask all signals
    server_is_ready = 0;
    interrupted = 0;

    struct sigaction new_action;
    memset(&new_action, 0, sizeof(struct sigaction));
    new_action.sa_flags = SA_RESTART;
    new_action.sa_handler = signal_handler;

    sigfillset(&new_action.sa_mask);
    // remove alarm from set
    sigaction(SIGINT, &new_action, NULL);

    // run python server
    PYTHON_PID = fork();
    if(!PYTHON_PID){
        // run python code
        int status = system(PYTHON_CALL); 
        exit(status);
    }
    // parent waits for completely installed server and loaded model
    // child will send SIGALARM on completion
    int remain = sleep(10);
    // if remain is positive -- catched signal 
    if(!remain){
        fprintf(stderr, "Timeout, failed to load model\n");
        exit(EXIT_FAILURE);
    }
    
    // add int signal for JAVA side
    sigaction(SIGALRM, &new_action, NULL);
    // run java environment
    JAVA_PID = fork();
    if(!JAVA_PID){
        int status = system(JAVA_CALL);
        exit(status);
    }
    // parent now waits for both children
    // TODO: wrapper
    waitpid(JAVA_PID, NULL, 0);
    waitpid(PYTHON_PID, NULL, 0);
}