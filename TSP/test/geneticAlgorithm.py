# This is an implementation of Genetic Algorithms for TSP problem.  #
# Author: Yu Zhang													#
# Course: Evolutionary Computation									#
#####################################################################
import matplotlib.pyplot as plt
import matplotlib.ticker as mtick
import matplotlib as mpl
from matplotlib.ticker import MultipleLocator, FormatStrFormatter
import numpy as np
import pandas as pd
import random
import time

cities_list = []  # Used to construct distance dictionary "dist_dict", see below.
cities_seed = []  # Used as permutation seeds when initializing the population.
dist_dict = {}    # key -> tuple(city_id1, city_id2), value -> distance between the two cities
new_keys = []     # Used to shuffle the new generation.

## Parameters for GA RUN ##
max_population_size = 0
genotype_length     = 0
mutation_scheme     = ""
tournament_size     = 0
max_generation      = 0
mutation_pr         = 0.0
xover_pr            = 0.0
stagnation          = 'False'
stag_counter        = 0
stag_delta          = 0

class City(object):
	"""
	self.id         : an int storing city ID.
	self.coordinate : a 2D numpy array storing the coordinate of a city.

	"""
	def __init__(self, city_id='', x=0.0, y=0.0):
		self.id = city_id
		self.coordinate = np.array((x, y))


