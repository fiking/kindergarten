#include <stdio.h>
#include <signal.h>
#include <unistd.h>
#include <stdlib.h>
#include <string.h>
#include "obstack.h"

#define obstack_chunk_alloc xmalloc

#define SWITCH_TAKES_ARG(CHAR)      \
  (CHAR == 'D' || CHAR == 'U' || CHAR == 'o' || CHAR == 'e' || CHAR == 'T'  \
   || CHAR == 'u' || CHAR == 'I' || CHAR == 'Y' || CHAR == 'd' || CHAR == 'm')

int argbuf_length;
char **argbuf;

struct obstack obstack;

char *temp_filename;
int temp_filename_length;

char **infiles;
int n_infiles;
char **outfiles;

char **switches;
int n_switches;

char *user_exec_prefix = 0;
unsigned char vflag;

char *input_filename;
int input_file_number;
int input_filename_length;

int basename_length;
char *input_basename;

int argbuf_index;
int arg_going;
int delete_this_arg;
int this_is_output_file;

/* This structure says how to run one compiler, and when to do so.  */
struct compiler
{
  char *suffix;  /* Use this compiler for input files 
                    whose names end in this suffix.  */
  char *spec;    /* To use this compiler, pass this spec
                    to do_spec.  */
};

/* Here are the specs for compiling files with various known suffixes.
   A file that does not end in any of these suffixes will be passed
   unchanged to the loader, but that is all.  */
struct compiler compilers[] =
{
  /* Note that we use the "cc1" from $PATH. */
  {".c",
   "cpp %{C} %p %{pedantic} %{D*} %{U*} %{I*} %i %{!E:%g.cpp}\n\
    %{!E:cc1 %g.cpp -quiet -dumpbase %i %{Y*} %{d*} %{m*} %{w} %{pedantic}\
	%{O:-opt}%{!O:-noreg}\
	%{g:-G}\
	-o %{S:%b}%{!S:%g}.s\n\
	%{!S:as %{R} -o %{!c:%d}%w%b.o %g.s\n }}"},

  {".s",
   "%{!S:as %{R} %i -o %{!c:%d}%w%b.o\n }"},

  /* Mark end of table */
  {0, 0}
};


void 
delete_temp_files ()
{
  /* Todo: write later */
  printf("delete_temp_file\n");
  return;
}

void
fatal_error (signum)
    int signum;
{
  signal (signum, SIG_DFL);
  delete_temp_files ();
  kill (getpid (), signum);
}

void
error (msg, arg1, arg2)
    char *msg, *arg1, *arg2;
{
  fprintf(stderr, "cc: ");
  fprintf(stderr, msg, arg1, arg2);
  fprintf(stderr, "\n");
}

void
fatal (msg, arg1, arg2)
    char *msg, *arg1, *arg2;
{
  error (msg, arg1, arg2);
  delete_temp_files ();
  exit(1);
}

void *
xmalloc (size)
    int size;
{
  void *p = malloc (size);
  if (p == NULL)
    printf ("Virtual memory full.");
  return p;
}

void
choose_temp_base ()
{
  char *foo = "/tmp/ccXXXXXX";
  temp_filename = (char *) xmalloc (strlen (foo) + 1);
  strcpy (temp_filename, foo);
  mkstemp (temp_filename);
  temp_filename_length = strlen (temp_filename);
}

char *
make_switch (p1, s1, p2, s2)
    char *p1;
	int s1;
	char *p2;
	int s2;
{
  char *new;
  if (p2 && s2 == 0) {
    s2 = strlen (p2);
  }
  new = (char *) xmalloc (s1 + s2 + 2);
  bcopy (p1, new, s1);
  if (p2) {
    new[s1++] = ' ';
	bcopy (p2, new + s1, s2);
  }
  new[s1 + s2] = 0;
  return new;
}

void
process_command (argc, argv)
    int argc;
	char **argv;
{
  int i;
  n_switches = 0;
  n_infiles = 0;

  for (i = 1; i < argc; i++) {
    if (argv[i][0] == '-' && argv[i][1] != 'l') {
	  char *p = &argv[i][1];
	  int c = *p;

	  switch (c) {
	    case 'B':
		  user_exec_prefix = p + 1;
		  break;

		case 'v':   /* Print commands as we execute them */
		  vflag++;

		default:
		  n_switches++;
		  if (SWITCH_TAKES_ARG (c) && p[1] == 0)
		    i++;
	  }
	} else {
	  n_infiles++;
	}
  }

  /* Then create the space for the vectors and scan again.  */

  switches = (char **) xmalloc ((n_switches + 1) * sizeof (char *));
  infiles = (char **) xmalloc ((n_infiles + 1) * sizeof (char *));
  n_switches = 0;
  n_infiles = 0;

  for (i = 1; i < argc; i++) {
    if (argv[i][0] == '-' && argv[i][1] != 'l') {
	  char *p = &argv[i][1];
	  int c = *p;

	  if (c == 'B') {
	    continue;
	  }

	  if (SWITCH_TAKES_ARG (c) && p[1] == 0) {
	    switches[n_switches++] = make_switch (p, 1, argv[++i], 0);
	  } else {
	    switches[n_switches++] = make_switch (p, strlen (p), (char *)0, 0);
	  }
	} else {
	  infiles[n_infiles++] = argv[i];
	}
  }

  switches[n_switches] = 0;
  infiles[n_infiles] = 0;
}

void 
clear_args ()
{
  argbuf_index = 0;
}

int
do_spec (spec)
    char spec;
{
  int value;
  clear_args ();
  arg_going = 0;
  delete_this_arg = 0;
  this_is_output_file = 0;
  value = do_spec_1 (spec, 0);
  if (value == 0) {
    value = do_spec_1 ("\n", 0);
  }
  return value;
}

int 
main (argc, argv)
      int argc;
      char **argv;
{
  int i;
  int value;
  int nolink = 0;

  signal (SIGINT, fatal_error);
  signal (SIGKILL, fatal_error);

  argbuf_length = 10;
  argbuf = (char **) xmalloc (argbuf_length * sizeof (char *));

  obstack_init (&obstack);

  choose_temp_base ();

  process_command (argc, argv);

  outfiles = (char **) xmalloc (n_infiles * sizeof (char *));
  bzero (outfiles, n_infiles * sizeof (char *));

  for (i = 0; i < n_infiles; i++) {
    /* First figure out which compiler from the file's suffix.  */
	struct compiler *cp;
    
	/* Tell do_spec what to substitute for %i.  */
	input_filename = infiles[i];
	input_filename_length = strlen (input_filename);
	input_file_number = i;

	/* Use the same thing in %o, unless cp->spec says otherwise.  */
	outfiles[i] = input_filename;

	for (cp = compilers; cp->spec; cp++) {
	  if (strlen (cp->suffix) < input_filename_length &&
	      !strcmp (cp->suffix, 
		    infiles[i] + input_filename_length - strlen (cp->suffix))) {
			
	    char *p;
		input_basename = input_filename;
		for (p = input_filename; *p; p++) {
		  if (*p == '/') {
		    input_basename = p + 1;
		  }
		}
		basename_length = (input_filename_length - strlen (cp->suffix)
		    - (input_basename - input_filename));
        value = do_spec (cp->spec);
	    if (value < 0) {
		  nolink = 1;
		}
		break;
	  }
	}
  }

  /* Run ld to link all the compiler output files.  */
  return 0;
}
