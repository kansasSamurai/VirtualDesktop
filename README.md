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
