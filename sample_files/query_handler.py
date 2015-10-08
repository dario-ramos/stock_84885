#!/usr/bin/env python

import os
import sys
import subprocess

print sys.argv[1]
count = int(sys.argv[1]) + 1
classpath = os.getcwd() + os.path.sep + "*"
for x in range(1, count):
    print "Launching shipping %d..." % (x)
    subprocess.Popen(["java","-cp",classpath,"stock84885queryhandler.Stock84885QueryHandler",str(x)])
