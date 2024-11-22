CFLAGS = -g -I.
CC = gcc
OBSTACK1=obstack.o

CONFIG_H =
RTL_H = rtl.h rtl.def machmode.def
TREE_H = tree.h tree.def machmode.def

OBJS = toplev.o tree.o decl.o stor-layout.o \
       rtl.o fold-const.o typecheck.o parse.tab.o \
       varasm.o final.o local-alloc.o flow.o \
	   emit-rtl.o explow.o expr.o insn-emit.o \
	   recog.o insn-recog.o insn-extract.o insn-output.o \
	   optabs.o expmed.o stmt.o stupid.o \
	   regclass.o combine.o reload.o reload1.o \
	   global-alloc.o jump.o dbxout.o print-tree.o \
	   cse.o symout.o loop.o

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
final.o : final.c insn-config.h
local-alloc.o : local-alloc.c
flow.o : flow.c
emit-rtl.o : emit-rtl.c
explow.o : explow.c insn-codes.h
expr.o: expr.c insn-flags.h
recog.o: recog.c
optabs.o : optabs.c
expmed.o : expmed.c
stmt.o : stmt.c
stupid.o : stupid.c
regclass.o: regclass.c
combine.o: combine.c
reload.o: reload.c
reload1.o: reload1.c
global-alloc.o: global-alloc.c
jump.o: jump.c
dbxout.o: dbxout.c
print-tree.o: print-tree.c
cse.o: cse.c
symout.o : symout.c
loop.o : loop.c

parse.tab.c : parse.y
	bison -v parse.y

# Making the preprocessor
cpp: cccp
	-rm -f cpp
	ln cccp cpp

cccp: cccp.o y.tab.o
	cc -o cccp -g cccp.o y.tab.o

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

# Now the source files that are generated from the machine description.
insn-config.h : md genconfig
	/home/fiking/code/gcc_code/gcc_0_9_replica/rewirte_gcc/genconfig md > insn-config.h

insn-flags.h : md genflags
	/home/fiking/code/gcc_code/gcc_0_9_replica/rewirte_gcc/genflags md > insn-flags.h

insn-codes.h : md gencodes
	/home/fiking/code/gcc_code/gcc_0_9_replica/rewirte_gcc/gencodes md > insn-codes.h

insn-emit.o : insn-emit.c $(RTL_H) insn-config.h
	$(CC) -c $(CFLAGS) insn-emit.c

insn-emit.c : md genemit
	/home/fiking/code/gcc_code/gcc_0_9_replica/rewirte_gcc/genemit md > insn-emit.c

insn-recog.o : insn-recog.c $(CONFIG_H) $(RTL_H) insn-config.h
	$(CC) -c $(CFLAGS) insn-recog.c

insn-recog.c : md genrecog
	/home/fiking/code/gcc_code/gcc_0_9_replica/rewirte_gcc/genrecog md > insn-recog.c

insn-extract.o : insn-extract.c $(RTL_H)
	$(CC) -c $(CFLAGS) insn-extract.c

insn-extract.c : md genextract
	/home/fiking/code/gcc_code/gcc_0_9_replica/rewirte_gcc/genextract md > insn-extract.c

insn-output.o : insn-output.c $(CONFIG_H) $(RTL_H) regs.h insn-config.h insn-flags.h conditions.h aux-output.c
	$(CC) -c $(CFLAGS) insn-output.c

insn-output.c : md genoutput
	/home/fiking/code/gcc_code/gcc_0_9_replica/rewirte_gcc/genoutput md > insn-output.c

# Now the programs that generate those files.
genconfig : genconfig.o $(OBSTACK1)
	$(CC) -o genconfig -g genconfig.o rtl.o $(OBSTACK1)

genflags : genflags.o $(OBSTACK1)
	$(CC) -o genflags -g genflags.o rtl.o $(OBSTACK1)

gencodes : gencodes.o $(OBSTACK1)
	$(CC) -o gencodes -g gencodes.o rtl.o $(OBSTACK1)

genemit : genemit.o $(OBSTACK1)
	$(CC) -o genemit -g genemit.o rtl.o $(OBSTACK1)

genrecog : genrecog.o $(OBSTACK1)
	$(CC) -o genrecog -g genrecog.o rtl.o $(OBSTACK1)

genextract : genextract.o $(OBSTACK1)
	$(CC) -o genextract -g genextract.o rtl.o $(OBSTACK1)

genoutput : genoutput.o $(OBSTACK1)
	$(CC) -o genoutput -g genoutput.o rtl.o $(OBSTACK1)


genconfig.o : genconfig.c rtl.def
	$(CC) -c $(CFLAGS) genconfig.c

genflags.o : genflags.c rtl.def
	$(CC) -c $(CFLAGS) genflags.c

gencodes.o : gencodes.c rtl.def
	$(CC) -c $(CFLAGS) gencodes.c

genemit.o : genemit.c rtl.def
	$(CC) -c $(CFLAGS) genemit.c

genrecog.o : genrecog.c rtl.def
	$(CC) -c $(CFLAGS) genrecog.c

genextract.o : genextract.c rtl.def
	$(CC) -c $(CFLAGS) genextract.c

genoutput.o : genoutput.c rtl.def
	$(CC) -c $(CFLAGS) genoutput.c
