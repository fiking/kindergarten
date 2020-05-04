CFLAGS = -g -I.
CC = gcc
OBSTACK1=obstack.o

OBJS = toplev.o tree.o decl.o stor-layout.o \
       rtl.o fold-const.o typecheck.o parse.tab.o \
       varasm.o final.o

all: gcc cc1 cpp
gcc: gcc.o $(OBSTACK1)
	$(CC) gcc.o $(OBSTACK1) -o gcc

gcc.o: gcc.c
	$(CC) -c gcc.c $(CFLAGS)

obstack.o: obstack.c
	$(CC) -c obstack.c $(CFLAGS)


cc1: $(OBJS) $(OBSTACK1)
	$(CC) -o cc1 $(OBJS) $(OBSTACK1)


toplev.o : toplev.c
tree.o : tree.c
decl.o : decl.c
stor-layout.o : stor-layout.c
rtl.o : rtl.c
fold-const.o : fold-const.c
typecheck.o : typecheck.c
parse.tab.o : parse.tab.c
varasm.o : varasm.c
final.o : final.c

parse.tab.c : parse.y
	bison -v parse.y

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

clean:
	-rm *.o
	-rm parse.tab.c insn-flags.h insn-config.h insn-codes.h
	-rm insn-output.c insn-recog.c insn-emit.c insn-extract.c
	-rm genemit genoutput genrecog genextract genflags gencodes genconfig
	-rm *.s *.s[0-9] *.co *.greg *.lreg *.combine *.flow *.cse *.jump *.rtl *.tree *.loop
	-rm parse.output core
