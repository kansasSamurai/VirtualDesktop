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
    private Map<String, NameSpace> workspaces = new HashMap<>();

    public WorkspaceManager(Interpreter it) {
        // Capture the original global namespace
        this.globalNs = it.getNameSpace();
    }

    public void createWorkspace(Interpreter it, String name) {
        // Create a new namespace with Global as the parent
        NameSpace ws = new NameSpace(globalNs, name);
        workspaces.put(name, ws);
    }

    public void switchTo(Interpreter it, String name) {
        if (name.equals("global")) {
            it.setNameSpace(globalNs);
            activeName = "global";
            return;
        }
        NameSpace target = workspaces.get(name);
        if (target != null) {
            it.setNameSpace(target);
            activeName = name;
        }
    }

    public String getActiveWorkspaceName() {
        return activeName;
    }

    public String[] getWorkspaceNames() {
        return workspaces.keySet().toArray(new String[0]);
    }

    /* ======== test script =============

     // 1. Define something in Global
     x = 100;
    
     // 2. Create and switch to a scratchpad
     wm.createWorkspace(this.interpreter, "test1");
     wm.switchTo(this.interpreter, "test1");
    
     // 3. Pollute this workspace
     x = 200; // This shadows the global 'x'
     tempFunc() { return "I only exist in test1"; }
    
     print(x); // Prints 200
    
     // 4. Switch back to Global or a fresh workspace
     wm.resetToGlobal(this.interpreter);
     print(x); // Prints 100! The global value is untouched.
     // tempFunc(); // This would now fail 

    */

}

