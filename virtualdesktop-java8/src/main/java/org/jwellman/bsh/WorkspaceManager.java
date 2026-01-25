package org.jwellman.bsh;

import bsh.*;
import java.util.HashMap;
import java.util.Map;

/**
 * 
 * 
 * @author rwellman
 */
public class WorkspaceManager {

    private String activeName = "global";
    private NameSpace globalNs;
    // The persistent interpreter; not the temporary one created during each eval loop
    private Interpreter masterInterpreter; 
    private Map<String, NameSpace> workspaces = new HashMap<>();

    public WorkspaceManager(Interpreter it) {
        this.masterInterpreter = it;

        // Capture the original global namespace
        this.globalNs = it.getNameSpace();
    }

    public void createWorkspace(String name) {
        // Create a new namespace with Global as the parent
        NameSpace ws = new NameSpace(globalNs, name);
        workspaces.put(name, ws);
    }

    /**
     * Provides a bridge for the BeanShell script to retrieve a specific 
     * NameSpace object from the manager's map.
     */
    public NameSpace getWorkspaceNamespace(String name) {
        if (workspaces.containsKey(name)) {
            return workspaces.get(name);
        } else if ("global".equalsIgnoreCase(name)) {
            return globalNs;
        }
        return null;
    }

    public void switchTo(String name) {
        int version = 4;
        NameSpace target = null;
        
        switch (version) {
        case 1:
            // This is the version that "mostly" works... testing version 2
            
            // We ignore any passed-in interpreter and use the Master
            if (name.equals("global")) {
                masterInterpreter.setNameSpace(globalNs);
                activeName = "global";
            } else {
                target = workspaces.get(name);
                if (target != null) {
                    masterInterpreter.setNameSpace(target);
                    activeName = name;
                }
            }
            break;
        case 2:
            // This is an attempted fix so that the console will "see" the same namespace as the scripttester
            
            target = name.equals("global") ? globalNs : workspaces.get(name);
            if (target != null) {
                // Force the MASTER interpreter to point to the new namespace
                this.masterInterpreter.setNameSpace(target);
                this.activeName = name;
                System.out.println("Master Interpreter shifted to: " + name);
            }
            break;
        case 3:
            // version 2 didn't work... version 3
            target = "global".equalsIgnoreCase(name) ? globalNs : workspaces.get(name);

            if (target != null) {
                // 1. Update the Interpreter's field (for future evals)
                masterInterpreter.setNameSpace(target);

                // 2. THE SECRET SAUCE:
                // We need to trigger the Interpreter to realize its root has changed.
                // In 2.0b5, if we can't restart the run() loop, we must inject the
                // change into the current stack.
                try {
                    // This eval runs in the current (old) stack but forces
                    // the 'this' reference to update to the new target.
                    masterInterpreter.eval(
                            "this.interpreter.globalNameSpace = " + "wm.getWorkspaceNamespace(\"" + name + "\");",
                            target);
                } catch (Exception e) {
                    // Fallback: Manually update the 'bsh' system object
                    // which the interpreter uses for some lookups
                    System.out.println("EXCEPTION: Exception swapping to " + name);
                    e.printStackTrace();
                }

                this.activeName = name;
                System.out.println("CRITICAL: Namespace swapped to " + name);
            }
            break;
        case 4:
            System.out.println("INFO: Fix version 4 ");
            target = "global".equalsIgnoreCase(name) ? globalNs : workspaces.get(name);
            if (target != null) {
                // 1. Update the master pointer
                masterInterpreter.setNameSpace(target);

                // 2. Re-anchor the live stack
                // We use 'this.interpreter' to ensure we are talking to the Master.
                // We clear the stack and push the NEW namespace that we just set.
                try {
                    masterInterpreter.eval(
                            "this.interpreter.globalNameSpace = " + "wm.getWorkspaceNamespace(\"" + name + "\");");

                    // This is the specific line to reset the stack's base
                    // without relying on 'this' as a namespace reference.
                    masterInterpreter.eval("this.interpreter.getNameSpace().clear();");
                } catch (Exception e) {
                    // Manual fallback if eval is blocked
                    masterInterpreter.setu("global", target);
                    System.out.println("EXCEPTION: Exception swapping to " + name);
                    e.printStackTrace();
                }

                this.activeName = name;
                System.out.println("CRITICAL: Namespace swapped to " + name);
            }
            break;
        }
    }

