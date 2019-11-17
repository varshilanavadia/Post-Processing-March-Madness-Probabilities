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


def initialize():
    year = sys.argv[1]
    prob_type = sys.argv[2]
    sim_index = sys.argv[3]

    # year = '2016'
    # prob_type = 0
    # sim_index = 0

    os.chdir("../Results/" + year + "/Simulation_" + str(sim_index) +
             ("/Mean/LogLoss_Leaderboard/" if prob_type == '0' else "/Median/LogLoss_Leaderboard/"))

    # data_files = sorted(os.listdir("../Results/" + year + "/Simulation_" + str(sim_index) + ("/Mean/LogLoss_Leaderboard/" if prob_type == '0' else "/Median/LogLoss_Leaderboard/")))
    print('Getting all .csv files...')
    data_files = glob.glob('*.csv')

    top_sub = np.zeros(shape=(len(data_files), 5, 2))
    count = 0
    print('Pooling data into numpy array...')
    for file in data_files:
        if count % 5000 == 0:
            print(str(count * 100 / len(data_files)) + str('%') + "  |  Iteration : " + str(count))
        my_data = np.genfromtxt(file, delimiter=",")
        my_data = my_data[1:6, (0, 2)]
        top_sub[count] = my_data.copy()
        count += 1

    print()
    top_sub = top_sub.astype(int)

    print('Calculating the count of top submissions...')
    count_map = {}
    for i in range(len(top_sub[0])):
        unique, counts = np.unique(top_sub[:, i, 1], return_counts=True)
        count_map[i] = dict(zip(unique, counts))
    df = pd.DataFrame(count_map)
    df = df.sort_values(by=0, ascending=False)

    # os.chdir('../../../../../topSubmissions/')
    if year == '2016' and prob_type == '0':
        print('Writing data frame to .csv file [2016-Mean]...')
        df.to_csv('../../../../../topSubmissions/top_sub-Sim_' + str(sim_index) + '-2016-Mean.csv')
    elif year == '2016' and prob_type == '1':
        print('Writing data frame to .csv file [2016-Median]...')
        df.to_csv('../../../../../topSubmissions/top_sub-Sim_' + str(sim_index) + '-2016-Median.csv')
    elif year == '2017' and prob_type == '0':
        print('Writing data frame to .csv file [2017-Mean]...')
        df.to_csv('../../../../../topSubmissions/top_sub-Sim_' + str(sim_index) + '-2017-Mean.csv')
    elif year == '2017' and prob_type == '1':
        print('Writing data frame to .csv file [2017-Median]...')
        df.to_csv('../../../../../topSubmissions/top_sub-Sim_' + str(sim_index) + '-2017-Median.csv')

    print('Done...')


if __name__ == "__main__":
    print('Starting program...')
    initialize()
