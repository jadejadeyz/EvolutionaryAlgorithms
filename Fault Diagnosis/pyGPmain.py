from matplotlib import pyplot as plt
import networkx as nx
from deap import base, creator, gp, tools, algorithms
from scoop import futures, IS_ORIGIN
import pandas as pd
import operator
import math
import random
import numpy
import time

# Load training data into a list of tuple: (time, I?)
filepath = "/Users/yzhang/Documents/EvolutionaryAlgorithms/Fault Diagnosis/lessCircles/"
filename = "m2_healthy_85_I1_test"
suffix = ".txt"
trainData = pd.read_csv(filepath + filename + suffix, skipinitialspace=True, sep='\t')
train_x = trainData['time']
train_i1 = trainData['current']
# train_i2 = trainData['I2']
# train_i3 = trainData['I3']
fitnessCases_i1 = [i for i in zip(train_x, train_i1)]
# fitnessCases_i2 = [i for i in zip(train_x, train_i2)]
# fitnessCases_i3 = [i for i in zip(train_x, train_i3)]


# Load test data, these data are used to test the classification quality
testData = pd.read_csv("/Users/yzhang/Documents/EvolutionaryAlgorithms/Fault Diagnosis/lessCircles/test_m2_H_85_Current.txt", skipinitialspace=True, sep='\t')
test_x = testData['time']
test_i1 = testData['I1']
test_i2 = testData['I2']
test_i3 = testData['I3']
test_points_i1 = [i for i in zip(test_x, test_i1)]
test_points_i2 = [i for i in zip(test_x, test_i2)]
test_points_i3 = [i for i in zip(test_x, test_i3)]


# Define new functions
def pDiv(left, right):
    try:
        return left / right
    except ZeroDivisionError:
        return 0.1


# Start GP Configuration
pset = gp.PrimitiveSet("MAIN", 1)
pset.addPrimitive(operator.add, 2)
pset.addPrimitive(operator.sub, 2)
pset.addPrimitive(operator.mul, 2)
pset.addPrimitive(pDiv, 2)
pset.addPrimitive(math.sin, 1)
if not IS_ORIGIN:
    pset.addEphemeralConstant("randConsts", lambda: random.randint(-1, 1))

pset.renameArguments(ARG0='t')

creator.create("FitnessMin", base.Fitness, weights=(-1.0,))
creator.create("Individual", gp.PrimitiveTree, fitness=creator.FitnessMin)

toolbox = base.Toolbox()
toolbox.register("expr", gp.genHalfAndHalf, pset=pset, min_=1, max_=3)
toolbox.register("individual", tools.initIterate, creator.Individual, toolbox.expr)
toolbox.register("population", tools.initRepeat, list, toolbox.individual)
toolbox.register("compile", gp.compile, pset=pset)


def evalFormula(individual, points):
    # Transform the tree expression in a callable function
    func = toolbox.compile(expr=individual)
    # Evaluate the mean squared error
    sqerrors = ((func(case[0]) - case[1]) ** 2 for case in points)
    return math.fsum(sqerrors) / len(points),


def testModel(individual, testPoints):
    func = toolbox.compile(expr=individual)
    sqerrors = (math.fabs(func(case[0]) - case[1]) for case in testPoints)
    return math.fsum(sqerrors) / len(testPoints),


toolbox.register("testModel", testModel)
toolbox.register("evaluate", evalFormula, points=fitnessCases_i1)
toolbox.register("select", tools.selTournament, tournsize=10)
toolbox.register("mate", gp.cxOnePoint)
toolbox.register("expr_mut", gp.genFull, min_=0, max_=2)
toolbox.register("mutate", gp.mutUniform, expr=toolbox.expr_mut, pset=pset)
toolbox.register("map", futures.map)

toolbox.decorate("mate", gp.staticLimit(key=operator.attrgetter("height"), max_value=18))
toolbox.decorate("mutate", gp.staticLimit(key=operator.attrgetter("height"), max_value=18))


def main():
    random.seed(1024)

    pop = toolbox.population(n=100)
    hof = tools.HallOfFame(1)

    stats_fit = tools.Statistics(lambda ind: ind.fitness.values)
    stats_size = tools.Statistics(len)

    mstats = tools.MultiStatistics(fitness=stats_fit, size=stats_size)
    mstats.register("Mean", numpy.mean)
    mstats.register("Std", numpy.std)
    mstats.register("Best", numpy.min)
    mstats.register("Worst", numpy.max)

    pop, log = algorithms.eaSimple(pop, toolbox, 0.9, 0.1, 3000, stats=mstats,
                                   halloffame=hof, verbose=True)

    print(min(toolbox.testModel(hof[0], test_points_i1), toolbox.testModel(hof[0], test_points_i2), toolbox.testModel(hof[0], test_points_i3)))

    outfilename = filename + time.strftime(" %H_%M_%S %d_%b_%Y", time.localtime())  + '.gp'
    with open(outfilename, "w") as outfile:
        outfile.write(str(hof[0]))
    outfile.close()

    return pop, log, hof


if __name__ == "__main__":
    pop, log, hof = main()