class GA(object):
	"""
	Implementations of selection (parents & survivors), recombination (crossover), mutation (permutation).

	"""
	def __init__(self):
		# Initialize a population with size 
		global max_population_size

		self.pop_dict = {}
		random.seed(time.time())
		for i in range(0, max_population_size):
			random.shuffle(cities_seed)
			self.pop_dict[i] = [cities_seed[:], 0.0]
		# Evaluate the fitness of the initial population
		for item in self.pop_dict.items():
			self.evaluation(item[1])

		# Summary the initial population
		self.mean = sum(element[1] for element in self.pop_dict.values()) / max_population_size
		self.worst = max(element[1] for element in self.pop_dict.values())
		self.best_solution = min(self.pop_dict.values(), key=lambda x:x[1])
		# To the standard output
		print "-" * 125
		print "The initial context of the population: "
		print "Best Fitness : %.7f\t\tAverage Fitness : %.7f\t\tWorst Fitness : %.7f" % (self.best_solution[1], self.mean, self.worst)
		print "-" * 125


	# Fitness Evaluation: straightforward way, i.e. the tour length is its fitness.
	def evaluation(self, routeInfo):
		global genotype_length
		routeInfo[1] = sum(dist_dict[(min(routeInfo[0][i], routeInfo[0][(i+1)%genotype_length]), max(routeInfo[0][i], routeInfo[0][(i+1)%genotype_length]))] 
							for i in range(genotype_length))
		return routeInfo
	

	# Parent selection: tournament selection with replacement
	def parent_selection(self):
		global max_population_size
		global tournament_size

		# mate is used to store selected parents.
		self.mates = []
		for i in range(max_population_size):
			random.seed(time.time())
			self.mates.append(min([random.choice(self.pop_dict.values()) for k in range(tournament_size)], key=lambda x:x[1]))


	# Crossover: PMX scheme
	def PMX_crossover(self):
		global max_population_size
		global genotype_length
		global xover_pr

		random.seed(time.time())
		for i in range(0, max_population_size, 2):
			# Allocate two offspring of the parents
			child1 = [None for j in range(genotype_length)]
			child2 = [None for j in range(genotype_length)]

			# Generate a probability to determine whether crossover happens between two parents.
			self.rand_pr = random.uniform(0,1)

			if self.rand_pr >= xover_pr:
				# No crossover
				self.pop_dict[max_population_size+i]   = self.mates[i]
				self.pop_dict[max_population_size+i+1] = self.mates[i+1]
			else:
			# Select two crossover positions.
				xover_pos_1 = random.randrange(0, genotype_length - 1)
				xover_pos_2 = random.randrange(0, genotype_length - 1)
				if (abs(xover_pos_1 - xover_pos_2) != genotype_length - 1):
					left = min(xover_pos_1, xover_pos_2)
					right = max(xover_pos_1, xover_pos_2)

					atime = time.time()
					child1[left:right+1] = self.mates[i][0][left:right+1]
					child2[left:right+1] = self.mates[i+1][0][left:right+1]

					uncopy_1 = set(self.mates[i+1][0][left:right+1]) - set(self.mates[i][0][left:right+1])
					uncopy_2 = set(self.mates[i][0][left:right+1]) - set(self.mates[i+1][0][left:right+1])
					
					# Composition of child 1 and child 2
					for x in range(left, right+1):
						if self.mates[i+1][0][x] in uncopy_1:
							self.fill_in(child1, self.mates[i+1][0], self.mates[i+1][0][x], x)
					for j in range(genotype_length):
						if child1[j] == None:
							child1[j] = self.mates[i+1][0][j]

					for x in range(left, right+1):
						if self.mates[i][0][x] in uncopy_2:
							self.fill_in(child2, self.mates[i][0], self.mates[i][0][x], x)
					for j in range(genotype_length):
						if child2[j] == None:
							child2[j] = self.mates[i][0][j]

					self.pop_dict[max_population_size+i]   = [child1[:], 0.0]
					self.pop_dict[max_population_size+i+1] = [child2[:], 0.0]
				else:
					self.pop_dict[max_population_size+i]   = self.mates[i]
					self.pop_dict[max_population_size+i+1] = self.mates[i+1]


	def fill_in(self, child, parent, gene, i):
		while child[i] != None:
			i = parent.index(child[i])
		child[i] = gene
			

	# Mutation: default scheme is Inversion Mutation.
	def mutation(self):
		global max_population_size
		global mutation_scheme
		global mutation_pr

		random.seed(time.time())
		for i in range(max_population_size, 2*max_population_size):
			self.rand_pr = random.uniform(0, 1)

			if self.rand_pr >= mutation_pr:
				# No mutation
				start_time = time.time()
				self.evaluation(self.pop_dict[i])
			else:
				# pick two positions randomly.
				mut_pos_1 = random.randrange(0, genotype_length - 1)
				mut_pos_2 = random.randrange(0, genotype_length - 1)

				if mutation_scheme == "inversion" and (mut_pos_1 != mut_pos_2):
					left = min(mut_pos_1, mut_pos_2)
					right = max(mut_pos_1, mut_pos_2)

					# inverse the elements between left and right.
					self.pop_dict[i][0][left:right+1] = reversed(self.pop_dict[i][0][left:right+1])
					self.evaluation(self.pop_dict[i])
				else:
					self.evaluation(self.pop_dict[i])

				if mutation_scheme == "swap" and (mut_pos_1 != mut_pos_2):
					# Swap two genes
					self.pop_dict[i][0][mut_pos_1], self.pop_dict[i][0][mut_pos_2] = self.pop_dict[i][0][mut_pos_2], self.pop_dict[i][0][mut_pos_1]
					self.evaluation(self.pop_dict[i])
				else:
					self.evaluation(self.pop_dict[i])

				if mutation_scheme == "insert" and (mut_pos_1 != mut_pos_2):
					obj = self.pop_dict[i][0].pop(mut_pos_2)
					self.pop_dict[i][0].insert(mut_pos_1 + 1, obj)
					self.evaluation(self.pop_dict[i])
				else:
					self.evaluation(self.pop_dict[i])


	# Survivor selection: deterministic methods -> ranking all the parents and offspring
	def survivor_selection(self, generation_i):
		global new_keys
		global max_population_size
		global max_population_size

		random.shuffle(new_keys)
		next_population = sorted(self.pop_dict.values(), key=lambda x:x[1])[0:max_population_size]
		self.pop_dict = dict(zip(new_keys, next_population))

		# Summary the current population
		self.mean = sum(element[1] for element in self.pop_dict.values()) / max_population_size
		self.worst = next_population[max_population_size - 1][1]
		self.best_solution = next_population[0]
		# To the standard output
		print "#%3d Best Fitness : %.7f\t\tAverage Fitness : %.7f\t\tWorst Fitness : %.7f" % (generation_i, self.best_solution[1], self.mean, self.worst)


	# The evolution of a current generation
	def evolution(self, generation_i):
		# Step 1: create mating pool (stochastic), the size of mating pool = the size of population
		self.parent_selection()

		# Step 2: recombine two consecutive parents in the mating pool, and reproduce two offspring after crossover.
		self.PMX_crossover()

		# Step 3: mutation the offspring.
		self.mutation()

		# Step 4: survivor selection among parent and offspring (elitism as default).
		self.survivor_selection(generation_i)


