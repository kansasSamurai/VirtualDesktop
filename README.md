# VirtualDesktop
A raw but functional virtual desktop - to be clear, this is not a "remote desktop".
This is an application like any other... it just provides features that your desktop provides.
So why create an app that duplicates features I already have?... One simple answer... control! :)

Though I do have bigger plans for this application, this idea started because:

1) I am tired of Winblows arbitrarily reorganizing my desktop space.  I do keep many shortcuts
on my desktop (98% of them are to applications with the occasional link to a document).
Windows likes to randumbly reorganize my icon placements and I spend another 20-30 minutes
of my life that I will never get back putting them back.  I finally thought to myself "hey...
I'm a programmer... I'll just create my own desktop!"

2) Building on #1, ... I also use other operating systems both personally and at work.
This application is another step for me having a consistent heterogenous desktop metaphor
and I am no longer burdened with learning someone else's preferred desktop paradigm.
[Though I prefer Linux to Winblows, I *am* talking about linux here and its cornucopia of desktop options.
Some of them are pretty good but they also change based on linux distro and even at the whim of the 
development staff.  I *just* need to open apps and docs people! :) ]

3) Finally, having a multi-platform Java based desktop provides me the opportunity to integrate some often
used development packages.  This is/was the original list... it will probably be pruned/altered soon but
thought I would share the original idea:

+ HyperSQL
+ BeanShell
+ JFreeChart
+ XChart
+ JCXConsole
+ ...Groovy ...muCommander
+ ...Swing ...Griffon?

## Overview
This is definitely in the proof of concept stage
but I find it a personally useful tool.
In particular, the integration of BeanShell and
the recent addition of the JCXConsole are providing
some interesting use cases.

Also, even though my goal was to integrate various
tools into the same JVM/memory space, it obviously
has the drawback that one misbehaving tool can
crash the entire JVM.  Therefore, if you use this
environment to edit files, be sure that you save often.

## JDK11

Due to the current version of Groovy being used, the Groovy console will throw
the following exception (via SpecGroovyConsole):

```
Caused by: java.lang.reflect.InaccessibleObjectException: 
Unable to make jdk.internal.loader.ClassLoaders$AppClassLoader(jdk.internal.loader.ClassLoaders$PlatformClassLoader,jdk.internal.loader.URLClassPath) accessible: 
module java.base 
	does not "opens jdk.internal.loader" 
	to unnamed module @3fd08df9
```

This appears to be a relatively common issue with legacy APIs as 
explained on the following web pages:

* [https://docs.oracle.com/javase/9/migrate/toc.htm]
* [https://issues.apache.org/jira/browse/GROOVY-9390]
* [https://docs.microsoft.com/en-us/azure/java/jdk/transition-from-java-8-to-java-11#warning-an-illegal-reflective-access-operation-has-occurred]

If calling from the command line, use the JAVA_OPTS syntax below...
If calling via and Eclipse project, add to the 'VM arguments' textarea on the Run configuration: 

```
JAVA_OPTS=--add-opens=java.base/jdk.internal.loader=ALL-UNNAMED
VM arguments:  --add-opens=java.base/jdk.internal.loader=ALL-UNNAMED
```


## Dependencies
These four(4) .jar files have to be manually downloaded and installed to your
local maven repository (if I determine that I can upload them as part
of the project files then I will):

```
mvn install:install-file -Dfile=weblaf-1.29.jar -DgroupId=com.mgarin -DartifactId=weblaf -Dversion=1.29 -Dpackaging=jar
mvn install:install-file -Dfile=jsilhouette-0.2.jar -DgroupId=org.kordamp -DartifactId=jsilhouette -Dversion=0.2 -Dpackaging=jar
mvn install:install-file -Dfile=graphicsbuilder-0.6.1.jar -DgroupId=org.codehaus.groovy-contrib -DartifactId=graphicsbuilder -Dversion=0.6.1 -Dpackaging=jar
mvn install:install-file -Dfile=dans-dbf-lib-1.0.0-beta-10.jar -DgroupId=nl.knaw.dans.common -DartifactId=dans-dbf-lib -Dversion=1.0.0-beta-10 -Dpackaging=jar
```

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
