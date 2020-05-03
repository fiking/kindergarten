#include <stdio.h>
#include <signal.h>
#include <unistd.h>
#include <stdlib.h>
#include <string.h>
#include "obstack.h"
#include "config.h"

#define obstack_chunk_alloc xmalloc

#define SWITCH_TAKES_ARG(CHAR)      \
  (CHAR == 'D' || CHAR == 'U' || CHAR == 'o' || CHAR == 'e' || CHAR == 'T'  \
   || CHAR == 'u' || CHAR == 'I' || CHAR == 'Y' || CHAR == 'd' || CHAR == 'm')

#define MIN_FATAL_STATUS 32

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

/* User-specified prefix to attach to command names,
   or 0 if none specified.  */
char *user_exec_prefix = 0;
/* Default prefixes to attach to command names.  */
char *standard_exec_prefix = "/usr/local/lib/gcc-";
char *standard_exec_prefix_1 = "/usr/lib/gcc-";

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

/* Here is the spec for running the linker, after compiling all files.  */
char *link_spec = "%{!c:%{!E:%{!S:ld %{o*}\
    %{A} %{d} %{e*} %{M} %{N} %{n} %{r} %{s} %{S} %{T*} %{t} %{u*} %{X} %{x} %{z}\
	/lib/crt0.o %o %{g:-lg} -lc\n }}}";

int do_spec_1 ();

void
record_temp_file (filename)
    char *filename;
{
  /* Todo: write later */
  return;
}

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

char *
save_string (s, len)
    char *s;
	int len;
{
  char *result = (char *) xmalloc (len + 1);
  bcopy (s, result, len);
  result[len] = 0;
  return result;
}

void 
clear_args ()
{
  argbuf_index = 0;
}

void
store_arg (arg, tempnamep)
    char *arg;
	int tempnamep;
{
  if (argbuf_index + 1 == argbuf_length) {
    argbuf = (char **) realloc (argbuf, (argbuf_length *= 2) * sizeof(char *));
  }
  argbuf[argbuf_index++] = arg;
  argbuf[argbuf_index] = 0;
  if (tempnamep) {
    record_temp_file (arg);
  }
}

void 
pfatal_with_name (name)
    char *name;
{
  char *s;
  s = "cannot open %s";
  fatal (s, name);
  //Todo: write later
}

void
perror_with_name (name)
    char *name;
{
  char *s;
  s = "cannot open %s";
  error (s, name);
}

/* Execute the command specified by the arguments on the current line of spec.
   Returns 0 if successful, -1 if failed.  */
int
execute ()
{
  int pid;
  union wait status;
  int size;
  char *temp;
  int win = 0;
  
  size = strlen (standard_exec_prefix);
  if (user_exec_prefix != 0 && strlen (user_exec_prefix) > size) {
    size = strlen (user_exec_prefix);
  }
  if (strlen (standard_exec_prefix_1) > size) {
    size = strlen (standard_exec_prefix_1);
  }
  size += strlen (argbuf[0]) + 1;
  temp = (char *) alloca (size);

  /* Determine the filename to execute.  */
  if (user_exec_prefix) {
    strcpy (temp, user_exec_prefix);
	strcat (temp, argbuf[0]);
	win = (access (temp, X_OK) == 0);
  }

  if (!win) {
    strcpy (temp, standard_exec_prefix);
	strcat (temp, argbuf[0]);
	win = (access (temp, X_OK) == 0);
  }

  if (!win) {
    strcpy (temp, standard_exec_prefix_1);
	strcat (temp, argbuf[0]);
	win = (access (temp, X_OK) == 0);
  }

  if (vflag) {
    int i;
	for (i = 0; argbuf[i]; i++) {
	  if (i == 0 && win) {
	    fprintf (stderr, " %s", temp);
	  } else {
	    fprintf (stderr, " %s", argbuf[i]);
	  }
	}

	fprintf (stderr, "\n");
#ifdef DEBUG
    fprintf (stderr, "\nGo ahead? (y or n) ");
	fflush (stderr);
	i = getchar ();
	if (i != '\n') {
	  while (getchar () != '\n') ;
	  if (i != 'y' && i != 'Y') {
	    return;
	  }
	}
#endif  /* DEBUG */
  }

  pid = vfork ();
  if (pid < 0) {
    pfatal_with_name ("vfork");
  }

  if (pid == 0) {
    if (win) {
	  execv (temp, argbuf);
	} else {
	  execvp (argbuf[0], argbuf);
	}

	perror_with_name (argbuf[0]);
	_exit (65);
  }

  wait (&status);
  if (WIFSIGNALED (status)) {
    fatal ("Program %s got fatal signal %d.", argbuf[0], status.w_termsig);
  }

  if ((WIFEXITED (status) && status.w_retcode >= MIN_FATAL_STATUS)
      || WIFSIGNALED (status)) {
    return -1;
  }
  return 0;
}

