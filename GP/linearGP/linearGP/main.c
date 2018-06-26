//  main.c
//  linearGP
//
//  Created by yz on 2018-03-15.
//  Copyright © 2018 yz. All rights reserved.

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <assert.h>
#include <float.h>
#include <math.h>
#include <time.h>

#define POPULATION_SIZE    10000
#define LAMBDA             100
#define MAX_ITERATION      100000
#define MAX_PROG_SIZE      200

#define TOURNAMENT         5

#define CROSSOVER          0.2
#define MUTATE             0.9

#define FUNCTION_SIZE      6        // +, -, *, /, sin, cos
#define TERMINAL_SIZE      5        // 1, 2, 3, 4, 5,

#define INPUT_NUM          20       // 20 inputs
#define TOTAL_REGS         51       // r0-r30: calculation, r31-r50: input
#define CONST_REGS         5        // 51-55: constants(read-only)

#define MAX_START_DIFF     5
#define MAX_SEG_LENGTH     10

#define FITNESS_CASES      1         // Each evaluation, randomly initializa a case

typedef struct prog
{
    unsigned int instructions[MAX_PROG_SIZE];
    int absolute_length;
    float fitness;
}PROG;

PROG population[POPULATION_SIZE];
PROG mating[LAMBDA];

// Terminal Set
float registers[TOTAL_REGS+CONST_REGS] = {0.0};
// Function Set
char functions[FUNCTION_SIZE] = {'+', '-', '*', '/', 'c', 's'};

float best_fitness = FLT_MAX;
float worst_fitness = -FLT_MAX;

int shortest_lenghth = MAX_PROG_SIZE;
int best_prog = 0;

unsigned int effective_code[MAX_PROG_SIZE];

// Log of winners and losers
int winner;

int generation;

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

// Random Function Generator: return index of element
unsigned int random_function()
{
    return (unsigned int) (rand_prob() * FUNCTION_SIZE);
}

unsigned int random_destination_reg()
// Only used to generate lefthand side register
{
    return (unsigned int) (rand_prob() * (TOTAL_REGS-INPUT_NUM));
}

unsigned int random_registers()
/* return the index of a register */
{
    return (unsigned int) (rand_prob() * TOTAL_REGS);
}

unsigned int random_constants()
/* return (index of constants) + MAX_REGS */
{
    return (unsigned int) ((rand_prob() * CONST_REGS) + TOTAL_REGS);
}

unsigned int random_instruction(unsigned int * p_instruction)
/* randomly generate an instruction */
{
    // <op, Lregs, R1, R2>  -->  Lregs = R1 op R2
    unsigned int op = 0;
    unsigned int Lr = 0;
    unsigned int ra = 0;
    unsigned int rb = 0;
    
    *p_instruction = 0;
    
    // Generate an operator
    op = random_function();
    *p_instruction |= (op << 24);
    
    // lefthand side register
    Lr = random_destination_reg();
    *p_instruction |= (Lr << 16);
    
    // Pick one that must be a register with uniform distribution
    if (rand_prob() < 0.5)   // ra is the one that must be a register
    {
        ra = random_registers();
        if (rand_prob() < 0.5)
            rb = random_registers();
        else
            rb = random_constants();
        
    }
    else
    {
        rb = random_registers();
        if (rand_prob() < 0.5)
            ra = random_registers();
        else
            ra = random_constants();
    }
    
    *p_instruction |= (ra << 8);
    *p_instruction |=  rb;
    
    return *p_instruction;
}

int rand_prog(int child)
/* Produce a random program and save it to population[child] */
{
    int init_len = 0;

    init_len = (int) (rand_prob() * MAX_PROG_SIZE) + 1;
    
    for (int i = 0; i < init_len; i++)
        random_instruction(&population[child].instructions[i]);
    
    population[child].absolute_length = init_len;
    
    return init_len;
}

// Parent Selection: two parallel tournament selection with replacement
void select_parent()
{
    float max_tournament_fit = 0.0;
    float min_tournament_fit = 0.0;
    int   index = 0;
    
    for (int i = 0; i < LAMBDA; i++)
    {
        max_tournament_fit = -FLT_MAX;
        min_tournament_fit = FLT_MAX;
        for (int j = 0; j < TOURNAMENT; j++)
        {
            index = (int)(rand_prob() * POPULATION_SIZE);
            if (min_tournament_fit > population[index].fitness)
            {
                min_tournament_fit = population[index].fitness;
                winner = index;
            }
        }
        memcpy((void*) mating[i], (void*) population[winner], sizeof(struct prog));
    }
}

