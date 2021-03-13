#!/usr/local/bin/python3

import argparse
import shutil
import subprocess
import time
from pathlib import Path

JAVA_MAIN = "au.rmit.agtgrp.pplib.pp.mrr.MrrMain"
JAVA_VM_ARGS = "-Xmx8G"
JAVA_CLASSPATH = "./lib/pplib-0.1.2.jar:./lib/args4j-2.33.jar:./lib/libtw.jar:./lib/pddl4j-3.5.0.jar"

TEMP_DIR = "./temp"

SEP = "************************************************"

MAXSAT_SAT = "SATISFIABLE"
MAXSAT_UNSAT = "UNSATISFIABLE"
MAXSAT_OPT = "OPTIMAL"
MAXSAT_TO = "TIMEOUT"
MAXSAT_ERR = "MAXSAT_ERROR"

ENCODING_ERR = "ENCODING_ERROR"

RESULTS_HEADERS = [
    "domain_name", "problem_name", "plan_file", "alg", "acyc", "asymm",
    "time_limit", "enc_time", "prepro_time", "maxsat_time",
    "n_props", "n_clauses", "n_symm_props", "n_symm_clauses",
    "maxsat_result", "pop_size", "pop_flex"]


def print_header(header: str):
    print("\n{}\n** {}\n{}".format(SEP, header, SEP))


def check_file(file_name: str, exit_on_no: bool = False) -> bool:
    exists = Path(file_name).is_file()
    if not exists:
        print("File does not exist: {}".format(file_name))
        if exit_on_no:
            exit(1)
    return exists


def make_dir(dir_name: str):
    path = Path(dir_name)
    if path.is_file():
        print("{} is a file".format(dir_name))
        exit(1)
    if Path(dir_name).is_dir():
        shutil.rmtree(dir_name)

    print("Making directory: {}".format(dir_name))
    path.mkdir(parents=True, exist_ok=True)


def get_time_ms() -> int:
    return int(round(time.time() * 1000))


START_TIME = get_time_ms()


def get_remaining_time(time_limit_ms: int) -> int:
    return time_limit_ms - (get_time_ms() - START_TIME)


def parse_maxpre_result(maxpre_result: str) -> str:
    maxpre_result = maxpre_result.split(" ")[1]  # remove s
    if maxpre_result == "OPTIMUM" or maxpre_result == "OPTIMAL":
        return MAXSAT_OPT
    if maxpre_result == "SATISFIABLE":
        return MAXSAT_SAT
    if maxpre_result == "UNSATISFIABLE":
        return MAXSAT_UNSAT
    if maxpre_result == "TIMEOUT" or maxpre_result == "UNKNOWN":
        return MAXSAT_TO
    if maxpre_result == "ERROR":
        return MAXSAT_ERR

    print("Cannot decode MaxSAT result: {}".format(maxpre_result))
    return ""


