import subprocess
import tempfile
import os
import sys


def start(working_dir, inventory):
    temp_dir = tempfile.mkdtemp(dir=working_dir, prefix="tmp_a_")

    executable = "java"
    jar = "/data/projects/Annotations/lib/annotation.jar"
    main = "org.forome.annotation.annotator.main.AnnotatorMainFork"
    annotator_cfg = "/data/projects/Annotations/lib/bgm.json"

    args = ["-cp", jar, main, "-config", annotator_cfg, "-inventory", inventory]

    child_pid = os.fork()
    if (child_pid):
        print("Starting child process: {} => {}".format(str(os.getpid()),
                                                        str(child_pid)))
        sys.exit(0)
    else:
        os.chdir(temp_dir)
        pid = os.getpid()
        sys.stdout = open("out-{}.log".format(str(pid)), "w")
        sys.stderr = open("err-{}.log".format(str(pid)), "w")
        # args = ["-version"]
        print("Executing {} {}".format(executable, " ".join(args)))
        # os.execvp(executable, args)
        subprocess.call([executable] + args, stdout=sys.stdout,
                        stderr=sys.stderr)


if __name__ == '__main__':
    start(os.getcwd(), sys.argv[1])