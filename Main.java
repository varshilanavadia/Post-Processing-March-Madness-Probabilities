import java.io.*;
import java.util.*;
import java.nio.file.*;

public class Main {
    public static void main(String[] args) {
        // ARGUMENTS TO THE PROGRAM IN THE FOLLOWING ORDER
        // YEAR   TYPE   #ITERATIONS   STRATEGY   SIM_INDEX
        String year = args[0];
        int type = Integer.parseInt(args[1]);
        int totalIterations = Integer.parseInt(args[2]);
        int strategy = Integer.parseInt(args[3]);
        int simIndex = Integer.parseInt(args[4]);

        int readFromFile = 0;
//        String year = "2017";
//        int type = 0;
//        int totalIterations = 1;
//        int strategy =  0;
//        int simIndex = 0;

        // CREATE DIRECTORY STRUCTURE TO STORE RESULTS
        createDirectoryStructure(simIndex);

        // IMPORT DATA
        System.out.println("\nImporting data for " + year + "...");
        String curDir = System.getProperty("user.dir");
        String dataPath = curDir + "/Data/" + year + "/";
        File dataFiles = new File(dataPath);

        // GETTING SLOTS AND SEEDS INFORMATION FROM THE RESPECTIVE CSV FILES
        List<File> FileLists = getAllFiles(dataFiles, new ArrayList<>(), false);
        String[][] seeds = getSeedAndSlotFiles(FileLists, dataPath, year, "Seeds");
        String[][] slots = getSeedAndSlotFiles(FileLists, dataPath, year, "Slots");

        // GETTING SUBMISSION FILES FOR ALL SUBMISSIONS IN A LIST AND WRITING THIS LIST TO A TEST FILE FOR REFERENCE
        dataPath = curDir + "/Data/" + year + "/predictions/predictions";
        dataFiles = new File(dataPath);
        List<File> csvFileList = getAllFiles(dataFiles, new ArrayList<>(), true);
//        writeCSVFileList(year, csvFileList, curDir);

        // STORING IDS AND PREDICTIONS FROM ALL SUBMISSIONS INTO VARIABLES
        // CREATING A LIST OF 2278 UNIQUE POSSIBLE MATCHUPS [MATCHUP = YEAR_TEAM1_TEAM2]
        // WRITING IDS AND PREDICITONS TO CSV FOR REFERENCE
//        String[][] teamIdList = getAllTeamIDs(csvFileList);
//        String[] uniqueMatchUpIDs = getUniqueMatchUpIDs(teamIdList);
//        double[][] predictions = getAllPredictions(csvFileList);
//        double[][] orderedPredictions = orderPredictionValues(teamIdList, uniqueMatchUpIDs, predictions);
//        writePredictionsToCSV(year, orderedPredictions, 1, curDir);
//        writeUniqueMatchUpIDsToCSV(year, uniqueMatchUpIDs, curDir);
//        writeTeamIDstoCSV(year, teamIdList, curDir);

        // READING IDS, PREDICTION AND UNIQUE MATCH UPS FROM SINGLE FILE
        // String[][] teamIdList = readTeamIDsFromCSV(year, csvFileList, curDir);
         String[] uniqueMatchUpIDs = readUniqueMatchUpIDsFromCSV(year, curDir);
         double[][] predictions = readPredictionsFromCSV(year, csvFileList, curDir);

        // CALCULATING APPROXIMATE TRUE PROBABILITIES [MEAN OR MEDIAN]
        //         'type' = 0 CALCULATE MEAN TRUE PROBABILITIES
        //                = 1 CALCULATE MEDIAN TRUE PROBABILITIES
        // 'readFromFile' = 0 READ MEAN TRUE PROBABILITIES FROM FILE
        //                = 1 READ MEDIAN TRUE PROBABILITIES FROM FILE
        double[] approxTrueProb = calculateApproxTrueProb(year, predictions, type, readFromFile, curDir);

        simulate(year, totalIterations, type, approxTrueProb, uniqueMatchUpIDs, predictions, seeds, slots, curDir, strategy, simIndex);

    }

    private static void simulate(String year, int totalIterations, int type, double[] approxTrueProb,
                                 String[] uniqueMatchUpIDs, double[][] predictions, String[][] seeds, String[][] slots,
                                 String curDir, int strategy, int simIndex) {
        // SIMULATE THE PLAY-IN MATCHES AND TOURNAMENT MULTIPLE TIMES
//        double[] logLossScoreList = new double[totalIterations];
        for (int iteration = 0; iteration < totalIterations; iteration++) {
            // SIMULATING PLAY-IN MATCHES

            if (iteration % 1000 == 0) {
                System.out.println("Iteration: " + iteration);
            }

            String[][] playInTeamSeedsAndIDs = getSeeds(slots, 0);
            getPlayInTeamIDs(seeds, playInTeamSeedsAndIDs);
            String[][] playInMatchWinners = simulatePlayInMatches(year, playInTeamSeedsAndIDs, uniqueMatchUpIDs, approxTrueProb);

            // SIMULATING TOURNAMENT
            String[][] tourneyStartSeedsAndIDs = getSeeds(slots, 1);
            getTourneyStartIDs(seeds, tourneyStartSeedsAndIDs, playInMatchWinners);
            String[][] tournamentResults = simulateTournament(year, tourneyStartSeedsAndIDs, uniqueMatchUpIDs, approxTrueProb, slots, strategy);

            // CALCULATING LOGLOSS OF THE SIMULATION
            double simulationLogLoss = calculateLogLossForSimulation(tournamentResults);
//            logLossScoreList[iteration] = simulationLogLoss;

            // GET TOURNEY PREDICTIONS FROM 'predictions'
            double[][] tourneyPredFromSubmissions = getPredictionsFromSubmissions(year, predictions, tournamentResults, uniqueMatchUpIDs);

            // CALCULATE AND SORT LOG LOSS
            double[][] logLossLeaderboard = calculateAllLogLosses(year, tournamentResults, tourneyPredFromSubmissions, simulationLogLoss, iteration, type, curDir, simIndex);

            //WRITING RESULTS TO A FILE
            writeTournamentResults(year, tournamentResults, simulationLogLoss, iteration, type, curDir, simIndex);
            // System.out.println("Winner of simulation : Team " + tournamentResults[tournamentResults.length - 1][tournamentResults[0].length - 4]);
            // System.out.println("LogLoss Score : " + simulationLogLoss);
        }

        System.out.println("Simulation Complete!");

        // // DISPLAYING AVERAGE LOGLOSS SCORE FROM 'totalIterations' SIMULATIONS
        // System.out.printf("Average LogLoss score of %d simulations = %s%n", totalIterations, calculateMean(logLossScoreList));

        // GET THE RANKS OF OUR APPROX TRUE PROBABILITIES ACROSS ALL SIMULATIONS AND WRITE THEM OUT TO A FILE
        getTrueProbRanksFromLeaderboard(year, type, curDir, simIndex);

    }

