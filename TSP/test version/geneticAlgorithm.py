# A Test module for improving efficiency!!!!




import matplotlib.pyplot as plt
import matplotlib as mpl
import matplotlib.animation as animation
from matplotlib.ticker import MultipleLocator, FormatStrFormatter
import numpy as np
import pandas as pd
import itertools
import random
import copy
import math
import time
import sys
import os

cities_list = []
cities_seed = []
dist_dict = {}

class City(object):
	"""
	Store cities as City object.

	self.id         : an int storing city ID.
	self.coordinate : a 2D numpy array storing the coordinate of a city.

	"""
	def __init__(self, city_id='', x=0.0, y=0.0, blank_city=False):
		if blank_city == True:
			self.id = ''
			self.coordinate = np.array((0.0, 0.0))
		else:
			self.id = city_id
			self.coordinate = np.array((x, y))


class Route(object):
	"""
	Representation of the individual in the generation.
	
	self.route    : a permutation of City objects.
	self.fitness  : the total lenght of a route.

	"""
	def __init__(self, empty_route=False):
		if empty_route == True:
			self.route = [None for i in range(len(cities_list))]
			self.fitness = 0.0
		else:
			# initialize a permutation of cities as an individual
			random.seed(time.time())
			random.shuffle(cities_seed)
			self.route = cities_seed[:]
			# calculate the length of the route
			self.fitness_evaluation()


	def fitness_evaluation(self):
		self.fitness = 0.0
		c2 = self.route[:-1]
		c2.insert(0, self.route[-1])
		self.fitness = sum(dist_dict[(min(s, t), max(s, t))] for s, t in zip(self.route, c2))


	def print_route(self):
		for i in range(0, len(self.route), 20):
			for j in range(i, min(i + 20, len(self.route))):
				print "%3d, " % self.route[j],
			if i == len(self.route) / 20 * 20:
				print '\b\b\b.'
			else:
				print '\n'


class Population(object):
	"""
	To manage population for each generation.

	self.pop_size          : population size.
	self.population        : list of Route objects (individuals in the generation).
	self.fittest_route     : the route object of the fittest.
	self.current_best      : the best individual in the current generation.
	self.current_average   : the mean fitness
	self.current_worst     : the worst fitness

	Individual (genotype) of the population is a Route object.

	"""
	def __init__(self, pop_size, init_flag):
		self.pop_size = pop_size	
		if init_flag == True:
			self.population = [Route() for i in range(pop_size)]
		else:
			self.population = []

	# get the route with the highest fitness
	def fittest_individual(self):
		#self.fittest_route = min(self.population, key=lambda rt: rt.fitness)
		self.fittest_route = sorted(self.population, key=lambda rt: rt.fitness, reverse=False)[0]
		return self.fittest_route


	def get_runtime_fitness(self):
		fitness_ranking = sorted(self.population, key=lambda rt: rt.fitness, reverse=False)
		self.fittest_route = fitness_ranking[0]
		self.current_best = fitness_ranking[0].fitness
		self.current_worst = fitness_ranking[-1].fitness

		length_data = np.array([i.fitness for i in self.population])
		self.current_average = np.mean(length_data)


	def population_summary(self, generation_i):
		self.get_runtime_fitness()

		if generation_i == 0:
			print "The initial context of the population: "
			print "#%3d  Best Fitness : %.6f\t\tAverage Fitness : %.6f\t\tWorst Fitness : %.6f" % (generation_i, self.current_best, self.current_average, self.current_worst)
			
		else:
			print "#%3d  Best Fitness : %.6f\t\tAverage Fitness : %.6f\t\tWorst Fitness : %.6f" % (generation_i, self.current_best, self.current_average, self.current_worst)

		return (self.fittest_route, self.current_best, self.current_average, self.current_worst)


