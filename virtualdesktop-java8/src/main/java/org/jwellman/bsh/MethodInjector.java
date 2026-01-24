package org.jwellman.bsh;

import bsh.*;

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

    private static void performInjection(Interpreter it, NameSpace parent, NameSpace target, String script) throws EvalError {
        NameSpace tempNs = new NameSpace(parent, "InjectionTemp");
        it.eval(script, tempNs);
        
        for (BshMethod m : tempNs.getMethods()) {
            // Shadow Check: Does a variable already exist with this name?
            try {
                Object existing = target.getVariable(m.getName());
                if (existing != Primitive.VOID) {
                    System.err.println("Warning: Injected method '" + m.getName() + 
                                       "' is shadowed by an existing variable.");
                }
                target.setMethod(m.getName(), m);
            } catch (UtilEvalError e) { /* Ignore */ }
            
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