def write_results_field(results_file: str, field: str, result: str):
    results = []
    with open(results_file, "r") as f:
        results = f.readlines()[1].split(",")

    i = 0
    for header in RESULTS_HEADERS:
        if field == header:
            results[i] = str(result)
            break
        i += 1

    if i == len(RESULTS_HEADERS):
        print("Cannot write to results file: header {} not found".format(field))
        exit(1)

    with open(results_file, "w") as f:
        for header in RESULTS_HEADERS:
            f.write("{},".format(header))
        f.write("\n")
        i = 0
        for result in results:
            f.write("{}".format(str(result).strip()))
            if i < len(results) - 1:
                f.write(",")
            i += 1
        f.write("\n")


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--dfile", help="Domain file name (eg domain.pddl)", required=True)
    parser.add_argument("--ifile", help="Problem instance file name (eg problem_02.pddl)", required=True)
    parser.add_argument("--pfile", help="Plan file (eg problem_02.pddl.m)", required=True)
    parser.add_argument("--encoder", help="MaxSAT encoder", required=True, choices=["MD", "MR", "MR_OPSB", "MRD", "MRR", "MRR_OPSB", "MRR_CSSB"])
    parser.add_argument("--time", help="Time limit in minutes", type=float, default=30.0)
    parser.add_argument("--verbose", help="Verbose mode", action='store_true')

    args = parser.parse_args()

    domain_file = args.dfile
    problem_file = args.ifile
    plan_file = args.pfile
    results_file = "mrr-results.csv"
    alg = args.encoder
    time_limit = args.time
    time_limit_ms = time_limit * 60000
    verbose = args.verbose

    print("Domain file:  {}".format(domain_file))
    print("Problem file: {}".format(problem_file))
    print("Plan file:    {}".format(plan_file))
    print("Results file: {}".format(results_file))
    print("Temp dir:     {}".format(TEMP_DIR))
    print("Optimisation alg:  {}".format(alg))
    print("Time limit:   {}m".format(time_limit))
    print("Verbose:  {}".format(verbose))

    check_file(domain_file, exit_on_no=True)
    check_file(problem_file, exit_on_no=True)
    if not check_file(plan_file, exit_on_no=False):
        plan_file = plan_file.lower()
        check_file(plan_file, exit_on_no=True)

    make_dir(TEMP_DIR)

    wcnf_file = "{}/encoded.wcnf".format(TEMP_DIR)
    pp_wcnf_file = "{}/preprocessed.wcnf".format(TEMP_DIR)
    pp_map_file = "{}.map".format(pp_wcnf_file)
    pp_wcnf_model = "{}/pp-model.dimacs".format(TEMP_DIR)
    wcnf_model = "{}/model.dimacs".format(TEMP_DIR)

    maxsat_out = "{}/maxsat-preprocessor-out.log"
    pp_out = "{}/maxsat-solver-out.log"

    print("Initialising results file")
    with open(results_file, "w") as f:
        for header in RESULTS_HEADERS:
            f.write("{}, ".format(header))
        f.write("\n")
        f.write("{},{},{}.{},{},{},{},{},{},{},{},{},{},{},{},{},{}\n".format(
            domain_file, problem_file, plan_file, alg, " ", " ",
            int(time_limit_ms), 0, 0, 0, 
            -1, -1, -1, -1, 
            ENCODING_ERR, -1, -1))

    #
    # encode WCNF
    #
    print_header("Encoding WCNF")
    task_start_ms = get_time_ms()
    args = ["java", JAVA_VM_ARGS, "-cp", JAVA_CLASSPATH, JAVA_MAIN,
            "--time", "{}".format(time_limit),
            "--domain", domain_file,
            "--problem", problem_file,
            "--plan", plan_file,
            "--encode",
            "--out-file", results_file,
            "--wcnf-file", wcnf_file,
            "--alg", alg]
    if verbose:
        args.append("--verbose")

    subprocess.call(args)

    task_time = get_time_ms() - task_start_ms
    write_results_field(results_file, "enc_time", task_time)

    if get_remaining_time(time_limit_ms) <= 0:
        write_results_field(results_file, "maxsat_result", "ENCODER_TIMEOUT")
        print("Time elapsed")
        exit(1)

    if not check_file(wcnf_file):
        write_results_field(results_file, "maxsat_result", "ENCODER_ERROR")
        print("Encoding failed")
        exit(1)

    #
    # preprocess WCNF
    #
    print_header("Preprocessing WCNF")
    task_start_ms = get_time_ms()
    args = ["maxpre", wcnf_file, "preprocess", "-mapfile={}".format(pp_map_file), "-timelimit={}".format(int(get_remaining_time(time_limit_ms) / 1000))]
    subprocess.call(args, stdout=open(pp_wcnf_file, "w"))

    task_time = get_time_ms() - task_start_ms
    write_results_field(results_file, "prepro_time", task_time)

    if not check_file(pp_wcnf_file):
        write_results_field(results_file, "maxsat_result", "PREPRO_ERROR")
        print("Preprocessing failed")
        exit(1)

    if get_remaining_time(time_limit_ms) <= 0:
        write_results_field(results_file, "maxsat_result", "PREPRO_TIMEOUT")
        print("Time elapsed")
        exit(1)

    #
    # solve MaxSAT
    #
    print_header("Solving MaxSAT")
    task_start_ms = get_time_ms()
    args = ["timeout", str(int(get_remaining_time(time_limit_ms) / 1000)), "loandra", "-print-model", pp_wcnf_file]
    completed = subprocess.run(args, capture_output=True)
    std_out = completed.stdout.decode("utf-8")
    print(std_out)

    task_time = get_time_ms() - task_start_ms
    write_results_field(results_file, "maxsat_time", task_time)

    model_found = False
    maxsat_result = MAXSAT_ERR
    for line in std_out.split("\n"):
        if line.startswith("v"):
            with open(pp_wcnf_model, "w") as f:
                f.write("s OPTIMUM\n{}\n".format(line))
            print("Model written to {}".format(pp_wcnf_model))
            model_found = True
        if line.startswith("s"):
            maxsat_result = parse_maxpre_result(line)
            print(maxsat_result)

    write_results_field(results_file, "maxsat_result", maxsat_result)
    if not model_found or (maxsat_result != MAXSAT_SAT and maxsat_result != MAXSAT_OPT):
        print("MaxSAT failed")
        exit(1)

    #
    # undo preprocessing
    #
    print_header("Undoing preprocessing")

    args = ["maxpre", pp_wcnf_model, "reconstruct", "-mapfile={}".format(pp_map_file)]
    completed = subprocess.run(args, capture_output=True)
    std_out = completed.stdout.decode("utf-8")
    print(std_out)

    model_found = False
    for line in std_out.split("\n"):
        if line.startswith("v"):
            with open(wcnf_model, "w") as f:
                f.write(line)
            print("Model written to {}".format(wcnf_model))
            model_found = True

    if not model_found:
        write_results_field(results_file, "maxsat_result", "PREPRO_RECONSTRUCT_ERROR")
        print("Failed to undo preprocessing")
        exit(1)

    #
    # evaluate model
    #
    print_header("Decoding MaxSAT model")

    # java $VMARGS -cp $CLASSPATH $MAIN --decode --domain $DOMAIN --problem $PROBLEM --plan $PLAN --model-file $ORIGM --out-file $OUT --wcnf-file $WCNF "${@:7}"
    args = ["java", JAVA_VM_ARGS, "-cp", JAVA_CLASSPATH, JAVA_MAIN,
            "--domain", domain_file,
            "--problem", problem_file,
            "--plan", plan_file,
            "--decode",
            "--model-file", wcnf_model,
            "--out-file", results_file,
            "--wcnf-file", wcnf_file]
    if verbose:
        args.append("--verbose")

    subprocess.call(args)

    print("Results written to {}".format(results_file))


if __name__ == "__main__":
    main()
