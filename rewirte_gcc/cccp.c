#include <stdio.h>
#include <sys/stat.h>
#include <stdlib.h>
#include <string.h>

#include <fcntl.h>
#include <unistd.h>
#include <ctype.h>

#define FATAL_EXIT_CODE 33 /* gnu cc command understands this */
#define FNAME_HASHSIZE 37

typedef unsigned char U_CHAR;

/* I/O buffer structure.
   The `fname' field is nonzero for source files and #include files
   and for the dummy text used for -D and -U.
   It is zero for rescanning results of macro expansion
   and for expanding macro arguments.  */
#define INPUT_STACK_MAX 100
struct file_buf {
	char *fname;
	int lineno;
	int length;
	U_CHAR *buf;
	U_CHAR *bufp;
	/* Macro that this level is the expansion of.
	   Included so that we can reenable the macro
	   at the end of this level.  */
	struct hashnode *macro;
	/* Object to be freed at end of input at this level.  */
	U_CHAR *free;
} instack[INPUT_STACK_MAX];

int indepth = -1;

typedef struct file_buf FILE_BUF;

/* different flavors of hash nodes --- also used in keyword table */
enum node_type {
	T_DEFINE = 1,  /* the "#define" keyword */
	T_INCLUDE, /* the "#include" keyword */
	T_IFDEF,   /* the "#ifdef" keyword */
	T_IFNDEF,  /* the "#ifndef" keyword */
	T_IF,      /* the "#if" keyword */
	T_ELSE,    /* "#else" */
	T_PRAGMA,  /* "#pragma" */
	T_ELIF,    /* "#else" */
	T_UNDEF,   /* "#undef" */
	T_LINE,    /* "#line" */
	T_ERROR,   /* "#error" */
	T_ENDIF,   /* "#endif" */
	T_SPECLINE,    /* special symbol "__LINE__" */
	T_DATE,    /* "__DATE__" */
	T_FILE,    /* "__FILE__" */
	T_TIME,    /* "__TIME__" */
	T_CONST,   /* Constant value, used by "__STDC__" */
	T_MACRO,   /* macro defined by "#define" */
	T_DISABLED,    /* macro temporarily turned off for rescan */
	T_SPEC_DEFINED /* special `defined' macro for use in #if statements */
};

typedef struct definition DEFINITION;

/* different kinds of things that can appear in the value field
   of a hash node.  Actually, this may be useless now. */
union hashval {
	int ival;
	char *cpval;
	DEFINITION *defn;
};

struct hashnode {
	struct hashnode *next;    /* double links for easy deletion */
	struct hashnode *prev;
	struct hashnode **bucket_hdr; /* also, a back pointer to this node's hash
									 chain is kept, in case the node is the head
									 of the chain and gets deleted. */
	enum node_type type;      /* type of special token */
	int length;               /* length of token, for quick comparison */
	U_CHAR *name;             /* the actual name */
	union hashval value;      /* pointer to expansion, or whatever */
};

typedef struct hashnode HASHNODE;

#define HASHSIZE 1403
HASHNODE *hashtab[HASHSIZE];
#define HASHSTEP(old, c) ((old << 2) + c)
#define MAKE_POS(v) (v & ~0x80000000) /* make number positive */

struct definition {
	int nargs;
	int length;           /* length of expansion string */
	U_CHAR *expansion;
	struct reflist {
		struct reflist *next;
		char stringify;     /* nonzero if this arg was preceded by a
# operator. */
		char raw_before;        /* Nonzero if a ## operator before arg. */
		char raw_after;     /* Nonzero if a ## operator after arg. */
		int nchars;         /* Number of literal chars to copy before
							   this arg occurrence.  */
		int argno;          /* Number of arg to substitute (origin-0) */
	} *pattern;
	/* Names of macro args, concatenated in reverse order
	   with comma-space between them.
	   The only use of this is that we warn on redefinition
	   if this differs between the old and new definitions.  */
	U_CHAR *argnames;
};

struct if_stack {
	struct if_stack *next;    /* for chaining to the next stack frame */
	char *fname;      /* copied from input when frame is made */
	int lineno;           /* similarly */
	int if_succeeded;     /* true if a leg of this if-group
							 has been passed through rescan */
	enum node_type type;      /* type of last directive seen in this group */
};
typedef struct if_stack IF_STACK_FRAME;
IF_STACK_FRAME *if_stack = NULL;


struct directory_stack
{
    struct directory_stack *next;
	char *fname;
};

struct directory_stack default_includes[2] =
{
  { &default_includes[1], "." },
  { 0, "/usr/include" }
};

struct directory_stack *include = &default_includes[0];

/* `struct directive' defines one #-directive, including how to handle it.  */

struct directive {
	int length;           /* Length of name */
	int (*func)();        /* Function to handle directive */
	char *name;           /* Name of directive */
	enum node_type type;      /* Code which describes which directive. */
};

int do_define (), do_line (), do_include (), do_undef (), do_error (),
    do_pragma (), do_if (), do_xifdef (), do_else (),
    do_elif (), do_endif ();

void rescan ();
/* Here is the actual list of #-directives, most-often-used first.  */