// Pure mutate on absolute instructions
void mutate(int child)
{
    int instruction = 0;
    int part        = 0;
    unsigned int alteration = 0;
    int ra, rb;
    
    instruction = (int) (rand_prob() * mating[child].absolute_length);
    part        = (int) (rand_prob() * 4);
    ra          = (mating[child].instructions[instruction] >> 8) & 0xFF;
    rb          = (mating[child].instructions[instruction]     ) & 0xFF;
    
    switch (part) {
        case 1:     // alter operantor
        {
            alteration = random_function();
            mating[child].instructions[instruction] &= 0x00FFFFFF;
            mating[child].instructions[instruction] |= (alteration << 24);
            break;
        }
        case 2:     // alter lefthand
        {
            alteration = random_destination_reg();
            mating[child].instructions[instruction] &= 0xFF00FFFF;
            mating[child].instructions[instruction] |= (alteration << 16);
            break;
        }
        case 3:     // generate a new register
        {
            alteration = random_registers();
            if (rand_prob() > 0.5) // mutate ra
            {
                mating[child].instructions[instruction] &= 0xFFFF00FF;
                mating[child].instructions[instruction] |= (alteration << 8);
            }
            else                  // mutate rb
            {
                mating[child].instructions[instruction] &= 0xFFFFFF00;
                mating[child].instructions[instruction] |= (alteration);
            }
            break;
        }
        case 4:     // generate a new constant
        {
            alteration = random_constants();
            if (ra >= TOTAL_REGS)       // mutate ra
            {
                mating[child].instructions[instruction] &= 0xFFFF00FF;
                mating[child].instructions[instruction] |= (alteration << 8);
            }
            else if (rb >= TOTAL_REGS)  // mutate rb
            {
                mating[child].instructions[instruction] &= 0xFFFFFF00;
                mating[child].instructions[instruction] |= (alteration);
            }
            else
            {
                if (rand_prob() > 0.5) // mutate ra
                {
                    mating[child].instructions[instruction] &= 0xFFFF00FF;
                    mating[child].instructions[instruction] |= (alteration << 8);
                }
                else                   // mutate rb
                {
                    mating[child].instructions[instruction] &= 0xFFFFFF00;
                    mating[child].instructions[instruction] |= (alteration);
                }
            }
            break;
        }
    }
}

int eliminate_struct_introns(int child)
// delete structural introns before evaluation
{
    int lh = 0, ra = 0, rb = 0;
    int effective_regs[TOTAL_REGS-INPUT_NUM] = {0};
    int next = 0;
    
    memset(effective_code, 0, MAX_PROG_SIZE*sizeof(unsigned int));
    
    memset(effective_regs, 0, (TOTAL_REGS-INPUT_NUM)*sizeof(int));
    effective_regs[0] = 1;
    
    next = 0;
    for (int i = population[child].absolute_length - 1; i >= 0; i--)
    {
        lh = (population[child].instructions[i] >> 16) & 0xFF;
        ra = (population[child].instructions[i] >> 8 ) & 0xFF;
        rb =  population[child].instructions[i]        & 0xFF;
        
        if (effective_regs[lh] == 1)
        {
            effective_code[next] = population[child].instructions[i];
            
            if (ra < (TOTAL_REGS - INPUT_DIMENSION))
                effective_regs[ra] = 1;
            if (rb < (TOTAL_REGS - INPUT_DIMENSION))
                effective_regs[rb] = 1;
            
            next++;
        }
    }
    
    return next;
}

float prog_value(int child)
/* excuate the program and get the value of register r0*/
{
    int op = 0, lh = 0, ra = 0, rb = 0;
    
    //population[child].effective_length = eliminate_struct_introns(child);
    
    for (int i = 0; i < population[child].absolute_length; i++)
    {
        op = (population[child].instructions[i] >> 24) & 0xFF;
        lh = (population[child].instructions[i] >> 16) & 0xFF;
        ra = (population[child].instructions[i] >> 8 ) & 0xFF;
        rb = (population[child].instructions[i]      ) & 0xFF;

        switch (functions[op])
        {
            case '+':   registers[lh] = registers[ra] + registers[rb];
                break;
            case '-':   registers[lh] = registers[ra] - registers[rb];
                break;
            case '*':   registers[lh] = registers[ra] * registers[rb];
                break;
            case '/':
            {
                if (registers[rb] == 0.0)
                    registers[lh] = 1.0;
                else
                    registers[lh] = registers[ra] / registers[rb];
                break;
            }
            case 'c':
            {
                if (ra < TOTAL_REGS)
                    registers[lh] = cos(registers[ra]);
                else registers[lh] = cos(registers[rb]);
                break;
            }
            case 's':
            {
                if (ra < TOTAL_REGS)
                    registers[lh] = sin(registers[ra]);
                else registers[lh] = sin(registers[rb]);
                break;
            }
            default:
                printf("Oooop!!\n");
                break;
        }
    }
    return registers[0];
}

