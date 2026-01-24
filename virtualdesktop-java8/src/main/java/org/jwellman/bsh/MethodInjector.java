package org.jwellman.bsh;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import bsh.BshMethod;
import bsh.EvalError;
import bsh.Interpreter;
import bsh.NameSpace;
import bsh.This;
import bsh.UtilEvalError;

/**
 * A utility to help dynamically alter beanshell closures.
 * 
 * @author rwellman
 *
 */
public class MethodInjector {

    // Version 1: Static Injection (Global Scope)
    /**
     * 
     * Original test script:
        myClosure() { 
            print("Initial state"); 
            return this; 
        };
        c = myClosure();
        
        // Later, inject a new capability
        addMethod(c, "void sayHello() { print(\"Hello from injected method!\"); }");
        
        myClosure.sayHello(); // This will now work!
        
        -----------------------------------------
        Test 1: Examining inject() (The "Clean Room")
        This should fail if the method relies on closure-internal data during definition.        

        // Setup
        myObj = { int secret = 42; return this; };
        
        // This will likely FAIL because 'secret' isn't in the global scope 
        // where the method is being initially parsed/evaluated.
        try {
            inject(myObj, "getSecret() { return secret; }");
        } catch (Exception e) {
            print("Inject failed as expected: " + e.getMessage());
        }

     *
     */
    public static void inject(Interpreter interpreter, This target, String methodDef) throws EvalError {
        NameSpace global = interpreter.getNameSpace();
        performInjection(interpreter, global, target.getNameSpace(), methodDef);
    }

    // Version 2: Contextual Injection (Closure Scope)
    /**
     * Injects a method defined in a script string into an existing BeanShell 'This'
     * object.
     * 
     * --------------------------------------
        // Test 2: Examining extendClosure() (The "Live Patch")
        // This should succeed because it uses the closure as the parent during evaluation.

        myObj = { int secret = 42; return this; };

        // This succeeds because the evaluator 'sees' secret via the targetNs parent link.
        extendClosure(myObj, "getSecret() { return secret; }");
        
        print("Secret value: " + myObj.getSecret()); // Prints 42
        
        ----------------------------------------
        // Test 3: The Shadowing Scenario        
        
        myObj = { int val = 100; return this; };

        // Injecting a method with the same name as a variable
        extendClosure(myObj, "void val() { print('I am a method'); }");
        
        // BeanShell resolution priority: 
        // Calling myObj.val() might return the integer 100 instead of executing the method!
        print("Result of myObj.val: " + myObj.val);

     */
    public static void extendClosure(Interpreter interpreter, This target, String methodDef) throws EvalError {
        NameSpace targetNs = target.getNameSpace();
        performInjection(interpreter, targetNs, targetNs, methodDef);
    }

