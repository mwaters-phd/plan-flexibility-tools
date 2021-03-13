import argparse
import os
import platform
import subprocess
import time
from pathlib import Path

JAVA_MAIN = "au.rmit.agtgrp.pplib.pp.mktr.MktrMain"
JAVA_VM_ARGS = "-Xmx8G"
JAVA_CLASSPATH = "./lib/pplib-0.1.1.jar:./lib/args4j-2.33.jar:./lib/libtw.jar:./lib/pddl4j-3.5.0.jar"

TEMP_DIR = "./temp"

SEP = "************************************************"


def print_header(header: str):
    print("\n{}\n** {}\n{}".format(SEP, header, SEP))


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--dfile", help="Domain file name (eg domain.pddl)", required=True)
    parser.add_argument("--ifile", help="Problem instance file name (eg problem_02.pddl)", required=True)
    parser.add_argument("--pfile", help="Plan file (eg problem.pddl.m)", required=True)
    parser.add_argument("--tw", help="Treewidth", required=True)
    parser.add_argument("--pol", help="Relaxation policy", required=True)
    parser.add_argument("--time", help="Time limit in minutes", type=int, default=30)
    parser.add_argument("--validate", help="Validate final POP", action='store_true')
    parser.add_argument("--verbose", help="Verbose mode", action='store_true')
    parser.add_argument("--count", help="Compute the final instantiation count", action='store_true')

    args = parser.parse_args()

    print("Domain file:  {}".format(args.dfile))
    print("Problem file: {}".format(args.ifile))
    print("Plan file:    {}".format(args.pfile))
    print("Treewidth:    {}".format(args.tw))
    print("Relaxation policy: {}".format(args.pol))
    print("Temp dir:    {}".format(TEMP_DIR))
    print("Time limit:   {}m".format(args.time))
    print("Memory limit: 8GB")
    print("Validate: {}".format(args.validate))
    print("Verbose:  {}".format(args.verbose))
    print("Count:    {}".format(args.count))

    #
    # Run MKTR
    #
    print_header("Running MKTR")
    java_args = [
        "java", JAVA_VM_ARGS,
        "-cp", JAVA_CLASSPATH, JAVA_MAIN,
        "--domain", args.dfile,
        "--problem", args.ifile,
        "--plan", args.pfile,
        "--policy", args.pol,
        "--tw", args.tw,
        "--mktr-time", "{}".format(args.time),
        "--optimise",
        "--temp", TEMP_DIR]
    if args.validate:
        java_args.append("--validate")
    if args.verbose:
        java_args.append("--verbose")
    if args.count:
        java_args.append("--count")

    subprocess.call(java_args)


if __name__ == "__main__":
    main()
