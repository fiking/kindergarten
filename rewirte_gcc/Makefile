CFLAGS = -g -I.
CC = gcc
OBSTACK1=obstack.o

OBJS = toplev.o

all: gcc cc1 cpp
gcc: gcc.o $(OBSTACK1)
	$(CC) gcc.o $(OBSTACK1) -o gcc

gcc.o: gcc.c
	$(CC) -c gcc.c $(CFLAGS)

obstack.o: obstack.c
	$(CC) -c obstack.c $(CFLAGS)


cc1: $(OBJS) $(OBSTACK1)
	$(CC) -o cc1 $(OBJS) $(OBSTACK1)

# Making the preprocessor
cpp: cccp
	-rm -f cpp
	ln cccp cpp

cccp: cccp.o
	cc -o cccp -g cccp.o

y.tab.o: y.tab.c
y.tab.c: cexp.y
	echo expect 40 shift/reduce conflicts
	yacc cexp.y
cccp.o: cccp.c