class GA(object):
	"""
	Implementations of selection (parents & survivors), recombination (crossover), mutation (permutation).

	"""

	# Parent selection: tournament selection
	def parent_selection(self, population, tournament_size, replacement=False):
		self.mate_population = Population(population.pop_size, False)
		for i in range(population.pop_size):
			random.seed(time.time())
			competitors = [population.population[random.randint(0, population.pop_size - 1)] for k in range(tournament_size)]
			winner = sorted(competitors, key=lambda rt: rt.fitness, reverse=False)[0]
			self.mate_population.population.append(winner)

		return self.mate_population


	# Crossover: PMX scheme
	def PMX_crossover(self, parent_1, parent_2, xover_pr):

		# clone two offspring of the parents
		self.child1 = Route(empty_route=True)
		self.child2 = Route(empty_route=True)

		self.xover_pos_1 = random.randrange(0, len(parent_1.route) - 1)
		random.seed(time.time())
		self.xover_pos_2 = random.randrange(0, len(parent_1.route) - 1)
		random.seed(time.time())
		self.rand_pr = random.random()

		if self.rand_pr > xover_pr:
			return (parent_1, parent_2)
		else:
			l = min(self.xover_pos_1, self.xover_pos_2)
			r = max(self.xover_pos_1, self.xover_pos_2)

			self.child1.route[l:r+1] = parent_1.route[l:r+1]
			self.child2.route[l:r+1] = parent_2.route[l:r+1]

			# Composition of child 1
			self.uncopied_id_1 = set(parent_2.route) - set(parent_1.route[l:r+1])
			#self.uncopied_id_1 = [city.id for city in parent_2.route if city.id not in parent_1.get_city_ids(parent_1.route[l:r+1])]
			for i in range(len(parent_1.route)):
				index_1 = (l + i) % len(parent_1.route)
				if parent_2.route[index_1] in self.uncopied_id_1:
					self.fill_in(self.child1.route, parent_2.route, parent_2.route[index_1], index_1)
			
			# Re-evaluate fitness of child 1
			self.child1.fitness_evaluation()

			# Composition of child 2
			self.uncopied_id_2 = set(parent_1.route) - set(parent_2.route[l:r+1])
			#self.uncopied_id_2 = [city.id for city in parent_1.route if city.id not in parent_2.get_city_ids(parent_2.route[l:r+1])]
			for i in range(len(parent_1.route)):			
				index = (l + i) % len(parent_1.route)
				if parent_1.route[index] in self.uncopied_id_2:
					self.fill_in(self.child2.route, parent_1.route, parent_1.route[index], index)
			
			# Re-evaluate fitness of child 2
			self.child2.fitness_evaluation()

		return (self.child1, self.child2)


	def fill_in(self, child, parent, gene, i):
		if child[i] == None:
			child[i] = gene
		else:
			while child[i] != None:
				i = parent.index(child[i])
			child[i] = gene
			

	# Mutation: default scheme is Inversion Mutation.
	def mutation(self, parent, mut_pr, scheme="inversion"):
		random.seed(time.time())
		self.pr = random.random()

		if self.pr > mut_pr:
			return parent

		# Inversion scheme
		if scheme == "inversion":
			# generate a random number to check whether the parent will be mutated
			self.m_pos_1 = random.randrange(0, len(parent.route) - 1)
			random.seed(time.time())
			self.m_pos_2 = random.randrange(0, len(parent.route) - 1)
			if self.m_pos_1 != self.m_pos_2:
				self.left = min(self.m_pos_1, self.m_pos_2)
				self.right = max(self.m_pos_1, self.m_pos_2)
				# inverse the elements between left and right.
				parent.route[self.left:self.right+1] = reversed(parent.route[self.left:self.right+1])
				parent.fitness_evaluation()
				return parent
			return parent

		# Swap scheme
		if scheme == "swap":
			self.m_pos_1 = random.randrange(0, len(parent.route) - 1)
			random.seed(time.time())
			self.m_pos_2 = random.randrange(0, len(parent.route) - 1)
			if self.m_pos_1 != self.m_pos_2:
				# Swap two genes
				parent.route[self.m_pos_1], parent.route[self.m_pos_2] = parent.route[self.m_pos_2], parent.route[self.m_pos_1]
				parent.fitness_evaluation()
				return parent
			return parent

		# Insert scheme
		if scheme == "insert":
			self.m_pos_1 = random.randrange(0, len(parent.route) - 1)
			random.seed(time.time())
			self.m_pos_2 = random.randrange(0, len(parent.route) - 1)
			if self.m_pos_1 != self.m_pos_2:
				obj = parent.route.pop(self.m_pos_2)
				parent.route.insert(self.m_pos_1 + 1, obj)
				parent.fitness_evaluation()
				return parent
			return parent


	# Survivor selection: deterministic methods -> ranking all the parents and offspring
	def survivor_selection(self, mate_population, offspring_population):
		self.mate_population.population.extend(offspring_population.population)
		self.next_generation = sorted(self.mate_population.population, key=lambda rt: rt.fitness, reverse=False)[0:mate_population.pop_size]
		return self.next_generation


	# The evolution of a current generation
	def evolution(self, cur_population, parameters):
		# Step 1: create mating pool (stochastic), the size of mating pool = the size of population
		self.parent_selection(cur_population, parameters[1], replacement=False)

		# Step 2: recombine two consecutive parents in the mating pool, and reproduce two offspring after crossover.
		self.kids_population = Population(cur_population.pop_size, False)
		for i in range(0, cur_population.pop_size / 2):
			self.child1, self.child2 = self.PMX_crossover(self.mate_population.population[i], self.mate_population.population[cur_population.pop_size - 1 - i], parameters[2])
			self.kids_population.population.append(self.child1)
			self.kids_population.population.append(self.child2)

		# Step 3: mutation the offspring.
		for i in range(0, self.kids_population.pop_size):
			self.kids_population.population[i] = copy.deepcopy(self.mutation(self.kids_population.population[i], parameters[3], parameters[4]))

		# Step 4: survivor selection among parent and offspring (elitism as default).
		return self.survivor_selection(cur_population, self.kids_population)


