//> Classes lox-class
package com.craftinginterpreters.lox;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/* Classes lox-class < Classes lox-class-callable
class LoxClass {
*/
//> lox-class-callable
class LoxClass extends LoxInstance implements LoxCallable {
//< lox-class-callable
  final String name;
//> Inheritance lox-class-superclass-field
  final LoxClass superclass;
//< Inheritance lox-class-superclass-field
/* Classes lox-class < Classes lox-class-methods

  LoxClass(String name) {
    this.name = name;
  }
*/
//> lox-class-methods
  private final Map<String, LoxFunction> methods;
  private final Map<String, LoxFunction> statMethods;

/* Classes lox-class-methods < Inheritance lox-class-constructor
  LoxClass(String name, Map<String, LoxFunction> methods) {
*/
//> Inheritance lox-class-constructor
  LoxClass(String name, LoxClass superclass,
           Map<String, LoxFunction> methods, Map<String, LoxFunction> statMethods) {
    super();
    this.superclass = superclass;
//< Inheritance lox-class-constructor
    this.name = name;
    this.methods = methods;
    this.statMethods = statMethods;
  }
//< lox-class-methods
//> lox-class-find-method
  LoxFunction findMethod(LoxInstance instance, String name) {
    //inheritance change added
    LoxFunction outerMethod = null;//parent
    LoxFunction innerMethod = null;//children
    LoxClass theClass = this;// current class running method based on input
    boolean hasAClass = true;
    //set boolean as true to say that there is a class to check for, but if it doesn't then the actual class is set to null and stops loop

    while(hasAClass) {//true turns to false if no class is there, aka null
        if (theClass == null) {
            hasAClass = false;
        }
        else if (theClass.methods.containsKey(name)) {//if class has method with name
            innerMethod = outerMethod;//have outer method turn to inner method
            outerMethod = theClass.methods.get(name);//gets method from current class and sets it as outer method//current method becomes outer method
            theClass = theClass.superclass;// the following class is then considered, moved up to next parent class
            //return methods.get(name);
            // loop repeats the steps if there is a class to look for
        }
        else{
            theClass = theClass.superclass;

        }
//> Inheritance find-method-recurse-superclass
    }
    if(outerMethod != null){//if an outer method
        return outerMethod.bind(instance, innerMethod);//connect outer method to current obj and give outer method the inner method and its contents to be called
        // outer method gets a reference to inner method, can execute what is in inner method
    }
//< Inheritance find-method-recurse-superclass
    return null;
  }
//< lox-class-find-method

  @Override
  public String toString() {
    return name;
  }
//> lox-class-call-arity
  @Override
  public Object call(Interpreter interpreter,
                     List<Object> arguments) {
    LoxInstance instance = new LoxInstance(this);//obj
//> lox-class-call-initializer
    LoxFunction initializer = findMethod(instance, "init");
    if (initializer != null) {
      initializer.call(interpreter, arguments);//already bound, so called instead
    }

//< lox-class-call-initializer
    return instance;
  }

  @Override
  public int arity() {
/* Classes lox-class-call-arity < Classes lox-initializer-arity
    return 0;
*/
//> lox-initializer-arity
    //added instance like call method
    LoxInstance instance = new LoxInstance(this);//obj
    LoxFunction initializer = findMethod(instance,"init");
    if (initializer == null) return 0;
    return initializer.arity();
//< lox-initializer-arity
  }
//< lox-class-call-arity
    //get looks at dot from class and looks for name in static method, but this time lox handles get to search for static methods rather than normal methods
    Object get(Token name)//overriden method from lox Instance bc we need to implement get for static methods
    {
     if(statMethods.get(name.lexeme) == null)
        {
            throw new RuntimeError(name, // [hidden]
                "Undefined property '" + name.lexeme + "'.");
        }
     else{
        return statMethods.get(name.lexeme);
     }

}
}
