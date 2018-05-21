#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#define MX_SIZE 2048

typedef struct tree
{
    char content;
    struct tree * left;
    struct tree * right;
    struct tree * father;
}TREE;

TREE node[MX_SIZE+2];
TREE root;

long maxDepth(TREE* node)
{
    if (node == NULL)
        return 0;
    else
    {
        long leftDepth = maxDepth(node->left);
        long rightDepth = maxDepth(node->right);
        
        if (leftDepth > rightDepth)
            return leftDepth + 1;
        else
            return rightDepth + 1;
    }
}

long tree_builder(char str[], int len)
/* Return the depth of the tree */
{
    TREE* pivot = NULL;
    long tree_len;
    
    root.content = str[0];
    root.left = NULL;
    root.right = NULL;
    root.father = NULL;
    
    pivot = &root;
    
    memset(node, 0, sizeof(node));
    
    // start to construct tree
    for (int i = 1; i < len && str[i] != '\n'; i++)
    {
        node[i].content = str[i];
        node[i].left = NULL;
        node[i].right = NULL;
        node[i].father = NULL;
        
    coming_back:
        if (pivot == &root && root.left == NULL)
        {
            switch(str[i])
            {
                case '+':
                case '-':
                case '*':
                case '/':
                {
                    node[i].father = &root;
                    root.left = &node[i];
                    pivot = &node[i];
                    break;
                }
                default:
                {
                    node[i].father = &root;
                    root.left = &node[i];
                    break;
                }
            }
            continue;
        }
        if (pivot == &root && root.right == NULL)
        {
            switch(str[i])
            {
                case '+':
                case '-':
                case '*':
                case '/':
                {
                    node[i].father = &root;
                    root.right = &node[i];
                    pivot = &node[i];
                    break;
                }
                default:
                {
                    node[i].father = &root;
                    root.right = &node[i];
                    break;
                }
            }
            continue;
        }
        if (pivot != &root && pivot->left == NULL)
        {
            switch(str[i])
            {
                case '+':
                case '-':
                case '*':
                case '/':
                {
                    node[i].father = pivot;
                    pivot->left = &node[i];
                    pivot = &node[i];
                    break;
                }
                default:
                {
                    node[i].father = pivot;
                    pivot->left = &node[i];
                    break;
                }
            }
            continue;
        }
        if (pivot != &root && pivot->right == NULL)
        {
            switch(str[i])
            {
                case '+':
                case '-':
                case '*':
                case '/':
                {
                    node[i].father = pivot;
                    pivot->right = &node[i];
                    pivot = &node[i];
                    break;
                }
                default:
                {
                    node[i].father = pivot;
                    pivot->right = &node[i];
                    break;
                }
            }
            continue;
        }
        if (pivot->left != NULL && pivot->right != NULL)
        {
            do{
                pivot = pivot->father;
            }while(pivot->left != NULL && pivot->right != NULL);
            goto coming_back;
        }
    }
    
    // Now, find the max depth
    tree_len = maxDepth(&root) - 1;

    return tree_len;
}

int main(int argc, const char * argv[]) {
    
    FILE* fp;
    FILE* outfile;
    
    double len_array[100] = {0};
    char str_tree[MX_SIZE+2] = {0};
    long sum = 0;
    
    fp = fopen("treeStructs.txt", "r");
    
    if (fp == NULL)
    {
        printf("Open file failed!\n");
        return -1;
    }
    
    //printf("%d\n", tree_builder(str_tree, (int)strlen(str_tree)));

    for (int j = 0; j < 100; j++)
    {
        for (int i = 0; i < 100; i++)
        {
            fgets(str_tree, MX_SIZE+1, fp);
            sum += tree_builder(str_tree, (int)strlen(str_tree));
        }
        
        len_array[j] = (double) sum / 100.0;
        printf("%f\n", len_array[j]);
        sum = 0;
        memset(len_array, 0.0, 100);
    }
    
    outfile = fopen("meanLen.txt", "w");
    for (int i = 0; i < 100; i++)
        fprintf(outfile, "%f\n", len_array[i]);
    
    fclose(fp);
    fclose(outfile);

    return 0;
}
