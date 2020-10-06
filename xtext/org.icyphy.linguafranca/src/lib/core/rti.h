/**
 * @file
 * @author Edward A. Lee (eal@berkeley.edu)
 *
 * @section LICENSE
Copyright (c) 2020, The University of California at Berkeley.

Redistribution and use in source and binary forms, with or without modification,
are permitted provided that the following conditions are met:

1. Redistributions of source code must retain the above copyright notice,
   this list of conditions and the following disclaimer.

2. Redistributions in binary form must reproduce the above copyright notice,
   this list of conditions and the following disclaimer in the documentation
   and/or other materials provided with the distribution.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY
EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL
THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF
THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

 * @section DESCRIPTION
 * Header file for the runtime infrastructure for distributed Lingua Franca programs.
 */

#ifndef RTI_H
#define RTI_H

#include <pthread.h>
#include "reactor.h"

/** Size of the buffer used for messages sent between federates.
 *  This is used by both the federates and the rti, so message lengths
 *  should generally match.
 */
#define BUFFER_SIZE 256

/** Number of seconds that elapse between a federate's attempts
 *  to connect to the RTI.
 */
#define CONNECT_RETRY_INTERVAL 2

/** Bound on the number of retries to connect to the RTI.
 *  A federate will retry every CONNECT_RETRY_INTERVAL seconds
 *  this many times before giving up. E.g., 500 retries every
 *  2 seconds results in retrying for about 16 minutes.
 */
#define CONNECT_NUM_RETRIES 500

/**
 * Default starting port number for the RTI's socket server.
 * Unless a specific port has been specified by the LF program,
 * the RTI, when it starts up, will attempt to open a socket server
 * on this port, and, if this fails, increment the port number and
 * try again. The number of increments is limited by PORT_RANGE_LIMIT.
 */
#define STARTING_PORT 15045

/**
 * Number of ports to try to connect to. Unless the LF program specifies
 * a specific port number to use, the RTI will attempt to start
 * a socket server on port 15045. If that port is not available (e.g.,
 * another RTI is running or has recently exited), then it will try the
 * next port, 15046, and keep incrementing the port number up to this
 * limit. If no port between 15045 and 15045 + PORT_RANGE_LIMIT
 * is available, then the RTI will fail to start. This number, therefore,
 * limits the number of RTIs that can be simultaneously
 * running on any given machine.
 */
#define PORT_RANGE_LIMIT 8

////////////////////////////////////////////
//// Message types

// These message types will be encoded in an unsigned char,
// so the magnitude must not exceed 255.

/**
 * Byte identifying a rejection of the previously received message.
 * The reason for the rejection is included as an additional byte
 * (uchar) (see below for encodings of rejection reasons).
 */
#define REJECT 0

/**
 * Byte identifying an acknowledgment of the previously received message.
 */
#define ACK 255

/** Byte identifying a message from a federate to an RTI containing
 *  the federation ID and the federate ID. The message contains, in
 *  this order:
 *  * One byte equal to FED_ID.
 *  * Two bytes (ushort) giving the federate ID.
 *  * One byte (uchar) giving the length N of the federation ID.
 *  * N bytes containing the federation ID.
 *  Each federate needs to have a unique ID between 0 and
 *  NUMBER_OF_FEDERATES-1.
 *  Each federate, when starting up, should send this message
 *  to the RTI. This is its first message to the RTI.
 *  The RTI will respond with either REJECT or ACK.
 *  If the federate is a C target LF program, the generated
 *  code does this by calling synchronize_with_other_federates(),
 *  passing to it its federate ID.
 */
#define FED_ID 1

/** Byte identifying a timestamp message, which is 64 bits long. */
#define TIMESTAMP 2

/** Byte identifying a message to forward to another federate.
 *  The next two bytes will be the ID of the destination port.
 *  The next two bytes are the destination federate ID.
 *  The four bytes after that will be the length of the message.
 *  The remaining bytes are the message.
 */
#define MESSAGE 3

/** Byte identifying that the federate is ending its execution. */
#define RESIGN 4

/** Byte identifying a timestamped message to forward to another federate.
 *  The next two bytes will be the ID of the destination port.
 *  The next two bytes are the destination federate ID.
 *  The four bytes after that will be the length of the message.
 *  The next eight bytes will be the timestamp.
 *  The remaining bytes are the message.
 */