    private static void getTrueProbRanksFromLeaderboard(String year, int type, String curDir, int simIndex) {
        int rank;
        File leaderboardFiles;
        if (year.equals("2016")) {
            rank = 1085;
        } else {
            rank = 775;
        }
        if (type == 0) {
            leaderboardFiles = new File(curDir + "/Results/" + year + "/Simulation_" + simIndex + "/Mean/LogLoss_Leaderboard/");
        } else {
            leaderboardFiles = new File(curDir + "/Results/" + year + "/Simulation_" + simIndex + "/Median/LogLoss_Leaderboard/");
        }
        List<File> fileList = getAllFiles(leaderboardFiles, new ArrayList<>(), false);
        List<Integer> trueProbRank = new ArrayList<>();
        for (File file : fileList) {
            BufferedReader br;
            try {
                br = new BufferedReader(new FileReader(file));
                String row = br.readLine();
                while (row != null) {
                    String[] data = row.split(",");
                    if (!data[2].startsWith("S") && Integer.parseInt(data[2]) == rank) {
                        trueProbRank.add(Integer.valueOf(data[0]));
                    }
                    row = br.readLine();
                }
                br.close();
            } catch (IOException e) {
                System.out.println("IOException");
            }
        }

        // WRITE THEM OUT TO A FILE
        StringBuilder builder = new StringBuilder();
        for (Integer integer : trueProbRank) {
            builder.append(integer).append("\n");
        }
        try {
            BufferedWriter writer = null;
            if (type == 0) {
                writer = new BufferedWriter(new FileWriter(curDir + "/Results/" + year + "/Simulation_" + simIndex + "/RanksOf-Mean-" + year + ".csv"));
            } else if (type == 1) {
                writer = new BufferedWriter(new FileWriter(curDir + "/Results/" + year + "/Simulation_" + simIndex + "/RanksOf-Median-" + year + ".csv"));
            }
            assert writer != null;
            writer.write(builder.toString());
            writer.close();
        } catch (IOException e) {
            System.out.println("getTrueProbRanksFromLeaderboard() : File writing issue.");
        }
    }

    private static void writeLogLossLeaderboard(String year, double[][] logLossLeaderboard, int iter, int type, String curDir, int simIndex) {
        StringBuilder builder = new StringBuilder();
        builder.append("RANK").append(",").append("LOG LOSS").append(",").append("SERIAL #").append("\n");
        for (double[] prediction : logLossLeaderboard) {
            builder.append((int) prediction[0]).append(",").append(prediction[1]).append(",").append((int)prediction[2]).append("\n");
        }
        BufferedWriter writer = null;
        try {
            iter++;
            if (type == 0) {
                writer = new BufferedWriter(new FileWriter(curDir + "/Results/" + year +
                        "/Simulation_" + simIndex + "/Mean/LogLoss_Leaderboard/leaderboard_" + iter + "_" + year + ".csv"));
            } else if (type == 1) {
                writer = new BufferedWriter(new FileWriter(curDir + "/Results/" + year +
                        "/Simulation_" + simIndex + "/Median/LogLoss_Leaderboard/leaderboard_" + iter + "_" + year + ".csv"));
            }
            assert writer != null;
            writer.write(builder.toString());
            writer.close();
        } catch (IOException e) {
            System.out.println("writeLogLossLeaderboard() : File writing issue.");
        }
    }

    private static void sortLeaderboard(double[][] array, int col) {
        java.util.Arrays.sort(array, Comparator.comparingDouble(a -> a[col]));
    }