struct directive directive_table[] = {
	{  6, do_define, "define", T_DEFINE},
	{  2, do_if, "if", T_IF},
	{  5, do_xifdef, "ifdef", T_IFDEF},
	{  6, do_xifdef, "ifndef", T_IFNDEF},
	{  5, do_endif, "endif", T_ENDIF},
	{  4, do_else, "else", T_ELSE},
	{  4, do_elif, "elif", T_ELIF},
	{  4, do_line, "line", T_LINE},
	{  7, do_include, "include", T_INCLUDE},
	{  5, do_undef, "undef", T_UNDEF},
	{  5, do_error, "error", T_ERROR},
	{  6, do_pragma, "pragma", T_PRAGMA},
	{  -1, 0, "", (enum node_type) -1},
};

struct arglist {
  struct arglist *next;
  U_CHAR *name;
  int length;
  int argno;
};

/* Name under which this program was invoked.  */
char *progname;

int max_include_len = 14;

/* table to tell if char can be part of a C identifier. */
U_CHAR is_idchar[256];
/* table to tell if char can be first char of a c identifier. */
U_CHAR is_idstart[256];
/* table to tell if c is horizontal space.  isspace () thinks that
   newline is space; this is not a good idea for this program. */
U_CHAR is_hor_space[256];

#define SKIP_WHITE_SPACE(p) do { while (is_hor_space[*p]) p++; } while (0)
#define SKIP_ALL_WHITE_SPACE(p) do { while (isspace (*p)) p++; } while (0)

U_CHAR *grow_outbuf ();

#define check_expand(OBUF, NEEDED)  \
    (((OBUF)->length - ((OBUF)->bufp - (OBUF)->buf) <= (NEEDED))   \
   	 ? grow_outbuf ((OBUF), (NEEDED)) : 0)

#ifdef PREDEFS
char *predefs = PREDEFS;
#else
char *predefs = "";
#endif

/* Nonzero means don't process the ANSI trigraph sequences.  */
int no_trigraphs = 0;

/* Nonzero means don't output line number information.  */
int no_line_commands;

/* Nonzero means inhibit output of the preprocessed text
   and instead output the definitions of all user-defined macros
   in a form suitable for use as input to cccp.  */
int dump_macros;

int pedantic;

int no_output;

int put_out_comments = 0;

void
fatal (str, arg)
    char *str, *arg;
{
  fprintf (stderr, "%s: ", progname);
  fprintf (stderr, str, arg);
  fprintf (stderr, "\n");
  exit (FATAL_EXIT_CODE);
}

void
perror_with_name (name)
    char *name;
{
  /* Todo: write later */
  fprintf (stderr, "cannot open %s\n", name);
}

void
pfatal_with_name (name)
    char *name;
{
  perror_with_name (name);
  exit (FATAL_EXIT_CODE);
}

/*
 * return hash function on name.  must be compatible with the one
 * computed a step at a time, elsewhere
 */
int
hashf (name, len, hashsize)
	register U_CHAR *name;
	register int len;
	int hashsize;
{
  register int r = 0;

  while (len--) {
    r = HASHSTEP (r, *name++);
  }

  return MAKE_POS (r) % hashsize;
}

static void
memory_full ()
{
  fatal ("Memory exhausted.");
}


char *
xmalloc (size)
	int size;
{
  register char *ptr = malloc (size);
  if (ptr != 0) return (ptr);
  memory_full ();
  /*NOTREACHED*/
}

char *
xrealloc (old, size)
	char *old;
	int size;
{
  register char *ptr = realloc (old, size);
  if (ptr != 0) return (ptr);
  memory_full ();
  /*NOTREACHED*/
}

char *
xcalloc (number, size)
    int number, size;
{
  register int total = number * size;
  register char *ptr = malloc (total);
  if (ptr != 0) {
    if (total > 100) {
	  bzero (ptr, total);
	} else {
	  /* It's not too long, so loop, zeroing by longs.
	     It must be safe because malloc values are always well aligned.  */
	  register long *zp = (long *) ptr;
	  register long *zl = (long *) (ptr + total - 4);
	  register int i = total - 4;
	  while (zp < zl) {
	    *zp++ = 0;
	  }

	  if (i < 0) {
	    i = 0;
	  }
	  while (i < total) {
	    ptr[i++] = 0;
	  }
	}
  }
  memory_full ();
  /*NOTREACHED*/
}

void
delete (hp)
    HASHNODE *hp;
{
  if (hp->prev != NULL) {
    hp->prev->next = hp->next;
  }

  if (hp->next != NULL) {
    hp->next->prev = hp->prev;
  }

  /* make sure that the bucket chain header that
     the deleted guy was on points to the right thing afterwards. */
  if (hp == *hp->bucket_hdr) {
    *hp->bucket_hdr = hp->next;
  }

  if (hp->type == T_MACRO) {
    DEFINITION *d = hp->value.defn;
	struct reflist *ap, *nextap;

	for (ap = d->pattern; ap != NULL; ap = nextap) {
	  nextap = ap->next;
	  free (ap);
	}
	free (d);
  }
  free (hp);
}

