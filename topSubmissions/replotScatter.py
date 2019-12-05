import glob
import os
import sys
import numpy as np
import pandas as pd
import matplotlib

matplotlib.use('Agg')
import matplotlib.pyplot as plt


def get_team_match_up(year, team_a, team_b):
    if team_a < team_b:
        return year + "_" + str(team_a) + "_" + str(team_b)
    else:
        return year + "_" + str(team_b) + "_" + str(team_a)


def initialize():
    year = sys.argv[1]
    prob_type = sys.argv[2]
    sim_index = sys.argv[3]
    # year = '2016'
    # prob_type = '0'
    # sim_index = '0'

    print("YEAR     : " + year)
    print("PROB     : " + prob_type)
    print("SIM INDEX: " + sim_index)

    os.chdir('Expected Payout' + (
        '/Mean Prob/' if prob_type == '0' else '/Median Prob/') + year + '/Simulation_' + sim_index)

    csvFile = glob.glob('*.csv')
    top_sub = pd.read_csv(csvFile[0], index_col=0)
    top_sub = top_sub.sort_values(by=['Total'], ascending=False)

    if year == '2016' and prob_type == '0':
        print('Writing \'Expected Payout\' [2016-Mean] to .csv file ...')
        top_sub.to_csv('exp_pay-Sim_' + str(sim_index) + '-2016-Mean.csv')
    elif year == '2016' and prob_type == '1':
        print('Writing \'Expected Payout\' [2016-Median] to .csv file ...')
        top_sub.to_csv('exp_pay-Sim_' + str(sim_index) + '-2016-Median.csv')
    elif year == '2017' and prob_type == '0':
        print('Writing \'Expected Payout\' [2017-Mean] to .csv file ...')
        top_sub.to_csv('exp_pay-Sim_' + str(sim_index) + '-2017-Mean.csv')
    elif year == '2017' and prob_type == '1':
        print('Writing \'Expected Payout\' [2017-Median] to .csv file ...')
        top_sub.to_csv('exp_pay-Sim_' + str(sim_index) + '-2017-Median.csv')

    os.chdir('../../../../../')
    print('Generating scatter plots...')

    # GETTING SUBMISSION NUMBER OF TOP 5 SUBMISSIONS
    rank = top_sub.T.columns.values
    rank = rank[0:5]
    os.chdir('Other/')

    # GETTING MAIN PREDICTIONS MATRIX
    os.chdir('../Results/' + year + '/')
    predictions = pd.read_csv("Predictions-" + year + ".csv", header=None)

    # GETTING TOP 5 PREDICTIONS FROM MATRIX
    topRanks = predictions.iloc[:, rank - 1]
    topRanks.columns = ['Rank 1', 'Rank 2', 'Rank 3', 'Rank 4', 'Rank 5']

    # GETTING MEDIAN PREDICTIONS
    trueProb = pd.read_csv(("Mean" if prob_type == '0' else "Median") + "-True-Prob-" + year + ".csv", header=None)
    unique_match_ups = pd.read_csv("Unique-MatchUp-IDs-" + year + ".csv", header=None)

    os.chdir('../../topSubmissions/Expected Payout' + (
        '/Mean Prob/' if prob_type == '0' else '/Median Prob/') + year + '/Simulation_' + str(sim_index))
    # DEFINING SCATTER PLOT PARAMETERS
    alp = 0.5
    ar = 10
    res = 720

    # CREATE SCATTER PLOT WITH ALL PROBABILITIES
    print("\tAll Probabilities...")
    for i in range(len(topRanks.T)):
        plt.plot([0, 1], c='r')
        plt.scatter(trueProb, topRanks.iloc[:, i], alpha=alp, s=ar)
        # plt.title('Rank ' + str(i + 1) + 'v/s App. True Prob ' + (
        #     '[Mean]' if prob_type == '0' else '[Median]'))
        # plt.title('Scatter')
        plt.xlabel('App. True Prob ' + (
            '[Mean]' if prob_type == '0' else '[Median]'))
        plt.ylabel('Rank ' + str(i + 1) + ' Prob')
        plt.savefig(str(i + 1) + '.png', dpi=res)
        plt.close()

    os.chdir('../../../../../')
    # GETTING THE ROUND 1 SLOTS AND SEEDS
    os.chdir('Data/' + year + '/' + year + '-Round1/')
    round1Slots = pd.read_csv("Slots" + year + "-Round1.csv", header=None)

    # GET THE INDEX OF ROUND 1 MATCH UPS AND GET CORRESPONDING PREDICTIONS
    index_list = []
    for i in range(len(round1Slots)):
        team_match_up = get_team_match_up(year, round1Slots.iloc[i, 2], round1Slots.iloc[i, 3])
        indx = unique_match_ups.index[unique_match_ups[0] == team_match_up]
        index_list.append(indx[0])

    topRanks_round1 = topRanks.iloc[index_list]
    trueProb_round1 = trueProb.iloc[index_list]

    # CREATE SCATTER PLOT WITH ROUND 1 PROBABILITIES ONLY
    print("\tRound 1 Probabilities...")
    os.chdir('../../../topSubmissions/Expected Payout' + (
        '/Mean Prob/' if prob_type == '0' else '/Median Prob/') + year + '/Simulation_' + str(sim_index))
    for i in range(len(topRanks_round1.T)):
        plt.plot([0, 1], c='r')
        plt.scatter(trueProb_round1, topRanks_round1.iloc[:, i], alpha=alp, s=ar)
        plt.title('Round 1 Probabilities')
        plt.xlabel('App. True Prob ' + (
            '[Mean]' if prob_type == '0' else '[Median]'))
        plt.ylabel('Rank ' + str(i + 1) + ' Prob')
        plt.savefig('Round1_' + str(i + 1) + '.png', dpi=res)
        plt.close()

    print("Done")


if __name__ == "__main__":
    print('Starting program with following parameters...')
    initialize()
