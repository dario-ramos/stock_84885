#!/usr/bin/env python

import os
import sys
import subprocess

print sys.argv[1]
count = int(sys.argv[1]) + 1
classpath = os.getcwd() + os.path.sep + "*"
for x in range(1, count):
    print "Launching order receiver %d..." % (x)
    subprocess.Popen(["java","-cp",classpath,"stock84885customer.Stock84885Customer",str(x),"query"])
