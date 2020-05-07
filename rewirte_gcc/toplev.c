#include <stdio.h>
#include <string.h>
#include <stdlib.h>
#include <sys/time.h>
#include <sys/resource.h>
#include "config.h"
#include "tree.h"
#include "rtl.h"

#define TIMEVAR(VAR, BODY)    \
  { int otime = gettime (); BODY; VAR += gettime () - otime; }

int yydebug;  // Todo: extern

char *input_filename; // Todo: extern

/* Current line number in real source file.  */
int lineno;  // Todo: extern

FILE *finput;  // Todo: extern

extern void init_tree ();

int target_flags;

char *dump_base_name;

/* Flags saying which kinds of debugging dump have been requested.  */
int tree_dump = 0;
int rtl_dump = 0;
int rtl_dump_and_exit = 0;
int jump_opt_dump = 0;
int cse_dump = 0;
int loop_dump = 0;
int flow_dump = 0;
int combine_dump = 0;
int local_reg_dump = 0;
int global_reg_dump = 0;

/* Time accumulators, to count the total time spent in various passes.  */
int parse_time;
int varconst_time;
int expand_time;
int jump_time;
int cse_time;
int loop_time;
int flow_time;
int combine_time;
int local_alloc_time;
int global_alloc_time;
int final_time;
int symout_time;
int dump_time;

FILE *asm_out_file;

/* 1 => write gdb debugging output (using symout.c).  -g
   2 => write dbx debugging output (using dbxout.c).  -G  */
int write_symbols = 0;

/* Nonzero means do optimizations.  -opt.  */
int optimize = 0;

int force_mem = 0;

int force_addr = 0;

int obey_regdecls = 0;

int quiet_flag = 0;

int inhibit_warnings = 0;

/* Number of error messages and warning messages so far.  */
int errorcount = 0;
int warningcount = 0;

/* Name for output file of GDB symbol segment, specified with -symout.  */
char *sym_file_name;

/* Name for output file of assembly code, specified with -o.  */
char *asm_file_name;

/* Decode -m switches.  */
struct {char *name; int value;} target_switches []
    = TARGET_SWITCHES;

/* Nonzero for -pedantic switch: warn about anything
   that standard C forbids.  */
int pedantic = 0;

void
set_target_switch (name)
    char *name;
{
  int j = 0;
  for (j = 0; j < sizeof target_switches / sizeof target_switches[0]; j++) {
    if (!strcmp (target_switches[j].name, name)) {
	  if (target_switches[j].value < 0) {
	    target_flags &= ~-target_switches[j].value;
	  } else {
	    target_flags |= target_switches[j].value;
	  }
	  break;
	}
  }
}

/* Report a warning at line LINE.
   S and V are a string and an arg for `printf'.  */
void
warning_with_line (line, s, v)
    int line;
	char *s;
	int v;
{
  // Todo: write later
  fprintf (stderr, "%s:%d: ", input_filename, line);
  fprintf (stderr, "warning: ");
  fprintf (stderr, s, v);
  fprintf (stderr, "\n");
}

void
warning (s, v)
    char *s;
	int v;         /* @@also used as pointer */
{
  warning_with_line (lineno, s, v);
}

/* Report an error at line LINE.
   S and V are a string and an arg for `printf'.  */
void
yylineerror (line, s, v)
    int line;
    char *s;
    int v;
{
  // Todo: write later
  fprintf (stderr, "%s:%d: ", input_filename, line);
  fprintf (stderr, s, v);
  fprintf (stderr, "\n");
}

void
yyerror (s, v)
    char *s;
	int v;         /* @@also used as pointer */
{
  yylineerror (lineno, s, v);
}

void
fatal (s)
    char *s;
{
  yyerror (s, 0);
  exit (34);
}

int
pfatal_with_name (char *name)
{
  fprintf (stderr, "cc1: ");
  perror (name);
  exit (35);
}

/* Same as `malloc' but report error if no memory available.  */
char *
xmalloc (size)
    unsigned size;
{
  char *value = (char *) malloc (size);
  if (value == 0) {
    fatal ("Virtual memory exhausted.");
  }
  return value;
}

/* Same as `realloc' but report error if no memory available.  */
char *
xrealloc (ptr, size)
    char *ptr;
	int size;
{
  char *result = realloc (ptr, size);
  if (!result) {
    abort ();
  }
  return result;
}

/* Return the logarithm of X, base 2, considering X unsigned,
   if X is a power of 2.  Otherwise, returns -1.  */
int
exact_log2 (x)
    register unsigned int x;
{
  register int log = 0;
  for (log = 0; log < HOST_BITS_PER_INT; log++) {
    if (x == (1 << log)) {
	  return log;
	}
  }
  return -1;
}

int
floor_log2 (x)
    register unsigned int x;
{
  register int log = 0;
  for (log = 0; log < HOST_BITS_PER_INT; log++) {
     if ((x & ((-1) << log)) == 0) {
	   return log - 1;
	 }
  }
  return HOST_BITS_PER_INT - 1;
}

