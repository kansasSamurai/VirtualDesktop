# VirtualDesktop
==============

A raw but functional virtual desktop.
+ HyperSQL
+ BeanShell
+ JFreeChart
+ XChart
+ JCXConsole
+ ...Groovy ...muCommander
+ ...Swing ...Griffon?

This is definitely in the proof of concept stage
but I find it a personally useful tool.
In particular, the integration of BeanShell and
the recent addition of the JCXConsole are providing
some interesting use cases.

Also, even though my goal was to integrate various
tools into the same JVM/memory space, it obviously
has the drawback that one misbehaving tool can
crash the entire JVM.  Therefore, if you use this
environment to edit files, be sure that you save
often.

These two .jar files have to be manually downloaded and installed to your
local maven repository (if I determine that I can upload them as part
of the project files then I will):

mvn install:install-file -Dfile=graphicsbuilder-0.6.1.jar -DgroupId=org.codehaus.groovy-contrib -DartifactId=graphicsbuilder -Dversion=0.6.1 -Dpackaging=jar
mvn install:install-file -Dfile=jsilhouette-0.2.jar -DgroupId=org.kordamp -DartifactId=jsilhouette -Dversion=0.2 -Dpackaging=jar
mvn install:install-file -Dfile=weblaf-1.27.jar -DgroupId=com.mgarin -DartifactId=weblaf -Dversion=1.27 -Dpackaging=jar

<dependency>
    <groupId>org.codehaus.groovy-contrib</groupId>
    <artifactId>graphicsbuilder</artifactId>
    <version>0.6.1</version>
</dependency>

<dependency>
    <groupId>org.kordamp</groupId>
    <artifactId>jsilhouette</artifactId>
    <version>0.2</version>
</dependency>

<dependency>
    <groupId>com.mgarin</groupId>
    <artifactId>weblaf</artifactId>
    <version>1.27</version>
</dependency>