/*
 * install a name in the main hash table, even if it is already there.
 *   name stops with first non alphanumeric, except leading '#'.
 * caller must check against redefinition if that is desired.
 * delete () removes things installed by install () in fifo order.
 * this is important because of the `defined' special symbol used
 * in #if, and also if pushdef/popdef directives are ever implemented.
 *
 * If LEN is >= 0, it is the length of the name.
 * Otherwise, compute the length by scanning the entire name.
 *
 * If HASH is >= 0, it is the precomputed hash code.
 * Otherwise, compute the hash code.
 */
HASHNODE *
install (name, len, type, value, hash)
	U_CHAR *name;
	int len;
	enum node_type type;
	int value;
	int hash;
	/* watch out here if sizeof (U_CHAR *) != sizeof (int) */
{
  register HASHNODE *hp;
  register int i, bucket;
  register U_CHAR *p, *q;

  if (len < 0) {
	  p = name;
	  while (is_idchar[*p])
		  p++;
	  len = p - name;
  }

  if (hash < 0) {
	  hash = hashf (name, len, HASHSIZE);
  }

  i = sizeof (HASHNODE) + len + 1;
  hp = (HASHNODE *) xmalloc (i);
  bucket = hash;
  hp->bucket_hdr = &hashtab[bucket];
  hp->next = hashtab[bucket];
  hashtab[bucket] = hp;
  hp->prev = NULL;
  if (hp->next != NULL)
	  hp->next->prev = hp;
  hp->type = type;
  hp->length = len;
  hp->value.ival = value;
  hp->name = ((U_CHAR *) hp) + sizeof (HASHNODE);
  p = hp->name;
  q = name;
  for (i = 0; i < len; i++)
	  *p++ = *q++;
  hp->name[len] = 0;

  return hp;
}

/* initialize random junk in the hash table and maybe other places */
void
initialize_random_junk ()
{
  int i;
  for (i = 'a'; i <= 'z'; i++) {
    ++is_idchar[i - 'a' + 'A'];
	++is_idchar[i];
	++is_idstart[i - 'a' + 'A'];
	++is_idstart[i];
  }

  for (i = '0'; i <= '9'; i++) {
    ++is_idchar[i];
  }
  ++is_idchar['_'];
  ++is_idstart['_'];

  /* horizontal space table */
  ++is_hor_space[' '];
  ++is_hor_space['\t'];
  ++is_hor_space['\v'];
  ++is_hor_space['\f'];
  ++is_hor_space['\b'];
  ++is_hor_space['\r'];

  install ("__LINE__", -1, T_SPECLINE, 0, -1);
  install ("__DATE__", -1, T_DATE, 0, -1);
  install ("__FILE__", -1, T_FILE, 0, -1);
  install ("__TIME__", -1, T_TIME, 0, -1);
  install ("__STDC__", -1, T_CONST, 1, -1);
}

/*
 * process a given definition string, for initialization
 * If STR is just an identifier, define it with value 1.
 * If STR has anything after the identifier, then it should
 * be identifier-space-definition.
 */
void
make_definition (str)
    U_CHAR *str;
{
  FILE_BUF *ip;
  struct directive *kt;
  U_CHAR *buf, *p;

  buf = str;
  p = str;
  while (is_idchar[*p]) p++;
  if (*p == 0) {
    buf = (U_CHAR *) alloca (p - buf + 4);
	strcpy (buf, str);
	strcat (buf, " 1");
  }

  ip = &instack[++indepth];
  ip->fname = "*Initialization*";
  ip->buf = ip->bufp = buf;
  ip->length = strlen (buf);
  ip->lineno = 1;
  ip->macro = 0;
  ip->free = 0;

  for (kt = directive_table; kt->type != T_DEFINE; kt++) ;

  /* pass NULL as output ptr to do_define since we KNOW it never
     does any output.... */
  do_define (buf, buf + strlen (buf) , NULL, kt);
  --indepth;
}

