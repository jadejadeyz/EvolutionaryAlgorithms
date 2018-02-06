import matplotlib.pyplot as plt
import matplotlib as mpl
import matplotlib.animation as animation
import numpy as np
import pandas as pd
import random
import copy
import math
import time
import os

cities_list = []

class City(object):
	"""
	Store cities as City object.

	self.id         : an int storing city ID.
	self.coordinate : a 2D numpy array storing the coordinate of a city.
	self.dist_dict  : a dictionary storing the distances "self" to others cities.

	"""
	def __init__(self, city_id, x, y, dist_dict=None):
		self.id = city_id
		self.coordinate = np.array((x, y))
		self.dist_dict = {self.id : 0.0}
		if dist_dict:
			self.dist_dict = dist_dict
		cities_list.append(self)


	def distance_to_others(self):
		for city in cities_list:
			temp_dist = self.euclidean_distance(self.coordinate, city.coordinate)
			self.dist_dict[city.id] = temp_dist


	def euclidean_distance(self, source, target):
		return np.linalg.norm(source - target)


	def print_dist(self):
		for i in self.dist_dict.keys():
			print i, "  : ", self.dist_dict[i]


class Route(object):
	"""
	Representation of the individual in the generation.
	
	self.route    : a permutation of City objects.
	self.fitness  : the total lenght of a route.

	"""
	def __init__(self):
		# initialize a permutation of cities as an individual
		self.route = copy.deepcopy(sorted(cities_list, key=lambda *args : random.random()))
		# calculate the length of the route
		self.fitness_evaluation()


	def fitness_evaluation(self):
		self.fitness = 0.0

		for city in self.route:
			next_city = self.route[self.route.index(city) - len(self.route) + 1]
			dist_between = city.dist_dict[next_city.id]
			self.fitness += dist_between


	def get_city_ids(self, route):
		self.id_list = [city.id for city in route]
		return self.id_list


	def print_route(self):
		output = ''
		for city in self.route:
			output += city.id + ', '
		print '[', output[:-2], ']'


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
		self.population = []
		if init_flag == True:
			self.population = [Route() for i in range(pop_size)]

	# get the route with the highest fitness
	def fittest_individual(self):
		self.fittest_route = sorted(self.population, key=lambda rt: rt.fitness, reverse=False)[0]
		return self.fittest_route

	# get the value of the best fitness
	def get_best_fitness(self):
		self.current_best = sorted(self.population, key=lambda rt: rt.fitness, reverse=False)[0].fitness
		return self.current_best

	# get the value of the mean fitness
	def get_average_fitness(self):
		length_data = np.array([i.fitness for i in self.population])
		self.current_average = np.mean(length_data)
		return self.current_average

	# get the value of the worst fitness
	def get_worst_fitness(self):
		self.current_worst = sorted(self.population, key=lambda rt: rt.fitness, reverse=True)[0].fitness
		return self.current_worst


	def population_summary(self, generation_i):
		best = self.get_best_fitness()
		worst = self.get_worst_fitness()
		mean = self.get_average_fitness()

		print "#%3d  Best Fitness : %.6f\t\t Average Fitness : %.6f\t\t Worst Fitness : %.6f" % (generation_i, best, mean, worst)

		return (best, worst, mean)


