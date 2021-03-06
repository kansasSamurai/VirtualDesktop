package org.jwellman.jcx;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import javax.swing.JTextArea;

/**
 * This class can be used to execute a system command from a Java application.
 * See the documentation for the public methods of this class for more information.
 *
 * Documentation for this class is available at this URL:
 *
 * http://devdaily.com/java/java-processbuilder-process-system-exec
 *
 *
 * Copyright 2010 alvin j. alexander, devdaily.com.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.

 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser Public License for more details.

 * You should have received a copy of the GNU Lesser Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * Please ee the following page for the LGPL license:
 * http://www.gnu.org/licenses/lgpl.txt
 *
 */
public class SystemCommandExecutor {
	
    private String adminPassword ;
    private List<String> commandInformation ;
    private ThreadedStreamHandler inputStreamHandler ;
    private ThreadedStreamHandler errorStreamHandler ;

    private JTextArea txtStdErr ;
    private JTextArea txtStdOut ;
    private File directory ;

  /**
   * Pass in the system command you want to run as a List of Strings, as shown here:
   * <code>
   * List<String> commands = new ArrayList<String>();
   * commands.add("/sbin/ping");
   * commands.add("-c");
   * commands.add("5");
   * commands.add("www.google.com");
   * SystemCommandExecutor commandExecutor = new SystemCommandExecutor(commands);
   * commandExecutor.executeCommand();
   * </code>
   * Note: I've removed the other constructor that was here to support executing
   *       the sudo command. I'll add that back in when I get the sudo command
   *       working to the point where it won't hang when the given password is
   *       wrong.
   *
   * @param commandInformation The command you want to run.
   * @param stdout
   * @param stderr
   * @param directory
   */
  public SystemCommandExecutor(final List<String> commandInformation, JTextArea stdout, JTextArea stderr, File directory) {
    if (commandInformation==null) throw new NullPointerException("The commandInformation is required.");
    this.commandInformation = commandInformation;
    this.adminPassword = null;
    this.txtStdOut = stdout;
    this.txtStdErr = stderr;
    this.directory = directory;
  }

  /**
   *
   * @return
   * @throws IOException
   * @throws InterruptedException
   */
  public int executeCommand() throws IOException, InterruptedException {

    int exitValue = -99;

    try {
      final ProcessBuilder pb = new ProcessBuilder(commandInformation);
      pb.directory(this.directory); // this handles null
      
      final Process process = pb.start();
      final InputStream inputStream = process.getInputStream(); // [2]
      final InputStream errorStream = process.getErrorStream(); // [2]
      final OutputStream outputStream = process.getOutputStream(); // [1]
      // [1] You need this if you're going to write something to the command's input stream
      // (such as when invoking the 'sudo' command, and it prompts you for a password).
      // [2] I'm currently doing these on a separate line here in case i need to set them to null
      // to get the threads to stop.
      // see http://java.sun.com/j2se/1.5.0/docs/guide/misc/threadPrimitiveDeprecation.html

      // These need to run as java threads to get the standard output and error from the command.
      // The inputstream handler gets a reference to our stdOutput in case we need to write
      // something to it, such as with the sudo command
      errorStreamHandler = this.getErrorHandler(errorStream); // new ThreadedStreamHandler(errorStream);
      inputStreamHandler = this.getInputHandler(inputStream); // new ThreadedStreamHandler(inputStream, stdOutput);

      // TODO the inputStreamHandler has a nasty side-effect of hanging if the given password is wrong; fix it
      errorStreamHandler.start();
      inputStreamHandler.start();

      // TODO a better way to do this?
      exitValue = process.waitFor();

      // TODO a better way to do this?
      inputStreamHandler.interrupt();
      errorStreamHandler.interrupt();
      inputStreamHandler.join();
      errorStreamHandler.join();
    } catch (IOException e) {
      // TODO deal with this here, or just throw it?
      throw e;
    } catch (InterruptedException e) {
      // generated by process.waitFor() call
      // TODO deal with this here, or just throw it?
      throw e;
    } finally {
    }

    return exitValue;
  }

  private ThreadedStreamHandler getErrorHandler(InputStream is) {
      if (txtStdErr == null) return new ThreadedStreamHandler(is);
      else return new ThreadedSwingHandler(is, txtStdErr);
  }

  private ThreadedStreamHandler getInputHandler(InputStream is) {
      if (txtStdOut == null) return new ThreadedStreamHandler(is);
      else return new ThreadedSwingHandler(is, txtStdOut);
  }

  /**
   * Get the standard output (stdout) from the command you just exec'd.
   * @return
   */
  public StringBuilder getStandardOutputFromCommand() {
    return inputStreamHandler.getOutputBuffer();
  }

  /**
   * Get the standard error (stderr) from the command you just exec'd.
   * @return
   */
  public StringBuilder getStandardErrorFromCommand() {
    return errorStreamHandler.getOutputBuffer();
  }

}