#define TIMED_MESSAGE 5

/** Byte identifying a next event time (NET) message sent from a federate.
 *  The next eight bytes will be the timestamp. This message from a
 *  federate tells the RTI the time of the earliest event on that federate's
 *  event queue. In other words, absent any further inputs from other federates,
 *  this will be the logical time of the next set of reactions on that federate.
 */
#define NEXT_EVENT_TIME 6

/** Byte identifying a time advance grant (TAG) sent to a federate.
 *  The next eight bytes will be the timestamp.
 */
#define TIME_ADVANCE_GRANT 7

/** Byte identifying a logical time complete (LTC) message sent by a federate
 *  to the RTI. The next eight bytes will be the timestamp.
 */
#define LOGICAL_TIME_COMPLETE 8

/** Byte identifying a stop message. When any federate calls stop(), it will
 *  send this message to the RTI, which will then broadcast it to all other
 *  federates. The next 8 bytes will be the timestamp.
 *  NOTE: It is not clear whether sending a stopping timestamp is useful.
 *  If any federate can send a STOP message that specifies the stop time on
 *  all other federates, then every federate depends on every other federate
 *  and time cannot be advanced. Hence, the current implementations may result
 *  in nondeterministic stop times.
 */
#define STOP 9

/**
 * Byte identifying a address query message, sent by a federate to RTI
 * to ask for another federates address and port number.
 */
#define ADDRESSQUERY 10

/**
 * Byte identifying a message adverising the port for the physical connection server
 * of a federate
 */
#define ADDRESSAD 11

/**
 * Byte identifying a message that is sent by a federate directly to another federate.
 */
#define P2PMESSAGE 12

/**
 * A vairant of P2PMESSAGE that carries the logical timestamp as well between the federates.
 * This can only be used in coordinated mode.
 */
#define P2PMESSAGE_TIMED 13

/////////////////////////////////////////////
//// Rejection codes

/**
 * These codes are sent in a REJECT message.
 * They are limited to one byte (uchar).
 */

/** Federation ID does not match. */
#define FEDERATION_ID_DOES_NOT_MATCH 1

/** Federate with the specified ID has already joined. */
#define FEDERATE_ID_IN_USE 2

/** Federate ID out of range. */
#define FEDERATE_ID_OUT_OF_RANGE 3

/** Incoming message is not expected. */
#define UNEXPECTED_MESSAGE 4

/** Connected to the wrong server. */
#define WRONG_SERVER 5

/////////////////////////////////////////////
//// Data structures

/** Mode of execution of a federate. */
typedef enum execution_mode_t {
    FAST,
    REALTIME
} execution_mode_t;

/** State of a federate during execution. */
typedef enum fed_state_t {
    NOT_CONNECTED,  // The federate has not connected.
    GRANTED,        // Most recent NEXT_EVENT_TIME has been granted.
    PENDING         // Waiting for upstream federates.
} fed_state_t;

/** Information about a federate, including its runtime state,
 *  mode of execution, and connectivity with other federates.
 *  The list of upstream and downstream federates does not include
 *  those that are connected via a "physical" connection (one
 *  denoted with ~>) because those connections do not impose
 *  any scheduling constraints.
 */
typedef struct federate_t {
    int id;                 // ID of this federate.
    pthread_t thread_id;    // The ID of the thread handling communication with this federate.
    int socket;             // The socket descriptor for communicating with this federate.
    instant_t completed;    // The largest logical time completed by the federate (or NEVER).
    instant_t next_event;   // Most recent NET received from the federate (or NEVER).
    fed_state_t state;      // State of the federate.
    int* upstream;          // Array of upstream federate ids.
    interval_t* upstream_delay;    // Minimum delay on connections from upstream federates.
    int num_upstream;              // Size of the array of upstream federates and delays.
    int* downstream;        // Array of downstream federate ids.
    int num_downstream;     // Size of the array of downstream federates.
    execution_mode_t mode;  // FAST or REALTIME.
    unsigned char server_hostname[INET_ADDRSTRLEN]; // Information about the hostname and 
    int server_port;       // port number of the socket server of the federate
                    // if it has any.
    struct in_addr server_ip_addr; // Information about the ip address of the socket
                                // server of the federate.
} federate_t;


#endif /* RTI_H */