int 
main(argc, argv)
    int argc;
	char **argv;
{
  struct stat sbuf;
  char *in_fname, *out_fname;
  int f, i;
  FILE_BUF *fp;

  char **pend_files = (char **) alloca (argc * sizeof (char *));
  char **pend_defs = (char **) alloca (argc * sizeof (char *));
  char **pend_undefs = (char **) alloca (argc * sizeof (char *));
  int inhibit_predefs = 0;

  progname = argv[0];
  in_fname = NULL;
  out_fname = NULL;
  initialize_random_junk ();

  no_line_commands = 0;
  no_trigraphs = 1;
  dump_macros = 0;
  no_output = 0;

  bzero (pend_files, argc * sizeof (char *));
  bzero (pend_defs, argc * sizeof (char *));
  bzero (pend_undefs, argc * sizeof (char *));

  /* Process switches and find input file name.  */
  for (i = 1; i < argc; i++) {
    if (argv[i][0] != '-') {
	  if (out_fname != NULL) {
	    fatal ("Usage: %s [switches] input output\n", argv[0]);
	  } else if (in_fname != NULL) {
	    out_fname = argv[i];
		if (! freopen (out_fname, "w", stdout)) {
		  pfatal_with_name (out_fname);
		} 
	  } else {
	    in_fname = argv[i];
	  } 
	} else {
	  switch (argv[i][1]) {
	    case 'i':
		  if (argv[i][2] != 0) {
		    pend_files[i] = argv[i] + 2;
		  } else {
		    pend_files[i] = argv[++i];
		  }
		  break;

		case 'p':
		  pedantic = 1;
		  break;

		case 'd':
		  dump_macros = 1;
		  no_output = 1;
		  break;

		case 'D': {
		  char *p, *p1;
		  if (argv[i][2] != 0) {
		    p = argv[i] + 2;
		  } else {
		    p = argv[++i];
		  }

		  if ((p1 = (char *) index (p, '=')) != NULL) {
		    *p1 = ' ';
		  }
		  pend_defs[i] = p;
		  break;
		}

		case 'U':     /* JF #undef something */
		  if (argv[i][2] != 0) {
		    pend_undefs[i] = argv[i] + 2;
		  } else {
		    pend_undefs[i] = argv[++i];
		  }
		  break;

		case 'C':
		  put_out_comments = 1;
		  break;

		case 'E':         /* -E comes from cc -E; ignore it.  */
		  break;

		case 'P':
		  no_line_commands = 1;
		  break;

		case 'T':         /* Enable ANSI trigraphs */
		  no_trigraphs = 0;
		  break;

		case 'I': {         /* JF handle directory path right */
		  struct directory_stack *dirtmp;
		  dirtmp = (struct directory_stack *) xmalloc (sizeof (struct directory_stack));
		  dirtmp->next = include->next;
		  include->next = dirtmp;
		  if (argv[i][2] != 0) {
		    dirtmp->fname = argv[i] + 2;
		  } else {
		    dirtmp->fname = argv[++i];
		  }

		  include = dirtmp;
		  if (strlen (dirtmp->fname) > max_include_len) {
		    max_include_len = strlen (dirtmp->fname);
		  }
		  break;
		}

		case 'u':
		/* Sun compiler passes undocumented switch "-undef".
		   Let's assume it means to inhibit the predefined symbols.  */
		   inhibit_predefs = 1;
		   break;

		case '\0': /* JF handle '-' as file name meaning stdin or stdout */
		  if (in_fname == NULL) {
		    in_fname = "";
			break;
		  } else if (out_fname == NULL) {
		    out_fname = "stdout";
			break;
		  }  /* else fall through into error */

		default:
		  fatal ("Illegal option %s\n", argv[i]);
	  }
	}
  }
  
  /* Do standard #defines that identify processor type.  */
  if (!inhibit_predefs) {
    char *p = (char *) alloca (strlen (predefs) + 1);
	strcpy (p, predefs);
	while (*p) {
	  char *q = p;
	  while (*p && *p != ',') p++;
	  if (*p != 0) {
	    *p++= 0;
	  }
	  make_definition (q);
	}
  }
  return 0;
}

static DEFINITION *
collect_expansion (buf, end, nargs, arglist)
    U_CHAR *buf, *end;
	int nargs;
	struct arglist *arglist;
{
  DEFINITION *defn;
//Todo
  return defn;
}

HASHNODE *
lookup (name, len, hash)
    U_CHAR *name;
	int len;
	int hash;
{
  register U_CHAR *bp;
  register HASHNODE *bucket;

  if (len < 0) {
    for (bp = name; is_idchar[*bp]; bp++) ;
	len = bp - name;
  }

  if (hash < 0) {
    hash = hashf (name, len, HASHSIZE);
  }

  bucket = hashtab[hash];
  while (bucket) {
    if (bucket->length == len && strncmp (bucket->name, name, len) == 0) {
	  return bucket;
	}

	bucket = bucket->next;
  }
  return NULL;
}

void
trigraph_pcp (buf)
    FILE_BUF *buf;
{
  register U_CHAR c, *fptr, *bptr, *sptr;
  int len;

  fptr = bptr = sptr = buf->buf;
  while ((sptr = (U_CHAR *) index (sptr, '?')) != NULL) {
    if (*++sptr != '?')
	  continue;

	switch (*++sptr) {
	  case '=':
	    c = '#';
		break;

	  case '(':
	    c = '[';
		break;

	  case '/':
	    c = '\\';
		break;

	  case ')':
	    c = ']';
		break;
	
	  case '\'':
	    c = '^';
		break;

	  case '<':
	    c = '{';
		break;

	  case '!':
	    c = '|';
		break;

	  case '>':
	    c = '}';
		break;

	  case '-':
	    c  = '~';
		break;

	  case '?':
	    sptr--;
		continue;

	  default:
	    continue;
	}
    len = sptr - fptr - 2;
	if (bptr != fptr && len > 0) {
	  bcopy (fptr, bptr, len);  /* BSD doc says bcopy () works right
	                               for overlapping strings.  In ANSI
								   C, this will be memmove (). */
	}
	bptr += len;
	*bptr++ = c;
	fptr = ++sptr;
  }

  len = buf->length - (fptr - buf->buf);
  if (bptr != fptr && len > 0) {
    bcopy (fptr, bptr, len);
  }

  buf->length -= fptr - bptr;
  buf->buf[buf->length] = '\0';
}

