//  treeGP
//  This program aims to implement a tree-base GP to solve Symbolic Regression.
//
//  Created by yz on 2018-03-11.
//  Copyright © 2018 yz. All rights reserved.
//
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <assert.h>
#include <time.h>
#include <float.h>
#include <math.h>

// Max depth of a GP tree is 5 with maximum 63 nodes.
#define MAX_TREE_DEPTH  5
#define MAX_INIT_SIZE   63
#define STACK_SIZE      1024
#define MAX_PROG_SIZE   1024

#define MAX_FIT_CASES   21

#define LINE_WIDTH      200
#define MAX_FILENAME    100

// Parameter Setting
#define CROSSOVER        1
#define MUTATION         0.2
#define FUNCTION_SIZE    4
#define TERMINAL_SIZE    7
#define MAX_POPULATION   100000

// Input Command: load from the command line
int population_size;
int tournament_size;
int generation_size;

// Index of the current updates (No "generation" concept in steady-state),
// also indicate how many crossover operations have been done.
int generation;

// Fitness cases
float x[MAX_FIT_CASES];
float y[MAX_FIT_CASES];
float f[MAX_FIT_CASES];

// Results of the tournament
int winner1, winner2;              // Indices of the winners in the tournament
int loser1, loser2;                // Indices of the losers in the tournament

// Information about fitness and best program in one generation
int best_geno;                     // Best program in a run (its tree size is shortest)
int min_prog_size = MAX_PROG_SIZE; // The program size of the best individual
float max_fitness = -FLT_MAX;      // This is the worst fitness
float min_fitness = FLT_MAX;       // This is the best fitness

// Interpret GP program
int   stack;
float operand_stack[STACK_SIZE];
char  operator_stack[STACK_SIZE];
char  output_stack[STACK_SIZE][LINE_WIDTH];

// GP program structure
typedef struct genotype
{
    char tree[MAX_PROG_SIZE + 1];       // Store prefix-order tree
    float fitness;                      // Root-mean-squared error
}GENO;
GENO population[MAX_POPULATION];
GENO new_population[MAX_POPULATION];

// Random number generator
int seed;
int rand_int()
{
    double const base = 16807.0;
    double const op   = 2147483647.0;
    
    double temp = seed * base;
    seed = (int) (temp - op * floor(temp / op));
    return seed;
}

float rand_prob()
{
    float r;
    while ((r = ((float)(rand_int()) / 2147483647.0)) >= 1.0)
        continue;
    return r;
}

// Randomly pick a node from a tree
int random_node(char tree[])
{
    unsigned long tree_size;
    tree_size = strlen(tree);
    return ((int) (rand_prob() * (float) tree_size));
}

// Find the next subtree starting point after the picked node
int next_subtree_root(char tree[], int subtree)
{
    int branch, i;
    if (tree[subtree] == '\0')
        branch = 0;
    else
        branch = 1;
    
    for (i = subtree; branch > 0; i++)
    {
        switch (tree[i])
        {
            case '+':
            case '-':
            case '*':
            case '/':   branch++;
                break;
            case '\0':  assert(0 == 1);
                break;
                
            default:    branch--;
                break;
        }
    }
    return i;
}

// Randomly select function symbols from Function Set
char rand_function()
{
    char function[FUNCTION_SIZE] = {'+', '-', '*', '/'};
    
    return (function[(int)(rand_prob() * FUNCTION_SIZE)]);
}

// Randomly select terminal symbols from Terminal Set
char rand_terminal()
{
    char terminal[TERMINAL_SIZE] = {'x', 'y', '1', '2', '3', '4', '5'};
    return (terminal[(int)(rand_prob() * TERMINAL_SIZE)]);
}

// Random Tree Generator: used for mutation
int rand_tree(char buff[], int buff_size)
{
    int branch = 1, i;
    
    for (i = 0; (branch > 0) && (i <= buff_size); i++)
    {
        if (rand_prob() > (float)(branch*branch + 1)/(float)(buff_size - i))
        {
            buff[i] = rand_function();
            branch++;
        }
        else
        {
            buff[i] = rand_terminal();
            branch--;
        }
    }
    
    assert(0 == branch);
    
    return i;
}