void
announce_function (decl)
	tree decl;
{
  if (! quiet_flag) {
    printf (stderr, " %s", IDENTIFIER_POINTER (DECL_NAME (decl)));
	fflush (stderr);
  }
}

int
gettime ()
{
  struct rusage rusages;
  if (quiet_flag) {
    return 0;
  }
  getrusage (0, &rusages);

  return (rusages.ru_utime.tv_sec * 1000000 + rusages.ru_utime.tv_usec
      + rusages.ru_stime.tv_sec * 1000000 + rusages.ru_stime.tv_usec);
}

void
rest_of_compilation (decl, top_level)
    tree decl;
	int top_level;
{
  register rtx insns;
  int start_time = gettime ();
  int tem;

  /* Declarations of variables, and of functions defined elsewhere.  */
  if ((TREE_CODE (decl) == VAR_DECL || (TREE_CODE (decl) == FUNCTION_DECL
      && DECL_INITIAL (decl) == 0))
	  && (TREE_STATIC  (decl) || TREE_EXTERNAL (decl))) {
    TIMEVAR (varconst_time, 
	         {
			   assemble_variable (decl, top_level);
			   if (write_symbols == 2)
			     dbxout_symbol (decl, 0);
			 });
  } //else if (TREE_CODE (decl) == FUNCTION_DECL &&
    //         && DECL_INITIAL (decl)) {

  /* Function definitions are the real work (all the rest of this function).  */
    // Todo: write later
//  }
}

/* Compile an entire file of output from cpp, named NAME.
   Write a file of assembly output and various debugging dumps.  */
static void compile_file(char *name)
{
  tree globals;
  int start_time;
  int dump_base_name_length = strlen (dump_base_name);

  parse_time = 0;
  varconst_time = 0;
  expand_time = 0;
  jump_time = 0;
  cse_time = 0;
  loop_time = 0;
  flow_time = 0;
  combine_time = 0;
  local_alloc_time = 0;
  global_alloc_time = 0;
  final_time = 0;
  symout_time = 0;
  dump_time;

  /* Open input file.  */
  finput = fopen (name, "r");
  if (finput == 0) {
    pfatal_with_name (name);
  }

  init_tree ();
// Todo: write later
  return;
}


/* Entry point of cc1.  Decode command args, then call compile_file.
   Exit code is 34 if fatal error, else 33 if have error messages,
   else 1 if have warnings, else 0.  */
int main(int argc, char **argv, char **envp)
{
  int i;
  char *filename;

  target_flags = 0;
  set_target_switch ("");

  printf("cc1 is ok!\n");

  for (i = 1; i < argc; i++) {
    if (argv[i][0] == '-') {
	  register char *str = argv[i] + 1;
	  if (str[0] == 'Y') {
	    str++;
	  }

	  if (str[0] == 'm') {
	    set_target_switch (&str[1]);
	  } else if (!strcmp (str, "dumpbase")) {
	    dump_base_name = argv[++i];
	  } else if (str[0] == 'd') {
	    register char *p = &str[1];
		while (*p) {
		  switch (*p++) {
		    case 'c':
  			  combine_dump = 1;
			  break;

            case 'f':
			  flow_dump = 1;
			  break;

			case 'g':
			  global_reg_dump = 1;
			  break;

            case 'j':
              jump_opt_dump = 1;
			  break;

			case 'l':
			  local_reg_dump = 1;
			  break;

			case 'L':
			  loop_dump = 1;
			  break;

			case 'r':
			  rtl_dump = 1;
			  break;

			case 's':
			  cse_dump = 1;
			  break;

			case 't':
			  tree_dump = 1;
			  break;

			case 'y':
			  yydebug = 1;
			  break;
		  }
		}
	  } else if (!strcmp (str, "quiet")) {
	    quiet_flag = 1;
	  } else if (!strcmp (str, "opt")) {
	    optimize = 1;
	  } else if (!strcmp (str, "optforcemem")) {
	    force_mem = 1;
	  } else if (!strcmp (str, "optforceaddr")) {
	    force_addr = 1;
	  } else if (!strcmp (str, "noreg")) {
	    obey_regdecls = 1;
	  } else if (!strcmp (str, "w")) {
	    inhibit_warnings = 1;
	  } else if (!strcmp (str, "g")) {
	    write_symbols = 1;
	  } else if (!strcmp (str, "G")) {
	    write_symbols = 2;
	  } else if (!strcmp (str, "symout")) {
	    if (write_symbols == 0) {
		  write_symbols = 1;
		}
		sym_file_name = argv[++i];
	  } else if (!strcmp (str, "o")) {
	    asm_file_name = argv[++i];
	  } else {
	    yylineerror (0, "Invalid switch, %s.", argv[i]);
	  }
	} else {
	  filename = argv[i];
	}
  }

  if (filename == 0) {
    fatal ("no input file specified");
  }

  if (dump_base_name == 0) {
    dump_base_name = filename;
  }

  compile_file (filename);

  if (errorcount) {
    return 33;
  } else {
    return (warningcount > 0);
  }
  return 0;
}