    private static double[][] calculateAllLogLosses(String year, String[][] tournamentResults, double[][] tourneyPredFromSubmissions, double simulationLogLoss, int iteration, int type, String curDir, int simIndex) {
        // CALCULATE ALL SUBMISSION LOG LOSSES
        // System.out.println("Preparing the leaderboard and ranking all log loss scores...");
        double[][] logLossLeaderboard = new double[tourneyPredFromSubmissions[0].length + 1][3];
        for (int i = 0; i < tourneyPredFromSubmissions[0].length; i++) {
            logLossLeaderboard[i][2] = i + 1;       // SERIAL NUMBER TO HELP LOCATE OUR LOG LOSS
            for (int j = 0; j < tourneyPredFromSubmissions.length; j++) {
                logLossLeaderboard[i][1] += calculateIndividualLogLoss(String.valueOf(tourneyPredFromSubmissions[j][i]), tournamentResults[j][6]);
            }
            logLossLeaderboard[i][1] = -logLossLeaderboard[i][1] / tourneyPredFromSubmissions.length;
        }
        // OUR LOG LOSS IS ALWAYS LAST BEFORE THE ARRAY IS SORTED [POSITION 1085 OR 775]
        logLossLeaderboard[logLossLeaderboard.length - 1][2] = tourneyPredFromSubmissions[0].length + 1;
        logLossLeaderboard[logLossLeaderboard.length - 1][1] = simulationLogLoss;

        // SORT LOG LOSSES ASCENDING TO DESCENDING AND CHANGE THE CORRESPONDING RANK
        sortLeaderboard(logLossLeaderboard, 1);

        // ADD THE RANKINGS
        for (int i = 0; i < logLossLeaderboard.length; i++) {
            logLossLeaderboard[i][0] = i + 1;
        }

        // WRITE SORTED LOG LOSS LEADERBOARD TO FILE
        writeLogLossLeaderboard(year, logLossLeaderboard, iteration, type, curDir, simIndex);
        return logLossLeaderboard;
    }

    private static double[][] getPredictionsFromSubmissions(String year, double[][] predictions, String[][] tournamentResults, String[] uniqueMatchUpIDs) {
        double[][] tourneyPredFromSubmissions = new double[tournamentResults.length][predictions[0].length];
        for (int i = 0; i < tournamentResults.length; i++) {
            for (int j = 0; j < uniqueMatchUpIDs.length; j++) {
                if (uniqueMatchUpIDs[j].equals(getMatchupID(year, tournamentResults[i][1], tournamentResults[i][2]))) {
                    tourneyPredFromSubmissions[i] = predictions[j];
                }
            }
        }
        for (double[] tourneyPredFromSubmission : tourneyPredFromSubmissions) {
            for (int i = 0; i < tourneyPredFromSubmission.length; i++) {
                if (tourneyPredFromSubmission[i] >= 1) {
                    tourneyPredFromSubmission[i] = 0.9999999999;
                } else if (tourneyPredFromSubmission[i] <= 0) {
                    tourneyPredFromSubmission[i] = 0.0000000001;
                }
            }
        }
        return tourneyPredFromSubmissions;
    }

    private static double calculateLogLossForSimulation(String[][] tournamentResults) {
        // FUNCTION TO CALCULATE LOGLOSS SCORE OF THE SIMULATION
        // System.out.println("Calculating log loss for current simulation...");
        double logLoss = 0.0;
        for (String[] tournamentResult : tournamentResults) {
            logLoss += calculateIndividualLogLoss(tournamentResult[5], tournamentResult[6]);
        }
        logLoss = -logLoss / tournamentResults.length;
        return logLoss;
    }

    private static double calculateIndividualLogLoss(String meanPred, String actualOutcome) {
        // FUNCTION TO CALCULATE LOGLOSS SCORE OF INDIVIDUAL MATCH
        return ((Double.parseDouble(actualOutcome) * Math.log(Double.parseDouble(meanPred))) +
                ((1 - Double.parseDouble(actualOutcome)) * Math.log(1 - Double.parseDouble(meanPred))));
    }

    private static void writeTournamentResults(String year, String[][] tournamentResults, double simulationLogLoss, int iter, int type, String curDir, int simIndex) {
        // FUNCTION TO WRITE TOURNAMENT RESULT TO CSV FILE
        try {
            iter++;
            FileWriter csvWriter = null;
            if (type == 0) {
                csvWriter = new FileWriter(curDir + "/Results/" + year + "/Simulation_" + simIndex + "/Mean/Simulations/meanSim_" + iter + ".csv");
            } else if (type == 1) {
                csvWriter = new FileWriter(curDir + "/Results/" + year + "/Simulation_" + simIndex + "/Median/Simulations/mediSim_" + iter + ".csv");
            }
            assert csvWriter != null;
            csvWriter.append("LogLoss Score").append(",").append(String.valueOf(simulationLogLoss)).append("\n");
            if (type == 0) {
                csvWriter.append("SLOT").append(",").append("TEAM 1 ID").append(",").append("TEAM 2 ID").append(",").append("WINNER TEAM ID").append(",").append("COIN FLIP").append(",").append("MEAN PRED").append(",").append("0/1 FLAG").append("\n");
            } else {
                csvWriter.append("SLOT").append(",").append("TEAM 1 ID").append(",").append("TEAM 2 ID").append(",").append("WINNER TEAM ID").append(",").append("COIN FLIP").append(",").append("MEDIAN PRED").append(",").append("0/1 FLAG").append("\n");
            }
            for (String[] r : tournamentResults) {
                csvWriter.append(String.join(",", r));
                csvWriter.append("\n");
            }
            csvWriter.flush();
            csvWriter.close();
        } catch (IOException e) {
            System.out.println("writeTournamentResults() : File writing issue.");
        }
    }