char fd1[3] = {'f','t','t'};
char fd2[7] = {'f','f','t','t','f','t','t'};
char fd3[15] = {'f','f','f','t','t','f','t','t','f','f','t','t','f','t','t'};
char fd4[31] = {'f','f','f','f','t','t','f','t','t','f','f','t','t','f','t',
              't','f','f','f','t','t','f','t','t','f','f','t','t','f','t','t'};
char fd5[63] = {'f','f','f','f','f','t','t','f','t','t','f','f','t','t','f','t',
                't','f','f','f','t','t','f','t','t','f','f','t','t','f','t','t',
                'f','f','f','f','t','t','f','t','t','f','f','t','t','f','t','t',
                'f','f','f','t','t','f','t','t','f','f','t','t','f','t','t'};
// FULL method to generate a GP program
int full(char buff[])
{
    int i;
    int r_depth = 1;
    double node_num = 0;

    // random depth between [1, 5]
    r_depth = (int)(rand_prob() * 4) + 1;
    
    for (i = 0; i <= r_depth; i++)
        node_num += pow(2, i);
    switch (r_depth) {
        case 1:
        {
            for (i = 0; i < node_num; i++)
            {
                if (fd1[i] == 'f')
                    buff[i] = rand_function();
                else buff[i] = rand_terminal();
            }
            break;
        }
        case 2:
        {
            for (i = 0; i < node_num; i++)
            {
                if (fd2[i] == 'f')
                    buff[i] = rand_function();
                else buff[i] = rand_terminal();
            }
            break;
        }
        case 3:
        {
            for (i = 0; i < node_num; i++)
            {
                if (fd3[i] == 'f')
                    buff[i] = rand_function();
                else buff[i] = rand_terminal();
            }
            break;
        }
        case 4:
        {
            for (i = 0; i < node_num; i++)
            {
                if (fd4[i] == 'f')
                    buff[i] = rand_function();
                else buff[i] = rand_terminal();
            }
            break;
        }
        case 5:
        {
            for (i = 0; i < node_num; i++)
            {
                if (fd5[i] == 'f')
                    buff[i] = rand_function();
                else buff[i] = rand_terminal();
            }
            break;
        }
    }
    buff[i] = '\0';
    return i-1;
}

// GROW method to generate a GP program
int grow(char buff[], int buff_size)
{
    int i;
    int branch = 1;
    
    for (i = 0; (branch > 0) && (i <= buff_size); i++)
    {
        if (rand_prob() > (float)(branch*branch + 1)/(float)(buff_size - i))
        {
            buff[i] = rand_function();
            branch++;
        }
        else
        {
            buff[i] = rand_terminal();
            branch--;
        }
    }
    
    buff[i] = '\0';
    assert(0 == branch);
    
    return i;
}

int cutoff(char output[], int osize,
           char buff1[],  int tail1,
           char buff2[],  int tail2,
           char buff3[],  int tail3)
{
    if (osize <= (tail1 + tail2 + tail3))
        return 1;
    
    memcpy(output,              buff1, tail1);
    memcpy(&output[tail1],      buff2, tail2);
    memcpy(&output[tail1+tail2],buff3, tail3);
    
    output[(tail1 + tail2 + tail3)] = '\0';
    return 0;
    
}

// CROSSOVER OPERATION
void crossover(int mom, int dad, int child)
{
    int mom_tail1;
    int mom_head2, mom_tail2;
    int dad_head, dad_tail;
    int sanity = 0;
    
    do {
        assert (sanity++ < 1000);
        mom_tail1 = random_node(population[mom].tree);
        mom_head2 = next_subtree_root(population[mom].tree, mom_tail1);
        mom_tail2 = (int) strlen(population[mom].tree);
        
        dad_head = random_node(population[dad].tree);
        dad_tail = next_subtree_root(population[dad].tree, dad_head);
    } while (cutoff(new_population[child].tree, MAX_PROG_SIZE+1,
                    population[mom].tree, mom_tail1,
                    &population[dad].tree[dad_head], dad_tail - dad_head,
                    &population[mom].tree[mom_head2], mom_tail2 - mom_head2 ) != 0 );
}

