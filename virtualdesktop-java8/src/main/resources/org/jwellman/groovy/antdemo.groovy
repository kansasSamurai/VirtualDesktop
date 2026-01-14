def ant = new AntBuilder()

//def sourceFilePathAndName = args[0]
//def targetFileFromAntCopyName =  "target_antToken"
def out = ant.copy(file: "C:/Users/Rick/Downloads/mlb_teams_2012.csv", tofile: "mlb_teams_2012_ant.csv")

println(out);
