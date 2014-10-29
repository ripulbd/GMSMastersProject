#!/usr/bin/python
# $Id: pom2lib.py 18 2009-07-01 19:29:56Z spal $
# Simple python script to read the pom.xml and copy over JAR files from
# my local maven repository to a target directory. Also generates a shell
# script to move these files back to a local maven repository on a target
# machine.
# Usage Examples:
# 1) to copy from local M2_REPO to the jtmt lib directory:
# ./pom2lib.py -s /home/sujit/src/jtmt/pom.xml /home/sujit/src/jtmt/lib
# 2) to copy from jtmt lib directory to the local M2_REPO:
# ./pom2lib.py -r /home/sujit/src/jtmt/pom.xml /home/sujit/src/jtmt/lib
# Remember to set the M2_REPO appropriately for your system.
#
import sys
import getopt
import os.path
import os
from elementtree import ElementTree
import string
import shutil

POM_NS = "{http://maven.apache.org/POM/4.0.0}"
M2_REPO = "/home/sujit/.m2/repository"

def usage(error=None):
  if (error != None):
    print "ERROR: %s" % (error)
  print "Usage:"
  print "pom2lib.py [-s|-r|-h] pom_file lib_dir"
  print "where:"
  print "-s|--store - copy jar files from your m2 repo to target"
  print "-r|--retrieve - copy jar files from target to your m2 repo"
  print "-h|--help - print this message"
  print "pom_file - the full path to the pom.xml file"
  print "lib_dir - the full path to the jar directory"
  sys.exit(-1)

def contains(opts, patterns):
  return len(filter(lambda x: x[0] in patterns, opts)) == 1

def buildPath(props):
  """
  Return a pair containing the absolute file names of the jar file and
  the corresponding src-jar file in the user's M2_REPO. There is no check
  at this stage to verify that the src-jar exists (it may not in some cases)
  """
  groupId = props["%sgroupId" % (POM_NS)]
  artifactId = props["%sartifactId" % (POM_NS)]
  version = props["%sversion" % (POM_NS)]
  jarpath = os.path.join(M2_REPO,
    string.replace(groupId, ".", os.sep),
    artifactId,
    version,
    "".join([artifactId, "-", version, ".jar"]))
  srcJarpath = os.path.join(M2_REPO,
    string.replace(groupId, ".", os.sep),
    artifactId,
    version,
    "".join([artifactId, "-", version, "-sources.jar"]))
  return (jarpath, srcJarpath)
  
def parse(pom):
  """
  Parses the POM to get a list of file path pairs returned by buildPath()
  """
  paths = []
  tree = ElementTree.parse(pom)
  for dependency in tree.findall("//%sdependency" % (POM_NS)):
    props = {}
    for element in dependency:
      props[element.tag] = element.text
    paths.append(buildPath(props))
  return paths

def copyToLib(pathpairs, libdir):
  """
  Copies the jar file and the source jar file (if it exists) into the
  libdir.
  """
  if (not os.path.exists(libdir)):
    print "mkdir -p %s" % (libdir)
    os.makedirs(libdir)
  for (jar, srcjar) in pathpairs:
    print "cp %s %s" % (jar, libdir)
    shutil.copy(jar, libdir)
    if (os.path.exists(srcjar)):
      print "cp %s %s" % (srcjar, libdir)
      shutil.copy(srcjar, libdir)

def copyFromLib(libdir, pathpairs):
  """
  Copy the jars from libdir to the local M2_REPO of a target machine.
  """
  for (jarpath, srcJarpath) in pathpairs:
    src = os.path.join(libdir, os.path.basename(jarpath))
    targetdir = os.path.dirname(jarpath)
    if (os.path.exists(src)):
      if (not os.path.exists(targetdir)):
        print "mkdir -p %s" % (targetdir)
        os.makedirs(targetdir)
      print "cp %s %s" % (src, targetdir)
      shutil.copy(src, targetdir)
    src = os.path.join(libdir, os.path.basename(srcJarpath))
    if (os.path.exists(src)):
      print "cp %s %s" % (src, targetdir)
      shutil.copy(src, targetdir)

def main():
  """
  Input validation and dispatch to the appropriate method.
  """
  try:
    (opts, args) = getopt.getopt(sys.argv[1:], "srh",
      ["store", "retrieve", "help"])
  except getopt.GetoptError:
    usage()
  if (contains(opts, ("-h", "--help"))):
    usage()
  if (len(args) != 2):
    usage("Lib directory and/or POM path must be specified")
  if (not os.path.exists(args[0])):
    usage("POM file not found: %s" % (args[0]))
  if (contains(opts, ("-s", "--store"))):
    copyToLib(parse(args[0]), args[1])
  if (contains(opts, ("-r", "--retrieve"))):
    if (not os.path.exists(args[1])):
      usage("Lib directory not found: %s" % (args[1]))
    copyFromLib(args[1], parse(args[0]))
  
if __name__ == "__main__":
  main()
