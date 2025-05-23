# KI_Project_Group13

## Usage:

java -jar TSP_EA.jar **[options]**

options:

          -f, --file <path>           Path to the TSPLIB instance file (e.g., data/berlin52.tsp)
          -n, --numCities <n>         Number of cities for a randomly generated TSP instance (if -f is not used). Default: 50
          -m, --mu <n>                Parent population size (μ). Default: 50
          -l, --lambda <n>            Offspring population size (λ). Default: 100
          -g, --generations <n>       Number of generations to run. Default: 1000
          -mr, --mutationRate <rate>  Probability of mutation (0.0-1.0). Default: 0.2
          -cx, --crossover <type>     Crossover type (OX, PMX, ERX). Default: OX
          -mt, --mutation <type>      Mutation type (SWAP, INSERT, INVERT). Default: SWAP
          -s, --selection <type>      Parent selection type (TOURNAMENT, ROULETTE). Default: TOURNAMENT
          -ts, --tournamentSize <n>   Tournament size for tournament selection. Default: 3
          --greedy                    Use greedy initialization for part of the initial population. Default: false
          -q, --quiet                 Suppress detailed generational output. Default: false (verbose)
          -o, --output <filename>     File to save the best tour results. Default: best_tour_results.txt
          -h, --help                  Show this help message

