import geneticAlgorithm as ga
import sys

def main(args):
	ga_session = ga.Session(args[1], args[2])
	for i in range(0, int(args[3])):
		ga_session.run()
		ga_session.show_behaviour(i, from_file=False)


if __name__ == "__main__":
	main(sys.argv)