// MUTATION OPERATION
void mutate(int parent, int child)
{
    int parent_tail1;
    int parent_head2, parent_tail2;
    int mutate_size;
    int sanity = 0;
    char buff[MAX_INIT_SIZE];
    
    do {
        assert(sanity++ < 1000);
        parent_tail1 = random_node(population[parent].tree);
        parent_head2 = next_subtree_root(population[parent].tree, parent_tail1);
        parent_tail2 = (int) strlen(population[parent].tree);
        mutate_size = rand_tree(buff, (int) (rand_prob()*MAX_INIT_SIZE));
    }while (cutoff(new_population[child].tree, MAX_PROG_SIZE+1,
                   population[parent].tree, parent_tail1,
                   buff, mutate_size,
                   &population[parent].tree[parent_head2], parent_tail2 - parent_head2) != 0);
}

// STACK OPERATION
void push_operator(char operator)
{
    operator_stack[stack++] = operator;
    operand_stack[stack] = 0.0;     // keep clean
    output_stack[stack][0] = '\0';  // keep clean
}

void push_operand(float operand)
{
    float result, temp;
    
    if ((stack <= 0) || (operator_stack[stack - 1] != 0))
    {
        operator_stack[stack] = 0;
        operand_stack[stack++] = operand;
    }
    else
    {
        temp = operand_stack[--stack];
        switch (operator_stack[--stack]) {
            case '+':
            {
                result = temp + operand;
                break;
            }
            case '-':
            {
                result = temp - operand;
                break;
            }
            case '*':
            {
                result = temp * operand;
                break;
            }
            case '/':  // protected division
            {
                if (operand == 0.0)
                    result = 10.0;
                else
                    result = temp / operand;
                break;
            }
            default:
                assert(0 == 21);
                break;
        }
        push_operand(result);
    }
}

// Calculate the output value of a GP program
float geno_value(char tree[], float x, float y)
{
    stack = 0;
    
    for (int i = 0; tree[i] != 0; i++)
    {
        switch (tree[i])
        {
            case '+':
            case '-':
            case '*':
            case '/': push_operator(tree[i]);
                break;
                
            case 'x': push_operand(x);
                break;
            case 'y': push_operand(y);
                break;
                
            case '1': push_operand(1.0);
                break;
            case '2': push_operand(2.0);
                break;
            case '3': push_operand(3.0);
                break;
            case '4': push_operand(4.0);
                break;
            case '5': push_operand(5.0);
                break;
        }
    }
    
    assert (1 == stack);
    
    return (operand_stack[--stack]);
}

void update_quality(float fit, int child)
{
    unsigned long prog_length = 0;
    
    new_population[child].fitness = fit;
    
    prog_length = strlen(new_population[child].tree);
    
    // Update the best individual in terms of fitness value
    if (min_fitness > new_population[child].fitness)
    {
        min_fitness = new_population[child].fitness;
        min_prog_size = (int) prog_length;
        best_geno = child;
    }
    if ((min_fitness == new_population[child].fitness) && (min_prog_size > (int) prog_length))
    {
        min_prog_size = (int) prog_length;
        best_geno = child;
    }
}

// Test a GP program on all given fitness cases
void evaluate_fitness(int child)
{
    double error = 0.0;                // N1 error
    double squared_error = 0.0;        // squared error
    double total_squared_error = 0.0;  // Sum of squared errors
    
    for (int i = 0; i < MAX_FIT_CASES; i++)
    {
        error = fabs(f[i] - geno_value(new_population[child].tree, x[i], y[i]));
        squared_error = error*error;
        total_squared_error += squared_error;

    }
    update_quality(sqrt(total_squared_error/MAX_FIT_CASES), child);
}

// When no variation happens, just copy the individual in the new population
void copy (int parent, int child)
{
    strcpy(new_population[child].tree, population[parent].tree);
    update_quality(population[parent].fitness, child);
}

// After each variation operation, update the population pool with two children
void update_population()
{
    for (int i = 0; i < population_size; i++)
    {
        strcpy(population[i].tree, new_population[i].tree);
        population[i].fitness = new_population[i].fitness;
    }
}

// Population Initialization: ramped half-and-half
void init_population()
{
    for (int i = 0; i < population_size; i++)
    {
        if (i % 2 == 0)
        {
            full(new_population[i].tree);
            evaluate_fitness(i);
        }
        else
        {
            grow(new_population[i].tree, MAX_INIT_SIZE);
            evaluate_fitness(i);
        }
    }
    update_population();
}

