// ------------------------------------------------------------------------------
// the original version of this sample Gant file was found in the great article at
// https://www.javaworld.com/article/2077831/groovy-power-automated-builds-with-gant.html
//
// Note:  As of 1/4/2020, this file will not work directly from the Groovy Console
//
// ------------------------------------------------------------------------------

Ant.property(file: 'build.properties')

// now you can access a property, e.g., using a Groovy GString notation;
// this property can even be an Ant property defining the GROOVY_HOME environment variable;
def antProperty = Ant.project.properties // define a "shortcut"

// GROOVY_ADD_ONS is used in classpath definitions later on
final GROOVY_ADD_ONS = "${antProperty.'groovy.home'}/../groovy-add-ons"

//def wsdlRootDir = new File(antProperty.'wsdl.root.dir')

println("project.copyright is >> " + antProperty.project.copyright)

target ('default': 'print gant usage') {
    println """
    USAGE:
    gant ...

    Produced listings in your 'results' directory:
        output-<tool>.txt & error-<tool>.txt with infos/errors/warnings for the code generation
    """
}

target (init: 'An initialization target') {
    println init_description // Gant design rule
    Ant.echo ( message : "A message from Ant.echo()" )
}
