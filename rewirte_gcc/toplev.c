#include <stdio.h>
#include <string.h>
#include <stdlib.h>

#include "config.h"

int yydebug;  // Todo: extern

char *input_filename; // Todo: extern

/* Current line number in real source file.  */
int lineno;  // Todo: extern

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

/* Compile an entire file of output from cpp, named NAME.
   Write a file of assembly output and various debugging dumps.  */
static void compile_file(char *name)
{
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