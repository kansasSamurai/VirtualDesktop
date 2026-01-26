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

    /** The "Data" bucket */
    private NameSpace globalNs; 

    /** The "Tools" bucket */
    private NameSpace utilityNs; 

    /** The persistent interpreter; not the temporary one created during each eval loop */
    private Interpreter masterInterpreter; 

    /** The collection of workspaces */
    private Map<String, NameSpace> workspaces = new HashMap<>();

    /**
     * 
     * @param it
     */
    public WorkspaceManager(Interpreter it) {
        this.masterInterpreter = it;

        // 1. Capture the original global as the Data bucket
        this.globalNs = it.getNameSpace();

        // 2. Extract or define the Utility parent
        // If your interpreter already has a parent (like the 'system' namespace), capture it.
        // Otherwise, create a new one to hold your commands.
        this.utilityNs = globalNs.getParent();
        if (utilityNs == null) {
            this.utilityNs = new NameSpace((NameSpace) null, "utility");
            // TODO I really need to enable the following line for 
            // future completeness but I do not want to brick my current environment.
            // globalNs.setParent(utilityNs);
        }

    }

    /**
     * Why this is the "Golden Path"
     * 
     * Shared Tools: Any method defined in utilityNs (like a custom log() function)
     * is immediately available in every workspace you ever create.
     * 
     * Data Isolation: If you define userData = 500 in Global, a workspace will not
     * see it because they are now "siblings" under the Utility parent, rather than
     * a child of Global.
     * 
     * Clean Object Browser: Your Object Browser will show the Workspace as almost
     * empty, keeping your focus on the new code you are writing.
     * 
     * @param name
     */
    public void createWorkspace(String name) {
        // Create a new namespace with Global as the parent
        // NameSpace ws = new NameSpace(globalNs, name);

        // We set UTILITY as the parent, NOT global.
        // This gives the workspace the tools, but isolates it from Global's data.
        NameSpace ws = new NameSpace(utilityNs, name);
        workspaces.put(name, ws);
    }

    // TODO this has not been tested yet!!
    /*
     * Next Up: Implementing the Utility Parent hierarchy to keep your tools
     * available in every workspace without cluttering the data.
     * 
     * When you're ready to pick this back up, would you like me to help you wrap
     * this migration logic into a "Bootstrap" class that fires whenever the
     * environment starts?
     * 
     */
    public void migrateCommands(NameSpace source, NameSpace destination) {
        // 1. Migrate Methods (commands like print, cls, etc.)
// This is gemini's first version; compare with second version before final testing/commit
//        String[] methodNames = source.getMethodNames(); //.getDeclaredMethodNames();
//        for (String mName : methodNames) {
//            try {
//                // We only migrate methods that aren't internal BeanShell hooks
//                if (!mName.startsWith("_") && !mName.equals("main")) {
//                    BshMethod[] methods = source.getMethods(mName, new Class[0], true);
//                    for (BshMethod m : methods) {
//                        destination.setMethod(m.getName(), m); // .insertMethod(m);
//                    }
//                }
//            } catch (UtilEvalError e) {
//                System.err.println("Could not migrate method: " + mName);
//            }
//        }
///////////////
        String[] methodNames = source.getMethodNames();
        for (String mName : methodNames) {
            // We avoid internal hooks to keep the utility namespace 'clean'
            if (!mName.startsWith("_") && !mName.equals("main")) {
                try {
                    // Note: Use getMethod(name, paramTypes)
                    // We'll try to get the no-arg version or the most common version
                    BshMethod m = source.getMethod(mName, new Class[0]);
                    if (m != null) {
                        destination.setMethod(m.getName(), m);
                    }

                } catch (UtilEvalError e) {
                    System.err.println("Could not migrate method: " + mName);
                }
            }
        }

        // 2. Migrate Core Objects (like wm, db, etc.)
        // Note: You may want to be selective here so you don't move "data"
        String[] varNames = source.getVariableNames();
        for (String vName : varNames) {
            if (vName.equals("wm") || vName.equals("bsh")) {
                try {
                    destination.setVariable(vName, source.getVariable(vName), false);
                } catch (UtilEvalError e) {
                    System.err.println("Could not migrate variable: " + vName);
                }
            }
        }
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

        NameSpace target = "global".equalsIgnoreCase(name) ? globalNs : workspaces.get(name);
        if (target != null) {
            // This now triggers the JJTree reset and field update in one go
            masterInterpreter.setNameSpace(target);

            this.activeName = name;
            System.out.println("SYNC: Swapped to " + name + " [Hash: " + System.identityHashCode(target) + "]");
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

    /* =============== sandbox features ============= */

    public void createSandbox(String name) {
        // Passing null as the parent creates a root-level namespace
        // It will NOT see global variables or methods.
        NameSpace sandbox = new NameSpace((NameSpace)null, name);
        
        // Note: You may need to manually load default imports if 
        // you want basic Java types (String, etc.) available.
        // sandbox.loadDefaultImports(); 
        
        workspaces.put(name, sandbox);
    }    
    
    /* ======== test script =============

print("Scripted Tool sees wm as: " + System.identityHashCode(wm));

// 1. Initial State - Define something in Global
x = 100;

// 2. Create and switch to a scratchpad
wm.createWorkspace("test1");
wm.switchTo("test1");

// 3. Local Mutation
var x = 200; // This no longer shadows the global 'x'
tempFunc() { return "I only exist in test1"; }

print(x); // Prints 200
print(tempFunc()); // Prints "I only exist..."

// 4. Verification - Switch back to Global
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

---------------- logic gamma ----------------------------
// console : set a baseline
print("Current Hash: " + System.identityHashCode(this.interpreter.getNameSpace()));

// script tester
wm.createWorkspace("gamma_test");
wm.switchTo("gamma_test");
logicGamma() { return "Gamma Source-Shim Success!"; }
print("Tool defined logicGamma in gamma_test");

// console
// Refresh the console's view of the 'current' namespace without setting it
print("Current Hash: " + System.identityHashCode(this.interpreter.getNameSpace()));
print(logicGamma());

// success!!! followup...

// 1. Switch back to global
wm.switchTo("global");

// 2. Exercise the shim
1+1; 

// 3. Verify Isolation (These should both fail/return null)
print("Testing isolation in Global...");

try {
    logicGamma(); 
    print("FAILURE: logicGamma is visible in global!");
} catch (Exception e) {
    print("SUCCESS: logicGamma is isolated (not found in global).");
}

// 4. Switch back to gamma_test
wm.switchTo("gamma_test");
1+1;

// 5. Verify Persistence
print("Result in gamma_test: " + logicGamma());

----------- failure followup ------------
// 1. Is this the original global?
print("Current Hash: " + System.identityHashCode(this.interpreter.getNameSpace()));
// Compare this to your very first hash (2089204349). 
// If it matches, then the object IS the same, but its CONTENTS were wiped.
// If it is different, we have a "Namespace Collision" where two objects are named 'global'.

// 2. Look for the 'wm' object
print("Is wm here? " + (this.interpreter.get("wm") != null));

// 3. Check Parentage
print("Parent of current: " + this.interpreter.getNameSpace().getParent());







    */

}