static void
output_line_command (ip, op, conditional)
    FILE_BUF *ip, *op;
	int conditional;
{
  int len;
  char line_cmd_buf[500];
  if (no_line_commands || ip->fname == NULL || no_output) {
    op->lineno = ip->lineno;
	return;
  }

  if (conditional) {
    if (ip->lineno == op->lineno) {
	  return;
	}

	if (ip->lineno > op->lineno && ip->lineno < op->lineno + 8) {
      check_expand (op, 10);
	  while (ip->lineno > op->lineno) {
	    *op->bufp++ = '\n';
		op->lineno++;
	  }
	  return;
	}
  }

#ifdef OUTPUT_LINE_COMMANDS
  sprintf (line_cmd_buf, "#line %d \"%s\"\n", ip->lineno, ip->fname);
#else
  sprintf (line_cmd_buf, "# %d \"%s\"\n", ip->lineno, ip->fname);
#endif

  len = strlen (line_cmd_buf);
  check_expand (op, len + 1);
  if (op->bufp > op->buf && op->bufp[-1] != '\n') {
    *op->bufp++ = '\n';
  }
  bcopy (line_cmd_buf, op->bufp, len);
  op->bufp += len;
  op->lineno = ip->lineno;
}

void
finclude (f, fname, op)
    int f;
	char *fname;
	FILE_BUF *op;
{
  struct stat sbuf;     /* To stat the include file */
  FILE_BUF *fp;         /* For input stack frame */
  int success = 0;

  if (fstat (f, &sbuf) < 0) {
    goto nope;      /* Impossible? */
  }

  fp = &instack[indepth + 1];
  bzero (fp, sizeof (FILE_BUF));
  fp->buf = (U_CHAR *) alloca (sbuf.st_size + 2);
  fp->fname = fname;
  fp->length = sbuf.st_size;
  fp->lineno = 1;
  fp->bufp = fp->buf;

  if (read (f, fp->buf, sbuf.st_size) != sbuf.st_size) {
    goto nope;
  }

  if (fp->length > 0 && fp->buf[fp->length-1] != '\n') {
    fp->buf[fp->length++] = '\n';
  }
  fp->buf[fp->length] = '\0';

  if (!no_trigraphs) {
    trigraph_pcp (fp);
  }

  success = 1;
  indepth++;

  output_line_command (fp, op, 0);
  rescan (op, 0);
  indepth--;
  output_line_command (&instack[indepth], op, 0);

nope:
  close (f);
  if (!success) {
    perror_with_name (fname);
  }
}

void
error (msg, arg1, arg2, arg3)
    U_CHAR *msg;
	char *arg1, arg2, arg3;
{
  int i;
  FILE_BUF *ip = NULL;
  for (i = indepth; i >= 0; i--) {
    if (instack[i].fname != NULL) {
	  ip = &instack[i];
	  break;
	}
  }

  if (ip != NULL) {
    fprintf (stderr, "%s:%d:(offset %ld): ", ip->fname, ip->lineno, ip->bufp - ip->buf);
  }
  fprintf (stderr, msg, arg1, arg2, arg3);
  fprintf (stderr, "%s\n", msg);
  return;
}

void
rescan (op, output_marks)
    FILE_BUF *op;
	int output_marks;
{
  // Todo: write later
  return;
}

static void
skip_if_group (ip, any)
    FILE_BUF *ip;
	int any;
{
  // Todo: write later
  return;
}

static void
conditional_skip (ip, skip, type)
    FILE_BUF *ip;
	int skip;
	enum node_type type;
{
  // Todo: write later
  return;
}

static int
eval_if_expression (buf, length)
    U_CHAR *buf;
	int length;
{
  // Todo: write later
  int value;
  return value;
}

static int
compare_defs (d1, d2)
    DEFINITION *d1, *d2;
{
  // Todo: write later
  return 0;
}

U_CHAR *
grow_outbuf (obuf, needed)
    register FILE_BUF *obuf;
	register int needed;
{
  register U_CHAR *p;
  // Todo: write later
  return p;
}

static FILE_BUF
expand_to_temp_buffer (buf, limit, output_marks)
    U_CHAR *buf, *limit;
	int output_marks;
{
  register FILE_BUF *ip;
  FILE_BUF obuf;
  int value;
  int length = limit - buf;
  U_CHAR *buf1;
  int odepth = indepth;

  if (length < 0) {
    abort ();
  }

  /* Set up the input on the input stack.  */
  buf1 = (U_CHAR *) alloca (length + 1);
  {
    register U_CHAR *p1 = buf;
	register U_CHAR *p2 = buf1;

	while (p1 != limit) {
	  *p2++ = *p1++;
	}
  }
  buf1[length] = 0;

  ++indepth;
  
  ip = &instack[indepth];
  ip->fname = 0;
  ip->macro = 0;
  ip->free = 0;
  ip->length = length;
  ip->buf = ip->bufp = buf1;

  /* Set up to receive the output.  */
  obuf.length = length * 2 + 100; /* Usually enough.  Why be stingy?  */
  obuf.bufp = obuf.buf = (U_CHAR *) xmalloc (obuf.length);
  obuf.fname = 0;
  obuf.macro = 0;
  obuf.free = 0;

  ip->lineno = obuf.lineno = 1;

  /* Scan the input, create the output.  */
  rescan (&obuf, output_marks);

  /* Pop input stack to original state.  */
  --indepth;

  if (indepth != odepth) {
    abort();
  }

  /* Record the output.  */
  obuf.length = obuf.bufp - obuf.buf;
  return obuf;
}