/* Return 0 if we call do_spec_1 and that returns -1.  */
char *
handle_braces (p)
    char *p;
{
  char *q;
  int negate = *p == '!';
  char *filter;

  if (negate) {
    ++p;
  }
  
  filter = p;
  while (*p != ':' && *p != '}') {
    p++;
  }

  if (*p != '}') {
    int count = 1;
	q = p + 1;
	while (count > 0) {
	  if (*q == '{') {
	    count++;
	  } else if (*q == '}') {
	    count--;
	  } else if (*q == 0) {
	    abort ();
	  }
	  q++;
	}
  } else {
    q = p + 1;
  }

  if (p[-1] == '*') {
    /* Substitute all matching switches as separate args.  */
	int i;
	--p;
	for (i = 0; i < n_switches; i++) {
	  if (!strncmp (switches[i], filter, p - filter)) {
	    if (give_switch (i) < 0) {
		  return 0;
		}
	  }
	}
  } else {
    /* Test for presence of the specified switch.  */
	int i;
	int present = 0;
	for (i = 0; i < n_switches; i++) {
	  if (!strncmp (switches[i], filter, p - filter)) {
	    present = 1;
		break;
	  }
	}

	/* If it is as desired (present for %{s...}, absent for %{-s...})
	   then substitute either the switch or the specified
	   conditional text.  */
    if (present != negate) {
	  if (*p == '}') {
	    if (give_switch (i) < 0) {
		  return 0;
		}
	  } else {
	    if (do_spec_1 (save_string (p + 1, q - p - 2), 0) < 0) {
		  return 0;
		}
	  }
	}
  }
  return q;
}

int
give_switch (switchnum)
    int switchnum;
{
  do_spec_1 ("-", 0);
  do_spec_1 (switches[switchnum], 0);
  do_spec_1 (" ", 0);
}

int
do_spec_1 (spec, nopercent)
    char *spec;
	int nopercent;
{
  char *p = spec;
  int c;
  char *string;

  while (c = *p++) {
    switch (c) {
	  case '\n':
	  /* End of line: finish any pending argument,
	     then run the pending command if one has been started.  */
	    if (arg_going) {
		  obstack_1grow (&obstack, 0);
		  string = obstack_finish (&obstack);
		  store_arg (string, delete_this_arg);
		  if (this_is_output_file) {
		    outfiles[input_file_number] = string;
		  }
		}
		arg_going = 0;
		if (argbuf_index) {
		  int value = execute ();
		  if (value) {
		    return value;
		  }
		}
		/* Reinitialize for a new command, and for a new argument.  */
		clear_args ();
		arg_going = 0;
		delete_this_arg = 0;
		this_is_output_file = 0;
		break;

      case '\t':
	  case ' ':
	  /* Space or tab ends an argument if one is pending.  */
	    if (arg_going) {
		  obstack_1grow (&obstack, 0);
		  string = obstack_finish (&obstack);
		  store_arg (string, delete_this_arg);
		  if (this_is_output_file) {
		    outfiles[input_file_number] = string;
		  }
		}
		/* Reinitialize for a new argument.  */
		arg_going = 0;
		delete_this_arg = 0;
		this_is_output_file = 0;
	    break;

      case '%':
        if (! nopercent) {
		  switch (c = *p++) {
		    case 0:
			  fatal ("Invalid specification!  Bug in cc.");

			case 'i':
			  obstack_grow (&obstack, input_filename, input_filename_length);
			  arg_going = 1;
			  break;

			case 'b':
			  obstack_grow (&obstack, input_basename, basename_length);
			  arg_going = 1;
			  break;

			case 'p':
			  do_spec_1 (CPP_PREDEFINES, 1);
			  break;

			case 'g':
			  obstack_grow (&obstack, temp_filename, temp_filename_length);
			  delete_this_arg = 1;
			  arg_going = 1;
			  break;

	        case 'd':
			  delete_this_arg = 1;
			  break;

			case 'w':
			  this_is_output_file = 1;
			  break;

			case 'o': {
			  int f;
			  for (f = 0; f < n_infiles; f++) {
			    store_arg (outfiles[f], 0);
			  }
			  break;
			}

			case '{':
			  p = handle_braces (p);
			  if (p == 0) {
			    return -1;
			  }
			  break;

			case '%':
			  obstack_1grow (&obstack, '%');
			  break;

			default:
			  abort ();
		  }
		} else {
		  obstack_1grow (&obstack, c);
		  arg_going = 1;
		}
		break;

	  default:
	    obstack_1grow (&obstack, c);
		arg_going = 1;
	    break;
	}
  }

  return 0;  /* End of string */
}

int
do_spec (spec)
    char *spec;
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
  if (! nolink) {
    do_spec (link_spec);
  }

  /* Delete all the temporary files we made.  */
  delete_temp_files ();
  return 0;
}
