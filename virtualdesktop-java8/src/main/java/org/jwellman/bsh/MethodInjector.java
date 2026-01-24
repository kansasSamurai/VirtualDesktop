package org.jwellman.bsh;

import bsh.*;

/**
 * A utility to help dynamically alter beanshell closures.
 * 
 * @author rwellman
 *
 */
public class MethodInjector {

    /**
     * 
     * 
        myClosure() { 
            print("Initial state"); 
            return this; 
        };
        c = myClosure();
        
        // Later, inject a new capability
        addMethod(c, "void sayHello() { print(\"Hello from injected method!\"); }");
        
        myClosure.sayHello(); // This will now work!

     *
     */
    public static void inject(Interpreter interpreter, This target, String script) throws EvalError {
        // Evaluate the script in a temporary namespace to "capture" the method object
        NameSpace tempNs = new NameSpace(interpreter.getNameSpace(), "temp");
        interpreter.eval(script, tempNs);

        // Grab the method from the temp namespace
        BshMethod[] methods = tempNs.getMethods();
        if (methods.length > 0) {
            // Inject it into the target closure's namespace
            try {
                target.getNameSpace().setMethod(methods[0].getName(), methods[0]);
            } catch (UtilEvalError e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    /**
     * Injects a method defined in a script string into an existing BeanShell 'This'
     * object.
     */
    public static void extendClosure(Interpreter interpreter, This closure, String methodDef) throws EvalError {
        NameSpace targetNs = closure.getNameSpace();

        // 1. Create a temporary namespace to parse the new method
        NameSpace tempNs = new NameSpace(targetNs, "InjectionNamespace");

        // 2. Evaluate the method definition in the temp namespace
        // This populates tempNs with the BshMethod object
        interpreter.eval(methodDef, tempNs);

        // 3. Move the methods from temp to the target
        try {
            BshMethod[] methods = tempNs.getMethods();
            for (BshMethod m : methods) {
                targetNs.setMethod(m.getName(), m);
            }
        } catch (UtilEvalError e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }
    
}