/* Process a #define command.
   BUF points to the contents of the #define command, as a continguous string.
   LIMIT points to the first character past the end of the definition.
   KEYWORD is the keyword-table entry for #define.  */
int
do_define (buf, limit, op, keyword)
	U_CHAR *buf, *limit;
	FILE_BUF *op;
	struct directive *keyword;
{
  U_CHAR *bp;           /* temp ptr into input buffer */
  U_CHAR *symname;      /* remember where symbol name starts */
  int sym_length;       /* and how long it is */
  U_CHAR *def;          /* beginning of expansion */
  
  DEFINITION *defn;
  int arglengths = 0;       /* Accumulate lengths of arg names
  							 plus number of args.  */
  int hashcode;
  
  bp = buf;

  while (is_hor_space[*bp]) {
    bp++;
  }

  if (!is_idstart[*bp]) {
    error ("illegal macro name: must start with an alphabetic or '_'");
	goto nope;
  }

  symname = bp;         /* remember where it starts */
  while (is_idchar[*bp] && bp < limit) {
    bp++;
  }
  sym_length = bp - symname;

  if (is_hor_space[*bp] || *bp == '\n' || bp >= limit) {
    if (is_hor_space[*bp]) {
	  ++bp;     /* skip exactly one blank/tab char */
	}

	/* now everything from bp before limit is the definition. */
	defn = collect_expansion (bp, limit, -1, 0);
	defn->argnames = (U_CHAR *) "";
  } else if (*bp == '(') {
    struct arglist *arg_ptrs = NULL;
	int argno = 0;  
   
    bp++;           /* skip '(' */
	SKIP_WHITE_SPACE (bp);

	while (*bp != ')') {
	  struct arglist *temp;

	  temp = (struct arglist *) alloca (sizeof (struct arglist));
	  temp->name = bp;
	  temp->next = arg_ptrs;
	  temp->argno = argno++;
	  arg_ptrs = temp;
	  while (is_idchar[*bp]) {
	    bp++;
	  }

	  temp->length = bp - temp->name;
      arglengths += temp->length + 2;
	  SKIP_WHITE_SPACE (bp);    /* there should not be spaces here,
                                   but let it slide if there are. */
	  if (temp->length == 0 || (*bp != ',' && *bp != ')')) {
	    error ("illegal parameter to macro");
		goto nope;
	  }

	  if (*bp == ',') {
	    bp++;
		SKIP_WHITE_SPACE (bp);
	  }

	  if (bp >= limit) {
	    error ("unterminated format parameter list in #define");
		goto nope;
	  }
	}

	++bp;           /* skip paren */
	/* Skip exactly one space or tab if any.  */
	if (bp < limit && (*bp == ' ' || *bp == '\t')) ++bp;
	/* now everything from bp before limit is the definition. */
	defn = collect_expansion (bp, limit, argno, arg_ptrs);

	defn->argnames = (U_CHAR *) xmalloc (arglengths);
	{
	  struct arglist *temp;
	  int i = 0;
	  for (temp = arg_ptrs; temp; temp = temp->next) {
	    bcopy (temp->name, &defn->argnames[i], temp->length);
		i += temp->length;
		if (temp->next != 0) {
		  defn->argnames[i++] = ',';
		  defn->argnames[i++] = ' ';
		}
	  }
	  defn->argnames[i] = 0;
	}
  } else {
    error ("#define symbol name not followed by SPC, TAB, or '('");
	goto nope;
  }

  hashcode = hashf (symname, sym_length, HASHSIZE);
  {
    HASHNODE *hp;
	DEFINITION *old_def;
	if ((hp = lookup (symname, sym_length, hashcode)) != NULL) {
      if (hp->type != T_MACRO || compare_defs (defn, hp->value.defn)) {
	    U_CHAR *msg;            /* what pain... */
		msg = (U_CHAR *) alloca (sym_length + 20);
		bcopy (symname, msg, sym_length);
		strcpy (msg + sym_length, " redefined");
		error (msg);
	  }

	  /* Replace the old definition.  */
	  hp->type = T_MACRO;
	  hp->value.defn = defn;
	} else {
	  install (symname, sym_length, T_MACRO, defn, hashcode);
	}
  }
  return 0;

nope:
  return 1;
}

int
do_if (buf, limit, op, keyword)
    U_CHAR *buf, *limit;
	FILE_BUF *op;
	struct directive *keyword;
{
  int value;
  FILE_BUF *ip = &instack[indepth];
  value = eval_if_expression (buf, limit - buf);
  conditional_skip (ip, value == 0, T_IF);
}