    private static void performInjection(Interpreter it, NameSpace parent, NameSpace target, String script)
            throws EvalError {
        NameSpace tempNs = new NameSpace(parent, "InjectionTemp");
        it.eval(script, tempNs);

        for (BshMethod newMethod : tempNs.getMethods()) {
            String name = newMethod.getName();
            Class[] newParams = newMethod.getParameterTypes();

            // Get existing methods of the SAME name
            BshMethod[] existingMethods = getMethodsByName(target, name);

            if (existingMethods.length == 0) {
                // Easy case: Just set it
                try {
                    target.setMethod(name, newMethod);
                } catch (UtilEvalError e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

            } else {
                // Overloading case: Build a new list of methods
                List<BshMethod> updatedList = new ArrayList<>();
                boolean replaced = false;

                for (BshMethod existing : existingMethods) {
                    // Check if signatures match exactly
                    if (Arrays.equals(existing.getParameterTypes(), newParams)) {
                        updatedList.add(newMethod); // Replace existing overload
                        replaced = true;
                    } else {
                        updatedList.add(existing); // Keep existing overload
                    }
                }

                if (!replaced) {
                    updatedList.add(newMethod); // Add as a brand new overload
                }

                // Re-inject the entire set of overloads
                // Note: setMethod for multiple overloads requires looping or internal API
                // access
                // In 2.0b5, we have to clear and re-add or use the direct NameSpace.methods Map
                // but since setMethod() usually overwrites, we clear first if needed:
                for (BshMethod m : updatedList) {
                    try {
                        target.setMethod(m.getName(), m);
                    } catch (UtilEvalError e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private static BshMethod[] getMethodsByName(NameSpace ns, String name) {
        try {
            return ns.getMethods(); // Get all, then filter
        } catch (Exception e) {
            return new BshMethod[0];
        }
    }

    /**
     * Removes all overloads of a method by name.
     */
    public static void removeMethod(This target, String methodName) {
        NameSpace ns = target.getNameSpace();
        // Passing null or an empty array to an internal setter might not work, 
        // so we manually clear it from the namespace's internal map.
        try {
            // We use reflection or internal access if setMethod(name, null) fails
            ns.setMethod(methodName, null); 
        } catch (Exception e) {
            System.err.println("Could not remove method: " + e.getMessage());
        }
    }

    /**
     * Removes a specific overload by matching parameter types.
     * @throws UtilEvalError 
     */
    public static void removeMethodSignature(This target, String methodName, Class[] paramTypes) throws EvalError, UtilEvalError {
        NameSpace ns = target.getNameSpace();
        BshMethod[] existing = ns.getMethods();
        
        // Clear the method name first
        ns.setMethod(methodName, null);

        // Re-add everything EXCEPT the one we want to remove
        for (BshMethod m : existing) {
            if (m.getName().equals(methodName)) {
                if (!Arrays.equals(m.getParameterTypes(), paramTypes)) {
                    ns.setMethod(m.getName(), m);
                }
            } else {
                // It's a different method entirely, leave it alone
                // (Though ns.getMethods() returns everything, 
                // setMethod(name, null) only cleared the specific name)
            }
        }
    }
    
    /*

// ----------------------------------------
myClosure() { 
    print("Initial state"); 
    return this; 
};
c = myClosure();
c.sayHello(); // fails as expected: EvalError: Method sayHello() not found in bsh scripted object

// Later, inject a new capability
inject(c, "void sayHello() { print(\"Hello from injected method!\"); }");

c.sayHello(); // This will now work!

// ----------------------------------------
// Test 1: Examining inject() (The "Clean Room")
// This should fail if the method relies on closure-internal data during definition.        

// Setup
t1() { int secret = 42; return this; };
myObject = t1();
// This will likely FAIL because 'secret' isn't in the global scope 
// where the method is being initially parsed/evaluated.
try {
    // this did NOT fail as expected because ...
     ... BeanShell methods use late binding (lazy resolution
     In many languages, referencing an undefined variable during a function definition 
     triggers a compiler error. In BeanShell, the method is stored as 
     an Abstract Syntax Tree (AST). It doesn't actually try to resolve secret 
     until the moment you call myObject.getSecret(). If you were to call it, 
     it would search the method's local scope, then the closure's scope, 
     and finally the global scope.
     
    inject(myObject, "getSecret() { return secret; }");
} catch (Exception e) {
    print("Inject failed as expected: " + e.getMessage());
}

// ----------------------------------------
// Test 2: Examining extendClosure() (The "Live Patch")
// This should succeed because it uses the closure as the parent during evaluation.

t2() { int secret = 42; return this; };
myObj = t2();

// This succeeds because the evaluator 'sees' secret via the targetNs parent link.
extend(myObj, "getSecret() { return secret; }");

print("Secret value: " + myObj.getSecret()); // Prints 42

// ----------------------------------------
// Test 3: The Shadowing Scenario        

t3() { int val = 100; return this; };
myObj = t3();

// Injecting a method with the same name as a variable
extend(myObj, "val() { print(\"I am a method\"); }");

// BeanShell resolution priority: 
// Calling myObj.val() does return the integer 100 instead of executing the method!
print("Result of myObj.val: " + myObj.val);

// Now call the new method...
print("Result of myObj.val: " + myObj.val());
// you get the following in the console:
//I am a method
//Result of myObj.val: void

// ----------------------------------------
// Test 4: Injecting an overload
 
// 1. Setup a closure with one method
t4() { 
    add(int a) { return a + 1; }
    return this; 
};
c = t4();

// 2. Inject an OVERLOAD (different signature)
extend(c, "add(int a, int b) { return a + b; }");

// 3. Test both
print("Test Single: " + c.add(10));    // Should print 11
print("Test Double: " + c.add(10, 5)); // Should print 15

// ----------------------------------------
// Test 5: Removing methods

// 1. Setup
t5() { 
    work() { print("Working..."); }
    work(int x) { print("Working " + x + " times..."); }
    return this; 
};
c = t5();

// 2. Remove the specific 'int' overload
// Note: In BeanShell, you might need to pass the Class objects 
// for the types you want to match.
rms(c, "work", new Class[] { int.class });
// this "works" but removes all by name which is not intended behavior; fix later

// 3. Verify
c.work();    // Should still work
try {
    c.work(5); // Should fail: Method not found
} catch (e) {
    print("Overload successfully removed!");
}

     */
    
//    public static void inject(Interpreter interpreter, This target, String script) throws EvalError {
//        // Evaluate the script in a temporary namespace to "capture" the method object
//        NameSpace tempNs = new NameSpace(interpreter.getNameSpace(), "temp");
//        interpreter.eval(script, tempNs);
//
//        // Grab the method from the temp namespace
//        BshMethod[] methods = tempNs.getMethods();
//        if (methods.length > 0) {
//            // Inject it into the target closure's namespace
//            try {
//                target.getNameSpace().setMethod(methods[0].getName(), methods[0]);
//            } catch (UtilEvalError e) {
//                // TODO Auto-generated catch block
//                e.printStackTrace();
//            }
//        }
//    }

//    public static void extendClosure(Interpreter interpreter, This closure, String methodDef) throws EvalError {
//        NameSpace targetNs = closure.getNameSpace();
//
//        // 1. Create a temporary namespace to parse the new method
//        NameSpace tempNs = new NameSpace(targetNs, "InjectionNamespace");
//
//        // 2. Evaluate the method definition in the temp namespace
//        // This populates tempNs with the BshMethod object
//        interpreter.eval(methodDef, tempNs);
//
//        // 3. Move the methods from temp to the target
//        try {
//            BshMethod[] methods = tempNs.getMethods();
//            for (BshMethod m : methods) {
//                targetNs.setMethod(m.getName(), m);
//            }
//        } catch (UtilEvalError e) {
//            // TODO Auto-generated catch block
//            e.printStackTrace();
//        }
//
//    }
    
}