    public void deleteWorkspace(String name) {
        workspaces.remove(name);
    }
    
    public String getActiveWorkspaceName() {
        return activeName;
    }

    public String[] getWorkspaceNames() {
        return workspaces.keySet().toArray(new String[0]);
    }

    
    /* ======== test script =============

print("Scripted Tool sees wm as: " + System.identityHashCode(wm));

// 1. Define something in Global
x = 100;

// 2. Create and switch to a scratchpad
wm.createWorkspace(this.interpreter, "test1");
wm.switchTo("test1");

// 3. Pollute this workspace
var x = 200; // This no longer shadows the global 'x'
tempFunc() { return "I only exist in test1"; }

print(x); // Prints 200
print(tempFunc()); // Prints "I only exist..."

// 4. Switch back to Global or a fresh workspace
wm.switchTo("global");

print(x); // Prints 100! The global value is untouched.
tempFunc(); // This would now fail 

------------------------------------
ased on my analysis, I have a few theories about what's happening:                                   
                                                                                                          Theory 1: Script Block Namespace Binding                                                                                                                                                                      
  When you run a multi-line script, BeanShell parses and executes it as a single block. The namespace     context for all statements is established at the beginning of block execution. Calling switchTo()   
  changes the interpreter's namespace, but the currently-executing block still has its namespace        
  reference from when it started.

  Test this theory: Try running your script one line at a time in the REPL:
  bsh % x = 100;
  bsh % wm.switchTo(this.interpreter, "test1");
  bsh % x = 200;
  bsh % print(x);
  bsh % wm.resetToGlobal(this.interpreter);
  bsh % print(x);

  If one-line-at-a-time works but the multi-line script doesn't, this confirms the theory.

  Theory 2: Variable Resolution in Parent Namespace

  When you do x = 200, BeanShell walks up the namespace chain. Since x already exists in the parent     
  (global) namespace, it updates THAT rather than creating a new x in the workspace namespace. The child   namespace shadows only work when you declare a NEW variable.

  Test: Try using a different variable name:
  wm.switchTo(this.interpreter, "test1");
  y = 200;  // New variable, not in global

  Theory 3: The REPL's eval() Creates Wrapper Namespaces

  Looking at SpecBeanShell line 55:
  new Thread(interpreter, "BeanShell Interpreter").start();

  The interpreter's REPL loop calls eval() for each line/block. BeanShell's eval() method may create a  
  temporary "caller" namespace for each evaluation, which could interfere with your namespace switching.
  Potential Fix: Instead of interpreter.setNameSpace(), you might need to ensure the subsequent code    
  explicitly runs in the new namespace. One approach would be:

  public void switchTo(Interpreter it, String name) {
      NameSpace target = workspaces.get(name);
      if (target != null) {
          it.setNameSpace(target);
          // Also try setting the caller namespace if applicable
      }
  }

--------------------------------------------
How to test original version:
wm.switchTo(this.interpreter, "test1"); 
inject(this, "whereAmI() { return this.namespace; }");
print(whereAmI());

Note that the following output does NOT return 'test1'
as desired.  This led to the finding and confirmation that
the beanshell eval loop was creating a temporary interpreter
for EVERY command (Interpreter.java :: line 623)
bsh % NameSpace: whereAmI (bsh.NameSpace@5116435c) (method) 

--------------------------------------------
How to verify that the console is consistently using the same interpreter:
// Check identity of the interpreter
print("Interpreter ID: " + System.identityHashCode(this.interpreter));


--------------------------------------------

// 1. Switch
wm.switchTo("test1");

// 2. Define with a self-report
tempFunc() { return "I am in: " + this.namespace.getName() + " (Hash: " + System.identityHashCode(this.namespace) + ")";  }

// 3. Print immediately from the tool
print("Tool sees: " + tempFunc());

//////////////////////////////////////

print("Interpreter thinks it is in: " + this.interpreter.getNameSpace().getName());
print("Console sees: " + tempFunc());











    */

}