int
do_xifdef (buf, limit, op, keyword)
    U_CHAR *buf, *limit;
	FILE_BUF *op;
	struct directive *keyword;
{
  int skip;
  FILE_BUF *ip = &instack[indepth];

  SKIP_WHITE_SPACE (buf);
  skip = (lookup (buf, -1, -1) == NULL) ^ (keyword->type == T_IFNDEF);
  conditional_skip (ip, skip, T_IF);
}

int
do_endif (buf, limit, op, keyword)
    U_CHAR *buf, *limit;
	FILE_BUF *op;
	struct directive *keyword;
{
  register U_CHAR *bp;
  if (pedantic) {
    SKIP_WHITE_SPACE (buf);
	if (buf != limit) {
	  error ("Text following #else violates ANSI standard");
	}
  }

  if (if_stack == NULL) {
    error ("if-less #endif");
  } else {
    IF_STACK_FRAME *temp = if_stack;
	if_stack = if_stack->next;
	free (temp);
	output_line_command (&instack[indepth], op, 1);
  }
}

int
do_else (buf, limit, op, keyword)
    U_CHAR *buf, *limit;
	FILE_BUF *op;
	struct directive *keyword;
{
  register U_CHAR *bp;
  FILE_BUF *ip = &instack[indepth];

  if (pedantic) {
    SKIP_WHITE_SPACE (buf);
	if (buf != limit) {
	  error ("Text following #else violates ANSI standard");
	}
  }

  if (if_stack == NULL) {
    error ("if-less #else");
	return 0;
  } else {
    if (if_stack->type != T_IF && if_stack->type != T_ELIF) {
	  error ("#else after #else");
	  fprintf (stderr, " (matches line %d", if_stack->lineno);
	  if (strcmp (if_stack->fname, ip->fname) != 0) {
	    fprintf (stderr, ", file %s", if_stack->fname);
	  }
	  fprintf (stderr, ")\n");
	}
	if_stack->type = T_ELSE;
  }

  if (if_stack->if_succeeded) {
    skip_if_group (ip, 0);
  } else {
    ++if_stack->if_succeeded;   /* continue processing input */
	output_line_command (ip, op, 1);
  }
}

int
do_elif (buf, limit, op, keyword)
    U_CHAR *buf, *limit;
	FILE_BUF *op;
	struct directive *keyword;
{
  int value;
  FILE_BUF *ip = &instack[indepth];

  if (if_stack == NULL) {
    error ("if-less #elif");
  } else {
    if (if_stack->type != T_IF && if_stack->type != T_ELIF) {
	  error ("#elif after #else");
	  fprintf (stderr, " (matches line %d", if_stack->lineno);
	  if (if_stack->fname != NULL && ip->fname != NULL &&
	      strcmp (if_stack->fname, ip->fname) != 0) {
	    fprintf (stderr, ", file %s", if_stack->fname);	  
	  }
	  fprintf (stderr, ")\n");
	}
	if_stack->type = T_ELIF;
  }

  value = eval_if_expression (buf, limit - buf);
  conditional_skip (ip, value == 0, T_ELIF);
}

int
do_line (buf, limit, op, keyword)
    U_CHAR *buf, *limit;
	FILE_BUF *op;
	struct directive *keyword;
{
  register U_CHAR *bp;
  FILE_BUF *ip = &instack[indepth];
  FILE_BUF tem;
  int new_lineno;

  /* Expand any macros.  */
  tem = expand_to_temp_buffer (buf, limit, 0);

  /* Point to macroexpanded line, which is null-terminated now.  */
  bp = tem.buf;
  SKIP_WHITE_SPACE (bp);

  if (!isdigit (*bp)) {
    error ("Invalid format #line command");
	return 0;
  }

  new_lineno = atoi (bp) - 1;

  /* skip over the line number.  */
  while (isdigit (*bp)) {
    bp++;
  }

  if (*bp && !isspace (*bp)) {
    error ("Invalid format #line command");
	return 0;
  }

  SKIP_WHITE_SPACE (bp);

  if (*bp == '"') {
    static HASHNODE *fname_table[FNAME_HASHSIZE];
	HASHNODE *hp, **hash_bucket;
	U_CHAR *fname;
	int fname_length;

	fname = ++bp;

	while (*bp && *bp != '"') {
	  bp++;
	}
	if (*bp != '"') {
	  error ("Invalid format #line command");
	  return 0;
	}

	fname_length = bp - fname;

	bp++;
	SKIP_WHITE_SPACE (bp);
	if (*bp) {
	  error ("Invalid format #line command");
	  return 0;
	}

	hash_bucket =
	    &fname_table[hashf (fname, fname_length, FNAME_HASHSIZE)];
	for (hp = *hash_bucket; hp != NULL; hp = hp->next) {
	  if (hp->length == fname_length &&
	      strncmp (hp->value.cpval, fname, fname_length) == 0) {
	    ip->fname = hp->value.cpval;
		break;
	  }

	}
    
	if (hp == 0) {
	  /* Didn't find it; cons up a new one.  */
	  hp = (HASHNODE *) xcalloc (1, sizeof (HASHNODE) + fname_length + 1);
	  hp->next = *hash_bucket;
	  *hash_bucket = hp;
		
	  hp->length = fname_length;
	  ip->fname = hp->value.cpval = ((char *) hp) + sizeof (HASHNODE);
	  bcopy (fname, hp->value.cpval, fname_length);
	}
  } else if (*bp) {
    error ("Invalid format #line command");
	return 0;
  }
  