class Session(object):
	"""
	Implementation of a GA run.

	Load parameter settings from a file.
	Load city list from a file

	"""
	def __init__(self, parameters_file, city_file):
		global mutation_pr
		global xover_pr
		global tournament_size
		global max_population_size
		global max_generation
		global mutation_scheme
		global stagnation
		global stag_delta
		global stag_counter

		self.city_file_name = city_file.strip().split('_')[3][:-4]
		# Loading the parameters of the current run.
		with open(parameters_file, 'r') as infile:
			for line in infile:
				line_data = line.strip().split('\t')
				if line_data[0] == "mutation_probability":
					mutation_pr = float(line_data[1])

				if line_data[0] == "crossover_probability":
					xover_pr = float(line_data[1])

				if line_data[0] == "max_generation":
					max_generation = int(line_data[1])

				if line_data[0] == "population_size":
					max_population_size = int(line_data[1])

				if line_data[0] == "tournament_size":
					tournament_size = int(line_data[1])

				if line_data[0] == "mutation_scheme":
					mutation_scheme = line_data[1]

				if line_data[0] == "stag_delta":
					stag_delta = float(line_data[1])

				if line_data[0] == "stagnation":
					stagnation = line_data[1]

				if line_data[0] == "stag_counter":
					stag_counter = int(line_data[1])

		# Initialize cities_list
		self.read_cities(city_file)
		# Construct dist_dict for fitness evaluation
		self.construct_dist_ref()

	# read cities from a file
	def read_cities(self, city_file):
		global max_population_size
		global new_keys
		global genotype_length
		with open(city_file, 'r') as infile:
			for line in infile:
				line_data = line.strip().split(' ')
				# Add new_city into the global variable "cities_list"
				cities_list.append(City(int(line_data[0]), float(line_data[1]), float(line_data[2])))
				cities_seed.append(int(line_data[0]))
		new_keys = [i for i in range(max_population_size)]
		genotype_length = len(cities_seed)


	# Create the global distance reference for all chromosomes
	def construct_dist_ref(self):
		global genotype_length

		for i in range(genotype_length - 1):
			for j in range(i, genotype_length):
				dist_dict[(cities_list[i].id, cities_list[j].id)] = np.linalg.norm(cities_list[i].coordinate - cities_list[j].coordinate)


	def run(self):
		global max_generation
		global stagnation
		global stag_delta
		global stag_counter

		self.run_summary = []

		# Initialize the EA for TSP
		tsp_search = GA()
		# Get the information of the initial generation.
		self.best_solution = tsp_search.best_solution

		start_time = time.time()
		# Termination Condition: Stag
		if stagnation == 'True':
			tsp_search.evolution(1)
			if tsp_search.best_solution[1] < self.best_solution[1]:
				self.best_solution = tsp_search.best_solution
				self.run_summary.append([tsp_search.best_solution[1], tsp_search.mean, tsp_search.worst])
			i = 1
			counter = 0
			ref_fitness = tsp_search.best_solution[1]
			while counter < stag_counter:
				tsp_search.evolution(i+1)
				self.run_summary.append([tsp_search.best_solution[1], tsp_search.mean, tsp_search.worst])
				if tsp_search.best_solution[1] < self.best_solution[1]:
					self.best_solution = tsp_search.best_solution
				i += 1
				counter += 1
				if counter == stag_counter:
					if abs(self.best_solution[1] - ref_fitness) < self.best_solution[1]*stag_delta:
						break
					else:
						ref_fitness = tsp_search.best_solution[1]
						counter = 0

		# the evolution of the population for max_generation generations
		if stagnation == 'False':
			for i in range(max_generation):
				tsp_search.evolution(i+1)
				self.run_summary.append([tsp_search.best_solution[1], tsp_search.mean, tsp_search.worst])
				if tsp_search.best_solution[1] < self.best_solution[1]:
					self.best_solution = tsp_search.best_solution

		self.time_consumed = time.time() - start_time

		timestamp = time.strftime('%m-%d_%H-%M-%S', time.localtime())
		self.image_name     = timestamp + '_EA_behaviours_' + self.city_file_name + '.eps'
		self.fit_filename   = timestamp + '_EA_fitnesses_' + self.city_file_name + '.tsv'
		self.route_file     = timestamp + '_Route_Info_' + self.city_file_name + '.txt'
		# Display the summary of the current run: the time consumed and the information of the best route
		self.summary(save=True)


	def show_route(self, save=True):
		global genotype_length
		global max_population_size
		global mutation_scheme
		global tournament_size
		global max_generation
		global mutation_pr
		global xover_pr
		global stagnation
		global stag_counter
		global stag_delta

		for i in range(0, genotype_length, 20):
			for j in range(i, min(i + 20, genotype_length)):
				print "%3d, " % self.best_solution[0][j],
			if i == genotype_length / 20 * 20:
				print '\b\b\b.'
			else:
				print '\n'
		if save == True:
			with open(self.route_file, 'w') as outfile:
				for i in range(genotype_length - 1):
					outfile.write('{:3d},'.format(self.best_solution[0][i]))
					if i % 20 == 19:
						outfile.write("\n")
				outfile.write('{:3d}.'.format(self.best_solution[0][genotype_length-1]))
				outfile.write("\nBest Individual's Fitness (Total travel Length) : %s\n" % self.best_solution[1])
				outfile.write("Total time : %s seconds\n" % self.time_consumed)
				outfile.write("\n\nParameter Settings for this run:\n")
				outfile.write("max_population_size : %s \n" % max_population_size)
				outfile.write("mutation_scheme : %s \n" % mutation_scheme)
				outfile.write("tournament_size : %s \n" % tournament_size)
				outfile.write("max_generation : %s \n" % max_generation)
				outfile.write("mutation_pr : %s \n" % mutation_pr)
				outfile.write("xover_pr : %s \n" % xover_pr)
				outfile.write("stagnation : %s \n" % stagnation)
				outfile.write("stag_counter : %s \n" % stag_counter)
				outfile.write("stag_delta : %s \n" % stag_delta)

				
	def summary(self, save=True):
		if save == True:
			df = pd.DataFrame.from_records(self.run_summary)
			df.to_csv(self.fit_filename, sep='\t', header=False)

		print "Time consumed for the run         :  %.2f seconds." % self.time_consumed
		print "The route length of the solution  :  %.6f " % self.best_solution[1]
		print "Final solution route of the run   :  "
		# Display the final solution
		self.show_route()


	def show_behaviour(self, fromfile=False, filename=''):
		global max_generation

		if fromfile == True:
			x = []
			yb = []
			ya = []
			yw = []
			with open(filename, 'r') as infile:
				for line in infile:
					line_data = line.strip().split('\t')
					x.append(float(line_data[0]))
					yb.append(float(line_data[1]))
					ya.append(float(line_data[2]))
					yw.append(float(line_data[3]))
		else:
			yb = [i[0] for i in self.run_summary]
			ya = [i[1] for i in self.run_summary]
			yw = [i[2] for i in self.run_summary]
			if stagnation == 'True':
				x = range(len(yb))
			else:
				x = range(max_generation)

		mpl.style.use('default')

		fig = plt.figure()
		ax = fig.add_subplot(111)
		plt.ticklabel_format(axis='both', style='sci', scilimits=(-2,2))


		ax.xaxis.grid(True, which='major', linestyle='dotted')
		ax.yaxis.grid(True, which='major', linestyle='dotted')


		ax.xaxis.set_major_locator( MultipleLocator(len(x) / 5) )
		ax.xaxis.set_minor_locator( MultipleLocator(len(x) / 10) )
		ax.xaxis.set_major_formatter(FormatStrFormatter('%d'))

		plt.plot(x, yb, c='C1', linestyle="-", label="Best Fitness")
		plt.plot(x, ya, c='C2', linestyle="--", label="Mean Fitness")
		plt.plot(x, yw, c='C6', linestyle=":", label="Worst Fitness")
		

		plt.title("Behaviours of Evolutionary Algorithm for TSP")
		plt.legend(loc='upper right')

		plt.xlim(0, len(x))
		plt.ylim(yb[-1]*0.85, yw[0]*1.1)


		plt.xlabel("Generations")
		plt.ylabel("EA Behaviours")
		plt.savefig(self.image_name, format='eps', dpi=500, transparent=True)
		plt.show()

		
