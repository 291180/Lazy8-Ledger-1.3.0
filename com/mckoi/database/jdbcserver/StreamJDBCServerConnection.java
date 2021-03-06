/**
 * com.mckoi.database.jdbcserver.StreamJDBCServerConnection  22 Jul 2000
 *
 * Mckoi SQL Database ( http://www.mckoi.com/database )
 * Copyright (C) 2000, 2001  Diehl and Associates, Inc.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * Version 2 as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License Version 2 for more details.
 *
 * You should have received a copy of the GNU General Public License
 * Version 2 along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 *
 * Change Log:
 * 
 * 
 */

package com.mckoi.database.jdbcserver;

import com.mckoi.debug.DebugLogger;
import com.mckoi.database.Database;
import com.mckoi.database.jdbc.ProtocolConstants;
import com.mckoi.database.jdbc.DatabaseInterface;
import com.mckoi.util.LengthMarkedBufferedInputStream;
import java.io.*;

/**
 * A generic JDBC stream protocol server that reads JDBC commands from a
 * stream from each connection and dispatches the commands appropriately.
 *
 * @author Tobias Downer
 */

abstract class StreamJDBCServerConnection extends JDBCProcessor
                                                 implements ServerConnection {

  /**
   * The size in bytes of the buffer used for writing information onto the
   * output stream to the client.
   */
  private static final int OUTPUT_BUFFER_SIZE = 32768;

  /**
   * The size in bytes of the buffer used for reading information from the
   * input stream from the client.
   */
  private static final int INPUT_BUFFER_SIZE = 16384;

  /**
   * The LengthMarkedBufferedInputStream we use to poll for commands from the
   * client.
   */
  private LengthMarkedBufferedInputStream marked_input;

  /**
   * The output stream to the client formatted as a DataOutputStream.
   */
  private DataOutputStream out;

  /**
   * Sets up the protocol connection.
   */
  StreamJDBCServerConnection(DatabaseInterface db_interface,
                  InputStream in, OutputStream out, DebugLogger logger)
                                                         throws IOException {
    super(db_interface, logger);

    this.marked_input = new LengthMarkedBufferedInputStream(in);
    this.out = new DataOutputStream(
                           new BufferedOutputStream(out, OUTPUT_BUFFER_SIZE));

  }

  // ---------- Implemented from JDBCConnection ----------

  // NOTE: There's a security issue for this method.  See JDBCProcessor
  //   for the details.
  public void sendEvent(byte[] event_msg) throws IOException {
    synchronized (out) {
      // Command length...
      out.writeInt(4 + 4 + event_msg.length);
      // Dispatch id...
      out.writeInt(-1);
      // Command id...
      out.writeInt(ProtocolConstants.DATABASE_EVENT);
      // The message...
      out.write(event_msg, 0, event_msg.length);
      // Flush command to server.
      out.flush();
    }
  }

  // ---------- Implemented from ServerConnection ----------

  /**
   * Inspects the input stream and determines in there's a command pending
   * to be processed.
   */
  public boolean requestPending() throws IOException {
    int state = getState();
    if (state == 100) {
      return marked_input.pollForCommand(Integer.MAX_VALUE);
    }
    else {
      return marked_input.pollForCommand(256);
    }
  }

  /**
   * Processes a request from this connection.
   */
  public void processRequest() throws IOException {
    // Only allow 8 commands to execute in sequence before we free this
    // worker to the worker pool.
    // We have a limit incase of potential DOS problems.
    int sequence_limit = 8;

    // Read the command into a 'byte[]' array and pass to the command
    // processor.
    int com_length = marked_input.available();
    while (com_length > 0) {
      byte[] command = new byte[com_length];
      marked_input.read(command, 0, com_length);

      // Process the command
      byte[] response = processJDBCCommand(command);
      if (response != null) {

        synchronized (out) {
          // Write the response to the client.
          out.writeInt(response.length);
          out.write(response);
          out.flush();
        }

      }

      // If there's another command pending then process that one also,
      com_length = 0;
      if (sequence_limit > 0) {
        if (requestPending()) {
          com_length = marked_input.available();
          --sequence_limit;
        }
      }

    } // while (com_length > 0)

//    // Response...
//    printByteArray(response);
  }

  /**
   * Block waiting for a complete command to become available.
   */
  public void blockForRequest() throws IOException {
    marked_input.blockForCommand();
  }

  /**
   * Pings the client to check it's still alive.
   */
  public void ping() throws IOException {
    synchronized (out) {
      // Command length...
      out.writeInt(8);
      // Dispatch id...
      out.writeInt(-1);
      // Ping command id...
      out.writeInt(ProtocolConstants.PING);
      // Flush command to server.
      out.flush();
    }
  }

}