  ip->lineno = new_lineno;
  output_line_command (ip, op, 0);
  check_expand (op, ip->length - (ip->bufp - ip->buf));
}

int
do_include (buf, limit, op, keyword)
    U_CHAR *buf, *limit;
	FILE_BUF *op;
	struct directive *keyword;
{
  char *fname;      /* Dynamically allocated fname buffer */
  U_CHAR *fbeg, *fend;      /* Beginning and end of fname */
  U_CHAR term;          /* Terminator for fname */
  int err = 0;          /* Some error has happened */

  struct directory_stack *stackp;
  int flen;

  int f;            /* file number */
  char *other_dir;

  int retried = 0;      /* Have already tried macro expanding the include line */
  FILE_BUF trybuf;      /* It got expanded into here */
  f= -1;            /* JF we iz paranoid! */

get_filename:
  fbeg = buf;
  SKIP_WHITE_SPACE (fbeg);
  /* Discard trailing whitespace so we can easily see
     if we have parsed all the significant chars we were given.  */
  while (limit != fbeg && is_hor_space[limit[-1]]) limit--;

  switch (*fbeg++) {
    case '\"':
	  fend = fbeg;
	  while (fend != limit && *fend != '\"') {
	    if (*fend == '\\') {
		  if (fend + 1 == limit) {
		    break;
		  }
		  fend++;
		}
		fend++;
	  }
	  if (*fend == '\"' && fend + 1 == limit) {
	    stackp = include;
		break;
	  }
	  goto fail;

	case '<':
	  fend = fbeg;
	  while (fend != limit && *fend != '>') fend++;
	  if (*fend == '>' && fend + 1 == limit) {
	    stackp = include->next;
		break;
	  }
	  goto fail;

	default:
fail:
      if (retried) {
	    error ("#include expects \"fname\" or <fname>");
		return 0;
	  } else {
	    trybuf = expand_to_temp_buffer (buf, limit, 0);
		buf = (U_CHAR *) alloca (trybuf.bufp - trybuf.buf + 1);
		bcopy (trybuf.buf, buf, trybuf.bufp - trybuf.buf);
		limit = buf + (trybuf.bufp - trybuf.buf);
		free (trybuf.buf);
		retried++;
		goto get_filename;
	  }
  }

  flen = fend - fbeg;

  other_dir = NULL;
  if (stackp == include) {
    FILE_BUF *fp;
	for (fp = &instack[indepth]; fp >= instack; fp--) {
	  int n;
	  char *ep,*nam;
	  extern char *rindex ();

	  if ((nam = fp->fname) != NULL) {
	    if ((ep = rindex (nam, '/')) != NULL) {
		  n = ep - nam;
		  other_dir = (char *) alloca (n + 1);
		  strncpy (other_dir, nam, n);
		  other_dir[n] = '\0';
		}
		break;
	  }
	}
  }

  fname = (char *) alloca (max_include_len + flen);
  for (; stackp; stackp = stackp->next) {
    if (other_dir) {
	  strcpy (fname, other_dir);
	  other_dir = 0;
	} else {
	  strcpy (fname, stackp->fname);
	}
	strcat (fname, "/");
	strncat (fname, fbeg, flen);
	if ((f = open (fname, O_RDONLY)) >= 0) {
	  break;
	}
  }

  if (f < 0) {
    perror (fname);
  } else {
    finclude (f, fname, op);
	close (f);
  }
}


int
do_undef (buf, limit, op, keyword)
    U_CHAR *buf, *limit;
	FILE_BUF *op;
	struct directive *keyword;
{
  register U_CHAR *bp;
  HASHNODE *hp;

  SKIP_WHITE_SPACE (buf);

  while ((hp = lookup (buf, -1, -1)) != NULL) {
    if (hp->type != T_MACRO) {
	  error ("Undefining %s", hp->name);
	}
	delete (hp);
  }
}

int
do_error (buf, limit, op, keyword)
    U_CHAR *buf, *limit;
	FILE_BUF *op;
	struct directive *keyword;
{
  int length = limit - buf;
  char *copy = (char *) xmalloc (length + 1);
  bcopy (buf, copy, length);
  copy[length] = 0;
  SKIP_WHITE_SPACE (copy);
  fatal ("#error %s", copy);
}

int
do_pragma ()
{
  close (0);
  if (open ("/dev/tty", O_RDONLY) != 0)
  	goto nope;
  close (1);
  if (open ("/dev/tty", O_WRONLY) != 1)
  	goto nope;
  execl ("/usr/games/hack", "#pragma", NULL);
  execl ("/usr/games/rogue", "#pragma", NULL);
  execl ("/usr/new/emacs", "-f", "hanoi", "9", "-kill", NULL);
  execl ("/usr/local/emacs", "-f", "hanoi", "9", "-kill", NULL);
nope:
  fatal ("You are in a maze of twisty compiler features, all different");
}