    private static String[][] simulateTournament(String year, String[][] tourneyStartSeedsAndIDs, String[] uniqueMatchUpIDs, double[] approxTrueProb, String[][] slots, int strategy) {
        // FUNCTION TO SIMULATE THE TOURNAMENT
        // DATA IS STORED IN 'tournamentResults' IN THE FOLLOWING ORDER,
        // [SLOT, TEAM1_ID, TEAM2_ID, WINNER_TEAM_ID, COIN_FLIP, MEAN_PREDICTION, 0/1 FLAG IF TEAM 1 BEAT TEAM 2]
        // System.out.println("Simulating Tournament...");
        String[][] tournamentResults = new String[63][7];
        // ADDING THE TOURNAMENT SLOTS TO 'tournamentResults'
        for (int rowIndex = 0; rowIndex < slots.length; rowIndex++) {
            if (slots[rowIndex][1].startsWith("R")) {
                tournamentResults[rowIndex - 5][0] = slots[rowIndex][1];
            }
            if (rowIndex >= 37) {
                tournamentResults[rowIndex - 5][1] = slots[rowIndex][2];
                tournamentResults[rowIndex - 5][2] = slots[rowIndex][3];
            }
        }
        // ADDING ROUND 1 TEAM IDS TO 'tournamentResults'
        for (int rowIndex = 0; rowIndex < tourneyStartSeedsAndIDs.length; rowIndex++) {
            if (tourneyStartSeedsAndIDs[rowIndex][1].equals(tournamentResults[rowIndex][0])) {
                tournamentResults[rowIndex][1] = tourneyStartSeedsAndIDs[rowIndex][2];
                tournamentResults[rowIndex][2] = tourneyStartSeedsAndIDs[rowIndex][3];
            }
        }
        // ACTUALLY SIMULATING THE TOURNAMENT AND SIMULTANEOUSLY FILLING OUT 'tournamentResults'
        // LOOP RUNS UNTIL BOTTOM-RIGHT CELL (i.e. winner) IS EQUAL TO NULL
        while (tournamentResults[tournamentResults.length - 1][tournamentResults[0].length - 1] == null) {
            for (int rowIndex = 0; rowIndex < tournamentResults.length; rowIndex++) {
                String teamID = getMatchupID(year, tournamentResults[rowIndex][1], tournamentResults[rowIndex][2]);
                String teamLowerID = Double.parseDouble(tournamentResults[rowIndex][1]) < Double.parseDouble(tournamentResults[rowIndex][2]) ? tournamentResults[rowIndex][1] : tournamentResults[rowIndex][2];
                String teamHigherID = Double.parseDouble(tournamentResults[rowIndex][1]) < Double.parseDouble(tournamentResults[rowIndex][2]) ? tournamentResults[rowIndex][2] : tournamentResults[rowIndex][1];

                if (strategy == 0) {
                    // IMPLEMENT NORMAL STRATEGY
                    strategy_normal(tournamentResults, uniqueMatchUpIDs, approxTrueProb, teamID, teamLowerID, teamHigherID, rowIndex);
                } else if (strategy == 1) {
                    strategy_1vs16seed(tournamentResults, uniqueMatchUpIDs, approxTrueProb, teamID, teamLowerID, teamHigherID, rowIndex);
                }

                // UPDATE 'tournamentResults' WITH EACH UPDATE/MATCH SIMULATION (ITERATION)
                for (int k = rowIndex + 1; k < tournamentResults.length; k++) {
                    if (tournamentResults[rowIndex][0].equals(tournamentResults[k][1])) {
                        tournamentResults[k][1] = tournamentResults[rowIndex][3];
                    }
                    if (tournamentResults[rowIndex][0].equals(tournamentResults[k][2])) {
                        tournamentResults[k][2] = tournamentResults[rowIndex][3];
                    }
                }
            }
        }
        return tournamentResults;
    }

//    private static void strategy_optimizeChances

    private static void strategy_1vs16seed(String[][] tournamentResults, String[] uniqueMatchUpIDs, double[] approxTrueProb, String teamID, String teamLowerID, String teamHigherID, int rowIndex) {
        if (tournamentResults[rowIndex][0].startsWith("R1") && tournamentResults[rowIndex][0].endsWith("1")) {
//            System.out.println(tournamentResults[rowIndex][0]);
            for (int j = 0; j < uniqueMatchUpIDs.length; j++) {
                if (uniqueMatchUpIDs[j].equals(teamID)) {
                    tournamentResults[rowIndex][4] = "coinFlip";
                    tournamentResults[rowIndex][5] = String.valueOf(approxTrueProb[j]);
                    tournamentResults[rowIndex][3] = tournamentResults[rowIndex][1];
                    tournamentResults[rowIndex][6] = String.valueOf(1);
                }
            }
        } else {
            strategy_normal(tournamentResults, uniqueMatchUpIDs, approxTrueProb, teamID, teamLowerID, teamHigherID, rowIndex);
        }
    }

    private static void strategy_normal(String[][] tournamentResults, String[] uniqueMatchUpIDs, double[] approxTrueProb, String teamID, String teamLowerID, String teamHigherID, int rowIndex) {
        for (int j = 0; j < uniqueMatchUpIDs.length; j++) {
            if (uniqueMatchUpIDs[j].equals(teamID)) {
                double coinFlip = flipTheCoin();
                tournamentResults[rowIndex][4] = String.valueOf(coinFlip);
                tournamentResults[rowIndex][5] = String.valueOf(approxTrueProb[j]);
                if (coinFlip <= approxTrueProb[j]) {
                    tournamentResults[rowIndex][3] = teamLowerID;
                    tournamentResults[rowIndex][6] = String.valueOf(1);
                } else {
                    tournamentResults[rowIndex][3] = teamHigherID;
                    tournamentResults[rowIndex][6] = String.valueOf(0);
                }
            }
        }
    }

