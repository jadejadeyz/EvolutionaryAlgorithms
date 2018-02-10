import geneticAlgorithm as ga
import sys

def main(args):
	ga_session = ga.Session(args[1], args[2])
	ga_session.run()
	ga_session.show_behaviour()


if __name__ == "__main__":
	main(sys.argv)