void update_quality(float fitness, int child)
{
    population[child].fitness = fitness;
    
    if (best_fitness > population[child].fitness)
    {
        best_fitness = population[child].fitness;
        best_prog = child;
        shortest_lenghth = population[child].absolute_length;
    }
    if ((best_fitness == population[child].fitness) && shortest_lenghth > population[child].absolute_length)
    {
        best_prog = child;
        shortest_lenghth = population[child].absolute_length;
    }
}

void evaluate_fitness(int child)
// Connect to the tetris
{
    float error = 0.0;
    float sum_error = 0.0;
    
    sum_error = 0.0;
    
    for (int i = 0; i < FITNESS_CASES; i++)
    {
        // Load Input Registers: read only
        registers[TOTAL_REGS-INPUT_DIMENSION]   = x[i];
        registers[TOTAL_REGS-INPUT_DIMENSION+1] = y[i];
        
        // Initialize calculation register
        for (int j = 0; j < TOTAL_REGS - INPUT_DIMENSION; j++)
            registers[j] = 1.0;
        
        error = fabs(f[i] - prog_value(child));
        sum_error += (error * error);
    }
    
    update_quality(sqrt(sum_error/FITNESS_CASES), 0, child);
}

void initialize_population()
{
    for (int i = 0; i < POPULATION_SIZE; i++)
    {
        rand_prog(i);
        evaluate_fitness(i);
    }
}

void show_behavior()
{
    float mean_fit = 0.0;
    float mean_len = 0.0;
    
    worst_fitness = -FLT_MAX;
    mean_fit = 0.0;
    mean_len = 0.0;
    
    for (int i = 0; i < POPULATION_SIZE; i++)
    {
        mean_fit += (population[i].fitness / (float) POPULATION_SIZE);
        mean_len += ((float) population[i].absolute_length / (float) POPULATION_SIZE);
        
        if (worst_fitness < population[i].fitness)
            worst_fitness = population[i].fitness;
    }
    
    printf("Updates#%6d - Best: %8.3f\t\tMean: %10.3f\t\tWorst: %16.3f\t\tAvgLen: %6.1f\n", generation, best_fitness, mean_fit, worst_fitness, mean_len);
}

void display_effective_prog(int child)
{
    unsigned int op = 0, Lr = 0, ra = 0, rb = 0;
    
    for (int i = 0; i < population[child].absolute_length; i++)
    {
        op  = (population[child].instructions[i] >> 24) & 0xFF;
        Lr  = (population[child].instructions[i] >> 16) & 0xFF;
        ra  = (population[child].instructions[i] >> 8 ) & 0xFF;
        rb  = (population[child].instructions[i]      ) & 0xFF;
        
        printf("I%3d: r%d = r%d %c r%d\n", i+1, Lr, ra, functions[op], rb);
    }
    printf("\n");
}

void load_constants()
{
    for (int i = 0; i < CONST_REGS; i++)
        registers[TOTAL_REGS+i] = (float) (i+1);
}

void initialize_registers()
{
    int i = 0;
    time_t now;
    
    time(&now);
    seed = (int) now;
    
    while (i < TOTAL_REGS)
        registers[i++] = rand_prob() * 10;
}

int main(int argc, const char * argv[])
{
    generation = 0;
    
    // Load values into all registers
    load_constants();
    initialize_registers();
    
    // Initialize the population
    initialize_population();
    
    while ((++generation) <= MAX_ITERATION)
    {
        select_parent();
        
        if (rand_prob() < CROSSOVER)
        {
            crossover(winner1, winner2, loser1);
            crossover(winner2, winner1, loser2);
            evaluate_fitness(loser1);
            evaluate_fitness(loser2);
        }
        
        if (rand_prob() < MUTATE)
        {
            mutate(loser1);
            mutate(loser2);
            evaluate_fitness(loser1);
            evaluate_fitness(loser2);
        }

        if (generation % 10 == 0)
            show_behavior();
    }
    
    display_effective_prog(best_prog);
    
    return 0;
}