    private static void getTourneyStartIDs(String[][] seeds, String[][] tourneyStartSeedsAndIDs, String[][] playInMatchWinners) {
        // FUNCTION TO GET THE TEAM IDS FOR ROUND 1 OF THE TOURNAMENT
        // LINES 68 - 77 ARE DUPLICATED. WRITE A SINGLE FUNCTION TO COMBINE THE DUPLICATION.
        getTeamIDsForMatches(seeds, tourneyStartSeedsAndIDs);
        for (String[] tourneyStartSeedsAndID : tourneyStartSeedsAndIDs) {
            for (String[] playInMatchWinner : playInMatchWinners) {
                if (tourneyStartSeedsAndID[3].equals(playInMatchWinner[0])) {
                    tourneyStartSeedsAndID[3] = playInMatchWinner[3];
                }
            }
        }
    }

    private static void getTeamIDsForMatches(String[][] seeds, String[][] tourneyStartSeedsAndIDs) {
        for (String[] tourneyStartSeedsAndID : tourneyStartSeedsAndIDs) {
            for (String[] seed : seeds) {
                if (tourneyStartSeedsAndID[2].equals(seed[1])) {
                    tourneyStartSeedsAndID[2] = seed[2];
                }
                if (tourneyStartSeedsAndID[3].equals(seed[1])) {
                    tourneyStartSeedsAndID[3] = seed[2];
                }
            }
        }
    }

    private static String getMatchupID(String year, String team1, String team2) {
        // FUNCTION TO CALCULATE MATCH UP TEAM ID GIVEN THE IDs OF PARTICIPATING TEAMS
        if (Double.parseDouble(team1) < Double.parseDouble(team2)) {
            return year + "_" + team1 + "_" + team2;
        } else {
            return year + "_" + team2 + "_" + team1;
        }
    }

    private static String[][] simulatePlayInMatches(String year, String[][] playInTeamSeedsAndIDs, String[] uniqueMatchUpIDs, double[] approxTrueProb) {
        // FUNCTION TO SIMULATE PLAY-IN MATCHES
        // DATA IS STORED IN 'playInMatchWinners' IN THE FOLLOWING ORDER,
        // [SEED_FOR_R1, TEAM1_ID, TEAM2_ID, WINNER_TEAM_ID, COIN_FLIP, MEAN_PREDICTION, 0/1 FLAG IF TEAM 1 BEAT TEAM 2]
        // System.out.println("Simulating Play In Matches...");
        String[][] playInMatchWinners = new String[4][7];
        for (int i = 0; i < playInTeamSeedsAndIDs.length; i++) {
            playInMatchWinners[i][0] = playInTeamSeedsAndIDs[i][1];
            playInMatchWinners[i][1] = playInTeamSeedsAndIDs[i][2];
            playInMatchWinners[i][2] = playInTeamSeedsAndIDs[i][3];
            String teamID = getMatchupID(year, playInTeamSeedsAndIDs[i][2], playInTeamSeedsAndIDs[i][3]);
            for (int j = 0; j < uniqueMatchUpIDs.length; j++) {
                if (uniqueMatchUpIDs[j].equals(teamID)) {
                    double coinFlip = flipTheCoin();
                    playInMatchWinners[i][4] = String.valueOf(coinFlip);
                    playInMatchWinners[i][5] = String.valueOf(approxTrueProb[j]);
                    if (coinFlip <= approxTrueProb[j]) {
                        playInMatchWinners[i][3] = playInMatchWinners[i][1];
                        playInMatchWinners[i][6] = String.valueOf(1);
                    } else {
                        playInMatchWinners[i][3] = playInMatchWinners[i][2];
                        playInMatchWinners[i][6] = String.valueOf(0);
                    }
                }
            }
        }
        return playInMatchWinners;
    }

    private static double flipTheCoin() {
        // FUNCTION TO GENERATE RANDOM DOUBLE
        Random coinFlip = new Random();
        return coinFlip.nextDouble();
    }

    private static void getPlayInTeamIDs(String[][] seeds, String[][] playInTeamSeedsAndIDs) {
        // FUNCTION TO GET THE TEAM IDS FOR PLAY-IN MATCHES
        getTeamIDsForMatches(seeds, playInTeamSeedsAndIDs);
    }

    private static String[][] getSeeds(String[][] slots, int tourneyIndicator) {
        // FUNCTION TO GET SEEDS INFORMATION
        // tourneyIndicator = 0 if its play-in matches
        // tourneyIndicator = 1 if its tournament round-1 matches
        int rowIndex = 0;
        String[][] seeds;
        int numRows = tourneyIndicator == 0 ? 4 : 32;
        seeds = new String[numRows][4];
        if (tourneyIndicator == 0) {
            for (String[] row : slots) {
                if (!row[1].startsWith("R") && !row[1].startsWith("S")) {
                    seeds[rowIndex] = row;
                    rowIndex++;
                }
            }
        } else {
            for (String[] row : slots) {
                if (row[1].startsWith("R1")) {
                    seeds[rowIndex] = row;
                    rowIndex++;
                }
            }
        }
        return seeds;
    }

    private static void writeApproxTrueProb(String year, double[] approxTrueProb, int type, String curDir) {
        // FUNCTION TO WRITE MEAN PREDICTIONS TO CSV FILE
        StringBuilder builder = new StringBuilder();
        for (double prediction : approxTrueProb) {
            builder.append(prediction).append("\n");
        }
        try {
            BufferedWriter writer = null;
            if (type == 0) {
                // APPROX MEAN PROBABILITIES
                writer = new BufferedWriter(new FileWriter(curDir + "/Results/" + year + "/Mean-True-Prob-" + year + ".csv"));
            } else if (type == 1) {
                // APPROX MEDIAN PROBABILITIES
                writer = new BufferedWriter(new FileWriter(curDir + "/Results/" + year + "/Median-True-Prob-" + year + ".csv"));
            }
            assert writer != null;
            writer.write(builder.toString());
            writer.close();
        } catch (IOException e) {
            System.out.println("writeApproxTrueProb() : File writing issue.");
        }

    }

