CFLAGS = -g -I.
CC = gcc
OBSTACK1=obstack.o

all: gcc
gcc: gcc.o $(OBSTACK1)
	$(CC) gcc.o $(OBSTACK1) -o gcc

gcc.o: gcc.c
	$(CC) -c gcc.c $(CFLAGS)

obstack.o: obstack.c
	$(CC) -c obstack.c $(CFLAGS)