class Session(object):
	"""
	Implementation of a GA run.

	Load parameter settings from a file.
	Load city list from a file

	"""
	def __init__(self, parameters_file, city_file):
		self.city_file_name = city_file.strip().split('_')[3][:-4]
		# Loading the parameters of the current run.
		with open(parameters_file, 'r') as infile:
			for line in infile:
				line_data = line.strip().split('\t')
				if line_data[0] == "mutation_probability":
					self.mutation_pr = float(line_data[1])

				if line_data[0] == "crossover_probability":
					self.xover_pr = float(line_data[1])

				if line_data[0] == "max_generation":
					self.max_generation = int(line_data[1])

				if line_data[0] == "population_size":
					self.pop_size = int(line_data[1])

				if line_data[0] == "tournament_size":
					self.tournament_size = int(line_data[1])

				if line_data[0] == "elitism":
					self.elitism = bool(line_data[1])

				if line_data[0] == "mutation_scheme":
					self.mut_scheme = line_data[1]

		self.parameters = (self.pop_size, self.tournament_size, self.xover_pr, self.mutation_pr, self.mut_scheme)
		# Initialize cities_list
		self.read_cities(city_file)
		# Construct dist_dict for fitness evaluation
		self.construct_dist_ref()

	# read cities from a file
	def read_cities(self, city_file):
		with open(city_file, 'r') as infile:
			for line in infile:
				line_data = line.strip().split(' ')
				# Add new_city into the global variable "cities_list"
				cities_list.append(City(int(line_data[0]), float(line_data[1]), float(line_data[2])))
				cities_seed.append(int(line_data[0]))


	# Create the global distance reference for all chromosomes
	def construct_dist_ref(self):
		for i in range(len(cities_list) - 1):
			for j in range(i, len(cities_list)):
				dist_dict[(cities_list[i].id, cities_list[j].id)] = np.linalg.norm(cities_list[i].coordinate - cities_list[j].coordinate)


	def run(self):

		self.run_summary = []

		# Initialize the population of the first generation
		self.tsp_population = Population(self.pop_size, init_flag=True)
		self.best_solution = self.tsp_population.population_summary(0)[0]

		# the evolution of the population for max_generation generations
		start_time = time.time()
		for i in range(self.max_generation):
			self.tsp_population.population = GA().evolution(self.tsp_population, self.parameters)
			self.fitness_info = self.tsp_population.population_summary(i+1)
			self.run_summary.append(self.fitness_info[1:])

			if self.fitness_info[0].fitness < self.best_solution.fitness:
				self.best_solution = self.fitness_info[0]
		self.time_consumed = time.time() - start_time

		timestamp = time.strftime('%m-%d_%H-%M-%S', time.localtime())
		self.image_name     = timestamp + '_EA_behaviours_' + self.city_file_name + '.eps'
		self.fit_filename   = timestamp + '_EA_fitnesses_' + self.city_file_name + '.tsv'
		# Display the summary of the current run: the time consumed and the information of the best route
		self.summary(save=True)


	def summary(self, save=True):
		if save == True:
			df = pd.DataFrame.from_records(self.run_summary)
			df.to_csv(self.fit_filename, sep='\t', header=False)

		print "Time consumed for the run         :  %.2f seconds." % self.time_consumed
		print "The route length of the solution  :  %.6f " % self.best_solution.fitness
		print "Final solution route of the run   :  "
		# Display the final solution
		self.best_solution.print_route()


	def show_behaviour(self, run_time, from_file=False):

		if from_file == True:
			x = range(self.max_generation)
			yb, yw, ya = [], [], []
			with open(self.fit_filename, 'r') as infile:
				for line in infile:
					line_data = line.strip().split('\t')
					yb.append(float(line_data[1]))
					yw.append(float(line_data[2]))
					ya.append(float(line_data[3]))

		if from_file == False:
			x = range(self.max_generation)
			yb = [i[0] for i in self.run_summary]
			ya = [i[1] for i in self.run_summary]
			yw = [i[2] for i in self.run_summary]

		mpl.style.use('default')

		plt.figure(run_time)
		ax = plt.gca()
		plt.plot(x, yb, c='C1', linestyle="-", label="Best Fitness")
		plt.plot(x, yw, c='C2', linestyle=":", label="Worst Fitness")
		plt.plot(x, ya, c='C6', linestyle="--", label="Mean Fitness")

		plt.title("Behaviours of Evolutionary Algorithm for TSP")
		plt.legend(loc='upper right')
		plt.xlim(0, self.max_generation)
		plt.ylim(yb[-1] - 5000, yw[0] + 5000)
		ax.xaxis.set_major_locator( MultipleLocator(len(ya) / 10) )
		ax.xaxis.set_minor_locator( MultipleLocator(len(ya) / 100) )
		ax.xaxis.set_major_locator( MultipleLocator(len(ya) / 10) )
		ax.xaxis.set_major_formatter(FormatStrFormatter('%d'))
		plt.xlabel("Time")
		plt.ylabel("EA Behaviours")
		plt.savefig(self.image_name, format='eps', dpi=5000)
		ax.xaxis.grid(True, which='major', linestyle='dotted')
		ax.yaxis.grid(True, which='major', linestyle='dotted')
		#plt.show()