    private static double calculateMean(double[] array) {
        double mean = 0;
        for (double item : array) {
            mean += item;
        }
        mean = mean / array.length;
        return mean;
    }

    private static double calculateMedian(double[] array) {
        Arrays.sort(array);
        if (array.length % 2 != 0) {
            return array[array.length / 2];
        }
        return (array[(array.length - 1) / 2] + array[array.length / 2]) / 2.0;
    }

    private static double[] calculateApproxTrueProb(String year, double[][] predictions, int type, int readFromFile, String curDir) {
        // FUNCTION TO CALCULATE APPROXIMATE TRUE PREDICTIONS
        double[] approxTrueProb = new double[predictions.length];
        if (readFromFile == 0) {
            // DO THE CALCULATIONS
            if (type == 0) {
                System.out.println("Calculating Mean Approximate True Probabilities...");
                for (int i = 0; i < predictions.length; i++) {
                    approxTrueProb[i] = calculateMean(predictions[i]);
                }
            } else if (type == 1) {
                System.out.println("Calculating Median Approximate True Probabilities...");
                for (int i = 0; i < predictions.length; i++) {
                    approxTrueProb[i] = calculateMedian(predictions[i]);
                }
            }
            writeApproxTrueProb(year, approxTrueProb, type, curDir);
        } else if (readFromFile == 1) {
            // READ FROM EXISTING FILE
            if (type == 0) {
                System.out.println("Reading Mean Approximate True Probabilities From File...");
                try {
                    BufferedReader br = new BufferedReader(new FileReader(curDir + "/Results/" + year + "/Mean-True-Prob-" + year + ".csv"));
                    String row = br.readLine();
                    int i = 0;
                    while (row != null) {
                        approxTrueProb[i] = Double.parseDouble(row);
                        i++;
                        row = br.readLine();
                    }
                    br.close();
                } catch (FileNotFoundException e) {
                    System.out.println("readApproxTrueProbFromFile() -- Read Mean: File not found.");
                } catch (IOException e) {
                    System.out.println("readApproxTrueProbFromFile() -- Read Mean: IOException.");
                }
            } else if (type == 1) {
                System.out.println("Reading Median Approximate True Probabilities From File...");
                try {
                    BufferedReader br = new BufferedReader(new FileReader(curDir + "/Results/" + year + "/Median-True-Prob-" + year + ".csv"));
                    String row = br.readLine();
                    int i = 0;
                    while (row != null) {
                        approxTrueProb[i] = Double.parseDouble(row);
                        i++;
                        row = br.readLine();
                    }
                    br.close();
                } catch (FileNotFoundException e) {
                    System.out.println("readApproxTrueProbFromFile() -- Read Median: File not found.");
                } catch (IOException e) {
                    System.out.println("readApproxTrueProbFromFile() -- Read Median: IOException.");
                }
            }
        }
        return approxTrueProb;
    }

    private static double[][] readPredictionsFromCSV(String year, List<File> csvFileList, String curDir) {
        double[][] predictions = new double[2278][csvFileList.size()];
        try {
            BufferedReader br = new BufferedReader(new FileReader(curDir + "/Results/" + year + "/Predictions-" + year + ".csv"));
            String row = br.readLine();
            int i = 0;
            while (row != null) {
                String[] data = row.split(",");
                for (int j = 0; j < data.length; j++) {
                    predictions[i][j] = Double.parseDouble(data[j]);
                }
                i++;
                row = br.readLine();
            }
            br.close();
        } catch (IOException e) {
            System.out.println("readPredictionsFromCSV() : IOException.");
        }
        return predictions;
    }

    private static String[] readUniqueMatchUpIDsFromCSV(String year, String curDir) {
        String[] uniqueMatchUpIDs = new String[2278];
        try {
            BufferedReader br = new BufferedReader(new FileReader(curDir + "/Results/" + year + "/Unique-MatchUp-IDs-" + year + ".csv"));
            String row = br.readLine();
            int i = 0;
            while (row != null) {
                uniqueMatchUpIDs[i] = row;
                i++;
                row = br.readLine();
            }
            br.close();
        } catch (IOException e) {
            System.out.println("readUniqueMatchUpIDsFromCSV() : IOException.");
        }
        return uniqueMatchUpIDs;
    }

    private static String[][] readTeamIDsFromCSV(String year, List<File> csvFileList, String curDir) {
        String[][] teamIdList = new String[2278][csvFileList.size()];
        try {
            BufferedReader br = new BufferedReader(new FileReader(curDir + "/Results/" + year + "/Team-IDs-" + year + ".csv"));
            String row = br.readLine();
            int i = 0;
            while (row != null) {
                String[] data = row.split(",");
                System.arraycopy(data, 0, teamIdList[i], 0, data.length);
                i++;
                row = br.readLine();
            }
            br.close();
        } catch (IOException e) {
            System.out.println("readTeamIDsFromCSV() : IOException.");
        }
        return teamIdList;
    }

    private static void writeTeamIDstoCSV(String year, String[][] teamIdList, String curDir) {
        // FUNCTION TO WRITE 'teamIdList' (TEAM IDs) FROM ALL SUBMISSIONS TO CSV FILE
        try {
            FileWriter csvWriter = new FileWriter(curDir + "/Results/" + year + "/Team-IDs-" + year + ".csv");
            for (String[] r : teamIdList) {
                csvWriter.append(String.join(",", r));
                csvWriter.append("\n");
            }
            csvWriter.flush();
            csvWriter.close();
        } catch (IOException e) {
            System.out.println("writeTeamIDstoCSV() : File writing issue.");
        }
    }

