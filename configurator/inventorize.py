import argparse
import glob
import json
import os

if __name__ == '__main__':
    parser = argparse.ArgumentParser(
        description="Annotate VCF file with VEP and output results as JSON")
    parser.add_argument("-i", "--input", dest="input", help="VCF")
    parser.add_argument("-o", "--output", dest="output", help="Output file")
    parser.add_argument("-c", "--case", dest="case",
                        help="Case name, default is determined from directory name")
    parser.add_argument("-d", "--dir", dest="dir", help="Work directory",
                        default=os.getcwd())
    parser.add_argument("-p", "--platform", dest="platform",
                        help="Platform: wes/wgs/panel")
    parser.add_argument("-r", "--reuse", action='store_true',
                        help="resue intermediate files from previous run")

    args = parser.parse_args()
    print(args)

    working_dir = args.dir
    case = args.case if args.case else os.path.basename(working_dir).split('_')[0]
    if (args.input):
        input_file = args.input
    else:
        vcfs = glob.glob(os.path.join(working_dir, "*vcf*".format(case)))
        vcfs = sorted([vcf for vcf in vcfs if not vcf.endswith('idx')])
        if len(vcfs) == 0:
            raise Exception("No VCF files are found in {}".format(working_dir))
        elif len(vcfs) == 1:
            input_file = os.path.basename(vcfs[0])
        elif len(vcfs) == 2 and vcfs[1] == vcfs[0] + ".gz":
            input_file = os.path.basename(vcfs[0])
        else:
            vcfs = glob.glob(os.path.join(working_dir, "*{}*vcf*".format(case)))
            vcfs = sorted([vcf for vcf in vcfs if not vcf.endswith('idx')])
            if len(vcfs) == 2 and vcfs[1] == vcfs[0] + ".gz":
                input_file = os.path.basename(vcfs[0])
            elif len(vcfs) > 1:
                raise Exception(
                    "Ambiguos VCF files are in {}: {}".format(working_dir,
                                                              ", ".join(vcfs)))
            else:
                input_file = os.path.basename(vcfs[0])

    x = input_file.lower().split('.')[0].split('_')
    if ('wgs' in x):
        raw_platform = 'wgs'
    elif ('wes' in x):
        raw_platform = 'wes'
    else:
        raw_platform = "unknown"

    if args.platform:
        platform = args.platform
    else:
        platform = raw_platform

    if (platform):
        print("Platform: {}".format(platform))
    else:
        platform = "wgs"
        print("Could not determine platform (WES or WGS), assuming: ".format(platform))

    working_dir = args.dir
    case_id = "{}_{}".format(case, platform)
    fam_file = "{}.fam".format(case)
    if (not os.path.exists(os.path.join(working_dir, fam_file))):
        raise Exception("Fam file does is not found: {}".format(fam_file))

    patient_ids_file = os.path.join(working_dir, "samples-{}.csv".format(case))
    if (not os.path.exists(patient_ids_file)):
        patient_ids_file = None
    else:
        patient_ids_file = "$"

    if (input_file.endswith(".gz")):
        print("Unpacking: ".format(input_file))
        os.system("gunzip {}".format(input_file))
        input_file = input_file[:-3]

    if (args.output):
        output = args.output
    else:
        output = "${NAME}_anfisa.json.gz"

    if (args.reuse):
        vep_json = input_file[0:-4] + ".vep.json"
    else:
        vep_json = None

    d = os.path.dirname(os.path.realpath(__file__))
    template = os.path.join(d, "template.json")
    with open(template) as f:
        config = json.load(f)

    if patient_ids_file == "$":
        config["patient-ids"] = "${DIR}/samples-${CASE}.csv"
    elif patient_ids_file:
        config["patient-ids"] = patient_ids_file
    cfg_vcf = "${DIR}/" + input_file.replace(case_id, "${NAME}").replace(case, "${CASE}")
    if (cfg_vcf != config["vcf"]):
        config["vcf"] = cfg_vcf
    if (vep_json):
        config["vep-json"] = "${DIR}/" + vep_json
    if "cnv" in config:
        cnv_file = config["cnv"]
        cnv_file = cnv_file.replace("${DIR}", working_dir)
        if (not os.path.isfile(cnv_file)):
            print("Warning: no CNV file: {}".format(cnv_file))
            del config["cnv"]
    if "docs" in config and not os.path.isdir(os.path.join(working_dir, "docs")):
        print("Warning: DOCs directory not found")
        del config["docs"]

    inventory = os.path.join(working_dir, "{}.cfg".format(case_id))

    with open(inventory, "w") as cfg:
        json.dump(config, cfg, indent=4)

    print("Inventory: " + inventory)




