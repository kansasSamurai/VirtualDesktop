/**
 * #!/usr/bin/env groovy
 *
 * applyBuildFileInGroovy.groovy
 *
 * This is an example of executing an Ant build file from Groovy code using
 * Ant's Project and ProjectHelper classes.
 *
 * Usage: applyBuildFileInGroovy.groovy _buildFilePathName_ [target1] [target2] ... [targetn]
 *
 * where _buildFilePathName_ is the path and file name of the build file to be
 * used by this script and zero or more targets in that build file can be
 * specified (default target used if no targets specified).
 */

import org.apache.tools.ant.Project
import org.apache.tools.ant.ProjectHelper

// If running as an actual command line script, comment out the following:
args = String[] {"build.xml"}
if (args.length < 1) {
   println "You must provide an Ant build file as the first parameter."
   System.exit(-1)
}

def antBuildFilePathAndName = args[0]
def antFile = new File(antBuildFilePathAndName)

def project = new Project()
project.init()

ProjectHelper.projectHelper.parse(project, antFile)
if (args.length > 1) {
   def antTargets = args - antBuildFilePathAndName
   antTargets.each {
      project.executeTarget(it)
   }
} else {
   // use default target because no targets were specified on the command line
   project.executeTarget(project.defaultTarget);
}