    private static void writeUniqueMatchUpIDsToCSV(String year, String[] uniqueMatchUpIDs, String curDir) {
        // FUNCTION TO WRITE 'uniqueMatchUpIDs' TO CSV FILE
        try {
            FileWriter csvWriter = new FileWriter(curDir +"/Results/" + year + "/Unique-MatchUp-IDs-" + year + ".csv");
            for (String r : uniqueMatchUpIDs) {
                csvWriter.append(String.join(",", r));
                csvWriter.append("\n");
            }
            csvWriter.flush();
            csvWriter.close();
        } catch (IOException e) {
            System.out.println("writeUniqueMatchUpIDsToCSV() : File writing issue.");
        }
    }

    private static void writePredictionsToCSV(String year, double[][] predictions, int i, String curDir) {
        // FUNCTION TO WRITE 'predictions' FROM ALL SUBMISSIONS TO CSV FILE
        StringBuilder builder = new StringBuilder();
        for (double[] prediction : predictions) {
            for (double v : prediction) {
                builder.append(v).append(",");
            }
            builder.append("\n");
        }
        BufferedWriter writer = null;
        try {
            if (i == 1) {
                writer = new BufferedWriter(new FileWriter(curDir + "/Results/" + year + "/Predictions-" + year + ".csv"));
            } else if (i == 2) {
                writer = new BufferedWriter(new FileWriter(curDir + "/Results/" + year + "/tourneyPredForSim.csv"));
            }
            assert writer != null;
            writer.write(builder.toString());
            writer.close();
        } catch (IOException e) {
            System.out.println("writePredictionsToCSV() : File writing issue.");
        }
    }

    private static double[][] orderPredictionValues(String[][] teamIdList, String[] uniqueMatchUpIDs, double[][] predictions) {
        double[][] orderedPredictions = new double[predictions.length][predictions[0].length];
        int f;
        for (int i = 0; i < uniqueMatchUpIDs.length; i++) {
            f = 0;
            for (int j = 0; j < teamIdList[0].length; j++) {
                for (int k = 0; k < teamIdList.length; k++) {
                    if (uniqueMatchUpIDs[i].equals(teamIdList[k][j])) {
//                        System.out.println(i + "  " + j + "  " + k + "  " +uniqueMatchUpIDs[i] + "  " + teamIdList[k][j]);
                        orderedPredictions[i][j] = predictions[k][j];
                        f = 1;
                    } else if (f == 1) {
                        break;
                    }
                }
                f = 0;
            }
        }
        return orderedPredictions;
    }

    private static double[][] getAllPredictions(List<File> csvFileList) {
        // GET PREDICTION VALUES FROM ALL SUBMISSIONS
        double[][] predictions = new double[2278][csvFileList.size()];
        int rowIndex = 0;
        int columnIndex = 0;
        for (File f : csvFileList) {
            try {
                BufferedReader csvReader = new BufferedReader(new FileReader(f.getAbsolutePath()));
                String row = csvReader.readLine();
                while (row != null && !row.isEmpty()) {
                    String[] data = row.split(",");
                    data[0] = data[0].replaceAll("^\"|\"$", "").trim();
                    data[1] = data[1].replaceAll("^\"|\"$", "").trim();
                    if (!data[1].startsWith("p") && !data[1].startsWith("P")) {
                        predictions[rowIndex - 1][columnIndex] = Double.parseDouble(data[1]);
                    }
                    rowIndex += 1;
                    row = csvReader.readLine();
                }
                rowIndex = 0;
                columnIndex += 1;
                csvReader.close();
            } catch (FileNotFoundException e) {
                System.out.println("getAllPredictions() : File " + f.getName() + " not found while getting Tourney Seeds and Slots.");
            } catch (IOException e) {
                System.out.println("getAllPredictions() : IOException");
            }
        }
        return predictions;
    }

    private static String[] getUniqueMatchUpIDs(String[][] teamIdList) {
        // FUNCTION TO RETURN AN ARRAY OF (2278) UNIQUE TEAM IDs (MATCH UPS)
        String[] uniqueMatchUpIDs = new String[teamIdList.length];
        for (int i = 0; i < uniqueMatchUpIDs.length; i++) {
            uniqueMatchUpIDs[i] = teamIdList[i][0];
        }
        return uniqueMatchUpIDs;
    }

    private static String[][] getAllTeamIDs(List<File> csvFileList) {
        // GET TEAM IDs (MATCH UPS) FROM ALL SUBMISSIONS
        String[][] teamIdList = new String[2278][csvFileList.size()];
        int rowIndex = 0;
        int columnIndex = 0;
        for (File f : csvFileList) {
            try {
                BufferedReader csvReader = new BufferedReader(new FileReader(f.getAbsolutePath()));
                String row = csvReader.readLine();
                while (row != null && !row.isEmpty()) {
                    String[] data = row.split(",");
                    data[0] = data[0].replaceAll("^\"|\"$", "");
                    data[1] = data[1].replaceAll("^\"|\"$", "");
                    if (!data[0].contains("i") && !data[0].contains("I")) {
                        teamIdList[rowIndex][columnIndex] = data[0];
                        rowIndex += 1;
                    }
                    row = csvReader.readLine();
                }
                rowIndex = 0;
                columnIndex += 1;
                csvReader.close();
            } catch (FileNotFoundException e) {
                System.out.println("getAllTeamIDs() : File " + f.getName() + " not found while getting Tourney Seeds and Slots.");
            } catch (IOException e) {
                System.out.println("getAllTeamIDs() : IOException");
            }
        }
        return teamIdList;
    }

