package org.jwellman.jcx;

import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ProcessBuilderExample {

    public static void main(String[] args) throws Exception {
        new ProcessBuilderExample(2, "");
    }

    // can run basic ls or ps commands
    // can run command pipelines
    // can run sudo command if you know the password is correct
    public ProcessBuilderExample(int version, String cmdline) throws IOException, InterruptedException {
        this.guts(version, cmdline);
    }

    public ProcessBuilderExample() {}

    public void exec(String cmdline) {
        try {
            this.guts(2, cmdline);
        } catch (InterruptedException ex) {
            Logger.getLogger(ProcessBuilderExample.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(ProcessBuilderExample.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void guts(int version, String cmdline) throws IOException, InterruptedException {
        int result = -99;
        StringBuilder stdout = null;
        StringBuilder stderr = null;
        List<String> commands = new ArrayList<String>();
        SystemCommandExecutor commandExecutor = null;

        switch (version) {
            case 1:
                // build the system command we want to run
//                List<String> commands = new ArrayList<String>();
                commands.add("/bin/sh");
                commands.add("-c");
                commands.add("ls -l /var/tmp | grep tmp");

                // execute the command
                commandExecutor = new SystemCommandExecutor(commands, null, null, null);
                result = commandExecutor.executeCommand();

                // get the stdout and stderr from the command that was run
                stdout = commandExecutor.getStandardOutputFromCommand();
                stderr = commandExecutor.getStandardErrorFromCommand();

                // print the stdout and stderr
                System.out.println("The numeric result of the command was: " + result);
                System.out.println("STDOUT:");
                System.out.println(stdout);
                System.out.println("STDERR:");
                System.out.println(stderr);
                break;
            case 2:
                // build the system command we want to run
//                List<String> commands = new ArrayList<String>();
                commands.add("cmd.exe"); // need /windows/system32/ ?
                commands.add("/C");
                commands.add(cmdline);

                // execute the command
                commandExecutor = new SystemCommandExecutor(commands, null, null, null);
                result = commandExecutor.executeCommand();

                // get the stdout and stderr from the command that was run
                stdout = commandExecutor.getStandardOutputFromCommand();
                stderr = commandExecutor.getStandardErrorFromCommand();

                // print the stdout and stderr
                System.out.println("The numeric result of the command was: " + result);
                System.out.println("STDOUT:");
                System.out.println(stdout);
                System.out.println("STDERR:");
                System.out.println(stderr);
                break;
        }
    }

}
