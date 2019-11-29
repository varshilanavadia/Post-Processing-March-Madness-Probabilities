#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Created on Thur Nov  14 11:06:22 2019

@author: vra24
"""

import glob
import os
import sys
import numpy as np
import pandas as pd
import matplotlib
matplotlib.use('Agg')
import matplotlib.pyplot as plt


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

    os.chdir("../Results/" + year + "/Simulation_" + str(sim_index) +
             ("/Mean/LogLoss_Leaderboard/" if prob_type == '0' else "/Median/LogLoss_Leaderboard/"))

    # data_files = sorted(os.listdir("../Results/" + year + "/Simulation_" + str(sim_index) + ("/Mean/LogLoss_Leaderboard/" if prob_type == '0' else "/Median/LogLoss_Leaderboard/")))
    print('Getting all LogLoss_Leaderboard.csv files...')
    data_files = sorted(glob.glob('*.csv'))

    # INITIATE VARIABLE TO IMPORT ALL RANKS FROM .CSV FILES AND IMPORT DATA
    top_sub = np.zeros(shape=(len(data_files), 5, 2))
    count = 0
    print('Pooling data into numpy array...')
    for file in data_files:
        if count % 5000 == 0:
            print('\t' + str(count * 100 / len(data_files)) + str('%') + "  |  Iteration : " + str(count))
        my_data = np.genfromtxt(file, delimiter=",")
        my_data = my_data[1:6, (0, 2)]
        top_sub[count] = my_data.copy()
        count += 1

    # CONVERT TO INT
    top_sub = top_sub.astype(int)

    # CALCULATE THE NUMBER OF WINS OF THE TOP SUBMISSIONS
    print('Calculating the count of top submissions...')
    count_map = {}
    for i in range(len(top_sub[0])):
        unique, counts = np.unique(top_sub[:, i, 1], return_counts=True)
        count_map[i] = dict(zip(unique, counts))
    df = pd.DataFrame(count_map)
    df = df.sort_values(by=0, ascending=False)
    # CHANGING COLUMN NAMES
    df.columns = ['Rank 1', 'Rank 2', 'Rank 3', 'Rank 4', 'Rank 5']

    os.chdir('../../../../../topSubmissions/Number of wins' + ('/Mean Prob/' if prob_type == '0' else '/Median Prob/') + year + '/')

    if year == '2016' and prob_type == '0':
        print('Writing \'Number of wins\' [2016-Mean] to .csv file ...')
        df.to_csv('top_sub-Sim_' + str(sim_index) + '-2016-Mean.csv')
    elif year == '2016' and prob_type == '1':
        print('Writing \'Number of wins\' [2016-Median] to .csv file ...')
        df.to_csv('top_sub-Sim_' + str(sim_index) + '-2016-Median.csv')
    elif year == '2017' and prob_type == '0':
        print('Writing \'Number of wins\' [2017-Mean] to .csv file ...')
        df.to_csv('top_sub-Sim_' + str(sim_index) + '-2017-Mean.csv')
    elif year == '2017' and prob_type == '1':
        print('Writing \'Number of wins\' [2017-Median] to .csv file ...')
        df.to_csv('top_sub-Sim_' + str(sim_index) + '-2017-Median.csv')

    df['Rank 1'] = (df['Rank 1'] * 10000) / len(data_files)
    df['Rank 2'] = (df['Rank 2'] * 7000) / len(data_files)
    df['Rank 3'] = (df['Rank 3'] * 5000) / len(data_files)
    df['Rank 4'] = (df['Rank 4'] * 2000) / len(data_files)
    df['Rank 5'] = (df['Rank 5'] * 1000) / len(data_files)

    # ADDING COLUMN FOR TOTAL NUMBER OF WINS AND CALCULATING IT
    df.insert(5, 'Total', 0)
    df['Total'] = df.sum(axis=1)

    os.chdir('../../../Expected Payout' + (
        '/Mean Prob/' if prob_type == '0' else '/Median Prob/') + year)
    if os.path.exists('Simulation_' + str(sim_index)):
        os.chdir('Simulation_' + str(sim_index))
    else:
        os.mkdir('Simulation_' + str(sim_index))
        os.chdir('Simulation_' + str(sim_index))

    if year == '2016' and prob_type == '0':
        print('Writing \'Expected Payout\' [2016-Mean] to .csv file ...')
        df.to_csv('exp_pay-Sim_' + str(sim_index) + '-2016-Mean.csv')
    elif year == '2016' and prob_type == '1':
        print('Writing \'Expected Payout\' [2016-Median] to .csv file ...')
        df.to_csv('exp_pay-Sim_' + str(sim_index) + '-2016-Median.csv')
    elif year == '2017' and prob_type == '0':
        print('Writing \'Expected Payout\' [2017-Mean] to .csv file ...')
        df.to_csv('exp_pay-Sim_' + str(sim_index) + '-2017-Mean.csv')
    elif year == '2017' and prob_type == '1':
        print('Writing \'Expected Payout\' [2017-Median] to .csv file ...')
        df.to_csv('exp_pay-Sim_' + str(sim_index) + '-2017-Median.csv')

    os.chdir('../../../../../')
    print('Generating scatter plots...')

    # GETTING SUBMISSION NUMBER OF TOP 5 SUBMISSIONS
    rank = df.T.columns.values
    rank = rank[0:5]
    os.chdir('Other/')

    # GETTING CSV FILE LIST
    csvList = pd.read_csv("csvFileList" + year + ".txt", header=None)
    csvList.columns = ['Names']
    csvList.to_csv('csvList.csv')

    # GETTING MAIN PREDICTIONS MATRIX
    os.chdir('../Results/' + year + '/')
    predictions = pd.read_csv("Predictions-" + year + ".csv", header=None)

    # GETTING TOP 5 PREDICTIONS FROM MATRIX
    topRanks = predictions.iloc[:, rank - 1]
    topRanks.columns = ['Rank 1', 'Rank 2', 'Rank 3', 'Rank 4', 'Rank 5']

    # GETTING MEDIAN PREDICTIONS
    trueProb = pd.read_csv(("Mean" if prob_type == '0' else "Median") + "-True-Prob-" + year + ".csv", header=None)

    os.chdir('../../topSubmissions/Expected Payout' + ('/Mean Prob/' if prob_type == '0' else '/Median Prob/') + year + '/Simulation_' + str(sim_index))
    # DEFINING SCATTER PLOT PARAMETERS
    alp = 0.5
    ar = 10
    for i in range(len(topRanks.T)):
        plt.plot([0, 1], c='r')
        plt.scatter(trueProb, topRanks.iloc[:, i], alpha=alp, s=ar)
        # plt.title('Rank ' + str(i + 1) + 'v/s App. True Prob ' + (
        #     '[Mean]' if prob_type == '0' else '[Median]'))
        # plt.title('Scatter')
        plt.xlabel('App. True Prob ' + (
            '[Mean]' if prob_type == '0' else '[Median]'))
        plt.ylabel('Rank ' + str(i+1) + ' Prob')
        plt.savefig(str(i+1) + '.png', dpi=720)
        plt.close()

    os.chdir('../../../../../')

    print('Done...')


if __name__ == "__main__":
    print('Starting program with following parameters...')
    initialize()