// Parent Selection: tournament selection with replacement
void parent_selection()
{
    int parent;
    float tour_max = -FLT_MAX;
    float tour_min = FLT_MAX;
    float competitors_fit[MAX_POPULATION] = {0.0};
    int i_competitors[MAX_POPULATION] = {0};

    for (int i = 0; i < tournament_size; i++)
    {
        parent = (int) (rand_prob() * population_size);
        competitors_fit[i] = population[parent].fitness;
        i_competitors[i] = parent;
    }
    
    // Tournament selection to find the top 2 and last 2
    for (int i = 0; i < tournament_size; i++)
    {
        if (tour_max < competitors_fit[i])
        {
            loser1 = i_competitors[i];
            tour_max = competitors_fit[i];
        }
        if (tour_min > competitors_fit[i])
        {
            winner1 = i_competitors[i];
            tour_min = competitors_fit[i];
        }
    }
    
    tour_max = -FLT_MAX;
    tour_min = FLT_MAX;
    
    for (int i = 0; i < tournament_size;i++)
    {
        if (loser1 == i || winner1 == i)
            continue;
        if (tour_max < competitors_fit[i])
        {
            loser2 = i_competitors[i];
            tour_max = competitors_fit[i];
        }
        if (tour_min > competitors_fit[i])
        {
            winner2 = i_competitors[i];
            tour_min = competitors_fit[i];
        }
    }
}

void show_behavior()
{
    float sum_fit = 0.0;
    float mean_fit = 0.0;
    
    max_fitness = -FLT_MAX;
    
    for (int i = 0; i < population_size; i++)
    {
        sum_fit += new_population[i].fitness;
        
        if (max_fitness < new_population[i].fitness)
            max_fitness = new_population[i].fitness;
    }

    mean_fit = sum_fit / (float) population_size;
    
    printf("After %6d updates - Best: %8.3f\t\t\tMean: %8.3f\t\t\tWorst: %16.3f\n", generation, min_fitness, mean_fit, max_fitness);
}

// Load fitness cases
int load_cases(char filename[])
{
    int i = 0;
    time_t current;
    
    time(&current);
    seed = (int) current;
    
    FILE *fp;
    fp = fopen(filename, "r");
    if (fp == NULL)
    {
        printf("Could not open file %s", filename);
        return 1;
    }
    
    // read fitness cases into x[], y[], f[]
    while (!feof(fp))
    {
        fscanf(fp, "%f\t%f\t%f\n", &x[i], &y[i], &f[i]);
        i++;
    }
    
    fclose(fp);
    return 0;
}

int main(int argc, const char * argv[]) {
    
    int mom, dad;
    float r;
    
    // Read fitness cases from the input file.
    load_cases("A4_trainingSamples.txt");
    
    // Load patial parameters from standard input.
    printf("\nPlease enter population size: ");
    scanf("%d", &population_size);
    printf("Please enter tournament size: ");
    scanf("%d", &tournament_size);
    printf("Please enter  max generation: ");
    scanf("%d", &generation_size);
    
    generation = 0;
    
    // Randomly initialize the population
    init_population();
    
    // Keep updating the population until reaching the max generation size
    while ((++generation) <= generation_size)
    {
        parent_selection();
        mom = winner1;
        dad = winner2;
        
        for (int i = 0; i < population_size; i++)
        {
            if (i == loser1)
            {
                // Crossover with probability 1
                crossover(mom, dad, i);
                
                // Mutate with probability 0.2
                if ((r = rand_prob()) < MUTATION)
                    mutate(i, i);
                
                // Evaluate the children
                evaluate_fitness(i);
            }
            else if (i == loser2)
            {
                // Crossover with probability 1
                crossover(dad, mom, i);
                
                // Mutate with probability 0.2
                if ((r = rand_prob()) < MUTATION)
                    mutate(i, i);
                
                // Evaluate the children
                evaluate_fitness(i);
            }
            else copy(i, i);
        }
        
        // Display to the standard output every 100 generations
        if (generation % 100 == 0)
            show_behavior();
        
        update_population();
    }
    // After finishing all updates, display the final best individual to the standard output
    printf("Final fittest GP Program: %s\n\n", new_population[best_geno].tree);
    
    // Well, GP Done! :)
    return 0;
}
