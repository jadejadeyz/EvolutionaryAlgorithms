import random

def pmx(p1, p2, pr):
	child1 = [None for i in range(len(p1))]
	child2 = [None for i in range(len(p2))]

	pos1 = 3
	pos2 = 6
	randpr = random.random()

	if randpr > pr:
		# do not crossover
		child1 = p1
		child2 = p2
	else:
		# do the crossover
		l = min(pos1, pos2)
		r = max(pos1, pos2)
		child1[l:r+1] = p1[l:r+1]
		child2[l:r+1] = p2[l:r+1]

		# Composition of child 1
		uncopied_1 = set(p2[l:r+1]) - set(p1[l:r+1])
		uncopied_2 = set(p1[l:r+1]) - set(p2[l:r+1])
		for i in range(l, r+1):
			if p2[i] in uncopied_1:
				compare(child1, p2, p2[i], i)

		for j in range(0, len(p1)):
			if child1[j] == None:
				child1[j] = p2[j]

		# Composition of child 2
		
		for i in range(l, r+1):
			if p1[i] in uncopied_2:
				compare(child2, p1, p1[i], i)

		for j in range(0, len(p1)):
			if child2[j] == None:
				child2[j] = p1[j]

	return (child1, child2)

def compare(A, B, s, s_index):
	while(A[s_index]!= None):
		s_index = B.index(A[s_index])
	A[s_index] = s


def main():
	p1 = [1, 2, 3, 4, 5, 6, 7, 8, 9]
	p2 = [9, 3, 7, 8, 2, 6, 5, 1, 4]
	random.shuffle(p1)
	random.shuffle(p2)
	print p1
	print p2
	print "*"* 20
	c1, c2 = pmx(p1, p2, 0.9)

	print c1
	print c2


if __name__ == '__main__':
	main()