class GA(object):
	"""
	Implementations of selection (parents & survivors), recombination (crossover), mutation (permutation).

	"""

	# Parent selection: tournament selection with replacement
	def parent_selection(self, population, tournament_size):
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
		self.child1 = Route()
		self.child2 = Route()

		for city in self.child1.route:
			city.id = ''

		for city in self.child2.route:
			city.id = ''

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

			self.child1.route[l:r+1] = copy.deepcopy(parent_1.route[l:r+1])
			self.child2.route[l:r+1] = copy.deepcopy(parent_2.route[l:r+1])

			# Composition of child 1
			self.uncopied_id_1 = [city.id for city in parent_2.route if city.id not in parent_1.get_city_ids(parent_1.route[l:r+1])]
			for i in range(len(parent_1.route)):
				index_1 = (l + i) % len(parent_1.route)
				if parent_2.route[index_1].id in self.uncopied_id_1:
					self.fill_in(self.child1.route, parent_2.route, parent_2.route[index_1], index_1)
			
			# Re-evaluate fitness of child 1
			self.child1.fitness_evaluation()

			# Composition of child 2
			self.uncopied_id_2 = [city.id for city in parent_1.route if city.id not in parent_2.get_city_ids(parent_2.route[l:r+1])]
			for i in range(len(parent_1.route)):			
				index = (l + i) % len(parent_1.route)
				if parent_1.route[index].id in self.uncopied_id_2:
					self.fill_in(self.child2.route, parent_1.route, parent_1.route[index], index)
			
			# Re-evaluate fitness of child 2
			self.child2.fitness_evaluation()

		return (self.child1, self.child2)


	def fill_in(self, child, parent, gene, i):
		if child[i].id == '':
			child[i] = copy.deepcopy(gene)
		else:
			city_name = child[i].id
			update_index = self.slide(parent, city_name)
			self.fill_in(child, parent, gene, update_index)


	def slide(self, route, name):
		for city in route:
			if city.id == name:
				return route.index(city)


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


	# Survivor selection: derterministic methods -> ranking all the parents and offsprings
	def survivor_selection(self, mate_population, offspring_population):
		self.all_members = [i for i in mate_population.population]
		self.all_members.extend(offspring_population.population)
		self.all_members = sorted(self.all_members, key=lambda rt: rt.fitness, reverse=False)
		self.next_generation = self.all_members[0:mate_population.pop_size]
		return self.next_generation


	# The evolution of a current generation
	def evolution(self, cur_population, parameters):
		# Step 1: create mating pool (stochastic), the size of mating pool = the size of population
		self.parent_selection(cur_population, parameters[1])

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
		# Loading the parameters of the current run.
		self.image_name = 'EA_behaviours_' + city_file.strip().split('_')[3][:-4] + '.eps'
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
		# Loading city list
		self.read_cities(city_file)

	# read cities from a file
	def read_cities(self, city_file):
		with open(city_file, 'r') as infile:
			for line in infile:
				line_data = line.strip().split(' ')
				new_city = City(line_data[0], float(line_data[1]), float(line_data[2]))

		for city in cities_list:
			city.distance_to_others()

	def run(self):
		
		self.run_summary = []

		# Initialize the population of the first generation
		self.tsp_population = Population(self.pop_size, init_flag=True)
		self.best_solution = self.tsp_population.fittest_individual()

		# the evolution of the population for max_generation generations
		start_time = time.time()
		for i in range(self.max_generation):
			self.tsp_population.population = GA().evolution(self.tsp_population, self.parameters)
			self.run_summary.append(self.tsp_population.population_summary(i+1))

			if self.tsp_population.fittest_individual().fitness < self.best_solution.fitness:
				self.fittest_solution = copy.deepcopy(self.tsp_population.fittest_individual())

		self.time_consumed = time.time() - start_time

		# Display the summary of the current run: the time consumed and the information of the best route
		self.summary()


	def summary(self, save=True):
		if save == True:
			df = pd.DataFrame.from_records(self.run_summary)
			df.to_csv('run_summary.tsv', sep='\t', header=False)

		print "Time consumed for the run         :  %.2f seconds." % self.time_consumed
		print "The route length of the solution  :  %.6f " % self.fittest_solution.fitness
		print "Final solution route of the run   :  "
		# Display the final solution
		self.fittest_solution.print_route()


	def show_behaviour(self, from_file=False):

		if from_file == True:
			x = range(100)
			yb, yw, ya = [], [], []
			with open("run_summary.tsv", 'r') as infile:
				for line in infile:
					line_data = line.strip().split('\t')
					yb.append(float(line_data[1]))
					yw.append(float(line_data[2]))
					ya.append(float(line_data[3]))

		if from_file == False:
			x = range(100)
			yb = [i[0] for i in self.run_summary]
			yw = [i[1] for i in self.run_summary]
			ya = [i[2] for i in self.run_summary]

		mpl.style.use('default')

		#plt.grid()
		plt.plot(x, yb, c='C1', linestyle="-", label="Best Fitness")
		plt.plot(x, yw, c='C2', linestyle=":", label="Worst Fitness")
		plt.plot(x, ya, c='C3', linestyle="-.", label="Mean Fitness")

		plt.title("Behaviours of Evolutionary Algorithm for TSP")
		plt.xticks([0, 100])
		plt.xlabel("Time")
		plt.ylabel("EA Behaviours")
		plt.savefig(self.image_name, format='eps', dpi=5000)
		plt.show()
