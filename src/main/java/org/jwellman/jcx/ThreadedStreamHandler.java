package org.jwellman.jcx;

import java.io.*;

/**
 * This class is intended to be used with the SystemCommandExecutor
 * class to let users execute system commands from Java applications.
 *
 * This class is based on work that was shared in a JavaWorld article
 * named "When System.exec() won't" available at this URL:
 *
 * http://www.javaworld.com/javaworld/jw-12-2000/jw-1229-traps.html
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
 * Please see the following page for the LGPL license:
 * http://www.gnu.org/licenses/lgpl.txt
 *
 */
class ThreadedStreamHandler extends Thread {

    protected String adminPassword;
    protected InputStream inputStream;
    protected OutputStream outputStream;
    protected PrintWriter printWriter;

    protected StringBuilder outputBuffer = new StringBuilder();

    protected boolean sudoIsRequested = false;

    /**
     * A simple constructor for when the sudo command is not necessary.
     * This constructor will just run the command you provide,
     * without running sudo before the command,
     * and without expecting a password.
     *
     * @param inputStream
     * @param streamType
     */
    ThreadedStreamHandler(InputStream inputStream) {
        this.inputStream = inputStream;
    }

    ThreadedStreamHandler(InputStream inputStream, OutputStream outputStream) {
        this.inputStream = inputStream;
        this.outputStream = outputStream;
        this.printWriter = new PrintWriter(outputStream);
        this.sudoIsRequested = false; // true;
    }

    /**
     * Use this constructor when you want to invoke the 'sudo' command. The
     * outputStream must not be null. If it is, you'll regret it. :)
     *
     * TODO this currently hangs if the admin password given for the sudo
     * command is wrong.
     *
     * @param inputStream
     * @param streamType
     * @param outputStream
     * @param adminPassword
     */
    ThreadedStreamHandler(InputStream inputStream, OutputStream outputStream, String adminPassword) {
        this.sudoIsRequested = true;
        this.adminPassword = adminPassword;
        this.inputStream = inputStream;
        this.outputStream = outputStream;
        this.printWriter = new PrintWriter(outputStream);
    }

    @Override
    public void run() {

        // on mac os x 10.5.x, when i run a 'sudo' command, i need to write
        // the admin password out immediately; that's why this code is here.
        if (sudoIsRequested) {
            doSleep(500);
            printWriter.println(adminPassword);
            printWriter.flush();
        }

        BufferedReader bufferedReader = null;
        try {

            final InputStreamReader isr = new InputStreamReader(inputStream, "cp850"); // "cp437" works!! as does "cp850"!! 
            // These do not work as "desired" on windows(10):   , "Cp1252"); // "ISO-8859-1"); // "UTF8");
            // The encodings are from the following URL with a hint using the windows 'chcp' command
            // https://docs.oracle.com/javase/8/docs/technotes/guides/intl/encoding.doc.html
            System.out.println("TSH encoding: " + isr.getEncoding() + " : " + inputStream.getClass().getCanonicalName());
            
            bufferedReader = new BufferedReader(isr);
            String line = null;
            while ((line = bufferedReader.readLine()) != null) {
                this.lineOut(line);
            }
        } catch (IOException ioe) {
            // TODO handle this better
            ioe.printStackTrace();
        } catch (Throwable t) {
            // TODO handle this better
            t.printStackTrace();
        } finally {
            try {
                bufferedReader.close();
            } catch (IOException e) {
                // ignore this one
            }
        }

    }

    /** Handle each line (from the input stream) */
    protected void lineOut(String line) {
        outputBuffer.append(line).append("\n");
    }
    
    /** This method is only required when sudoIsRequested = true */
    private void doSleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            // ignore
        }
    }

    public StringBuilder getOutputBuffer() {
        return outputBuffer;
    }

}