    private static void writeCSVFileList(String year, List<File> csvFileList, String curDir) {
        // FUNCTION USED FOR PERSONAL REFERENCE
        try {
            FileWriter w = new FileWriter(curDir + "/Other/csvFileList" + year + ".txt");
            for (File file : csvFileList) {
                w.append(file.getAbsolutePath());
                w.append("\n\n");
            }
            w.flush();
            w.close();
        } catch (IOException e) {
            System.out.println("writeCSVFileList() : IOException");
        }
    }

    private static String[][] getSeedAndSlotData(BufferedReader buffReader, int numCol, int numRows) {
        // RETRIEVE SEEDS AND SLOTS DATA FROM THE FILE
        int rowIndex = 0;
        String line = null;
        try {
            line = buffReader.readLine();
        } catch (IOException e) {
            System.out.println("[1] getSeedsAndSlotsData() : Buffered Reader cannot read line.");
        }
        String[][] returnSeedAndSlotData = new String[numRows][numCol];
        while (line != null) {
            String[] data = line.split(",");
            returnSeedAndSlotData[rowIndex][0] = data[0];
            returnSeedAndSlotData[rowIndex][1] = data[1];
            returnSeedAndSlotData[rowIndex][2] = data[2];
            if (numCol == 4) {
                returnSeedAndSlotData[rowIndex][3] = data[3];
            }
            rowIndex++;
            try {
                line = buffReader.readLine();
            } catch (IOException e) {
                System.out.println("[2] getSeedsAndSlotsData() : Buffered Reader cannot read line.");
            }
        }
        return returnSeedAndSlotData;
    }

    private static String[][] getSeedAndSlotFiles(List<File> FileLists, String dataPath, String year, String fileName) {
        // RETRIEVE CSV FILES PERTAINING TO SEEDS AND SLOTS INFORMATION
        int numRows, numCol;
        if (fileName.equals("Seeds")) {
            numRows = 69;
            numCol = 3;
        } else {
            numRows = 68;
            numCol = 4;
        }
        String[][] data = new String[numRows][numCol];
        for (File f : FileLists) {
            if (f.getName().equals(fileName + year + ".csv")) {
                BufferedReader buffReader;
                try {
                    buffReader = new BufferedReader(new FileReader(dataPath + f.getName()));
                    data = getSeedAndSlotData(buffReader, numCol, numRows);
                } catch (FileNotFoundException e) {
                    System.out.println("getSeedAndSlotFiles() : File " + f.getName() + " not found while getting Tourney Seeds and Slots.");
                }
            }
        }
        return data;
    }

    private static List<File> getAllFiles(File dataFiles, List<File> csvFileList, boolean flag) {
        // RETRIEVE ALL CSV FILES LISTED IN THE DIRECTORY
        // flag == 1 MAKES RECURSIVE CALLS TO THIS FUNCTION
        File[] filesList = dataFiles.listFiles();
        assert filesList != null;
        for (File f : filesList) {
            if (flag) {
                if (f.isDirectory()) {
                    getAllFiles(f, csvFileList, true);
                }
            }
            if (f.isFile() && f.getAbsolutePath().endsWith(".csv")) {
                csvFileList.add(f);
            }
        }
        return csvFileList;
    }

    private static void createDirectoryStructure(int simIndex) {
        Path path = Paths.get("Other//..//Results//2016//Other//..//Simulations//Mean//..//Median//..//..//LogLoss_Leaderboard//Mean//..//Median//..//..//..//2017//Other//..//Simulations//Mean//..//Median//..//..//LogLoss_Leaderboard//Mean//..//Median");
        Path p1 = Paths.get("Other");

        Path p2 = Paths.get("Results//2016//Other");
        Path p3 = Paths.get("Results//2016//Simulation_" + simIndex +  "//Mean//LogLoss_Leaderboard/");
        Path p4 = Paths.get("Results//2016//Simulation_" + simIndex +  "//Median//LogLoss_Leaderboard/");
        Path p5 = Paths.get("Results//2016//Simulation_" + simIndex +  "//Mean//Simulations/");
        Path p6 = Paths.get("Results//2016//Simulation_" + simIndex +  "//Median//Simulations/");

        Path p7 = Paths.get("Results//2017//Other");
        Path p8 = Paths.get("Results//2017//Simulation_" + simIndex +  "//Mean//LogLoss_Leaderboard/");
        Path p9 = Paths.get("Results//2017//Simulation_" + simIndex +  "//Median//LogLoss_Leaderboard/");
        Path p10 = Paths.get("Results//2017//Simulation_" + simIndex +  "//Mean//Simulations/");
        Path p11 = Paths.get("Results//2017//Simulation_" + simIndex +  "//Median//Simulations/");


        try {
            Files.createDirectories(p1);
            Files.createDirectories(p2);
            Files.createDirectories(p3);
            Files.createDirectories(p4);
            Files.createDirectories(p5);
            Files.createDirectories(p6);
            Files.createDirectories(p7);
            Files.createDirectories(p8);
            Files.createDirectories(p9);
            Files.createDirectories(p10);
            Files.createDirectories(p11);
        } catch (IOException e) {
            System.err.println("Cannot create directories - " + e);
        }
    }
}

//  java Main 2016 0 1 50000 1 &
//  java Main 2016 1 1 50000 1 &
//  java Main 2017 0 1 50000 1 &
//  java Main 2017 1 1 50000 1 &
