
# How to simulate with user-defined worlds

1. Copy the "worlds" folder into the root directory "Wumpus_World".
2. In IDE go to Run > Edit Configurations > in the arguments field add "-f worlds output.txt" without quotation marks.
    * -f will force the program to use user-defined worlds;
	* worlds is a folder with user-defined worlds;
	* output.txt is a file in the root folder that will contain the results.
3. Check the agent's actions in SearchAI.java (move right, grad, move left, climb).
4. Run the simulation and check the output.txt. Not helpful! Because it contains summary statistics (mean and stdev) of both simulations.
5. To see the results of one simulation - leave only one file (delete the other one because both are stored also in the folder "worlds_all") and run again:
    * easy_gold.txt has gold in [0,1], so the score should be 994.0;
	* easy_wumpus.txt has wumput in [0,1], so the score will be -1001.0.

Instructions how to create worlds are on the project's page in Studium.
