/* Emit RTL for the GNU C-Compiler expander.
   Copyright (C) 1987 Free Software Foundation, Inc.

This file is part of GNU CC.

GNU CC is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY.  No author or distributor
accepts responsibility to anyone for the consequences of using it
or for whether it serves any particular purpose or works at all,
unless he says so in writing.  Refer to the GNU CC General Public
License for full details.

Everyone is granted permission to copy, modify and redistribute
GNU CC, but only under the conditions described in the
GNU CC General Public License.   A copy of this license is
supposed to have been given to you along with GNU CC so you
can know your rights and responsibilities.  It should be in a
file named COPYING.  Among other things, the copyright notice
and this notice must be preserved on all copies.  */


/* Middle-to-low level generation of rtx code and insns.

   This file contains the functions `gen_rtx', `gen_reg_rtx'
   and `gen_label_rtx' that are the usual ways of creating rtl
   expressions for most purposes.

   It also has the functions for creating insns and linking
   them in the doubly-linked chain.

   The patterns of the insns are created by machine-dependent
   routines in insn-emit.c, which is generated automatically from
   the machine description.  These routines use `gen_rtx' to make
   the individual rtx's of the pattern; what is machine dependent
   is the kind of rtx's they make and what arguments they use.  */

#include "config.h"
#include <stdio.h>
#include <stdarg.h>
#include "rtl.h"
#include "regs.h"
#include "insn-config.h"
#include "recog.h"

#define max(A,B) ((A) > (B) ? (A) : (B))
#define min(A,B) ((A) < (B) ? (A) : (B))

/* This is reset to FIRST_PSEUDO_REGISTER at the start each function.
   After rtl generation, it is 1 plus the largest register number used.  */

int reg_rtx_no = FIRST_PSEUDO_REGISTER;

/* This is *not* reset after each function.  It gives each CODE_LABEL
   in the entire compilation a unique label number.  */

int label_no = 1;

/* Nonzero means do not generate NOTEs for source line numbers.  */

static int no_line_numbers;

/* Commonly used rtx's, so that we only need space for one copy.
   These are initialized at the start of each function.  */

rtx pc_rtx;			/* (PC) */
rtx cc0_rtx;			/* (CC0 */
rtx cc1_rtx;			/* (CC1) */
rtx const0_rtx;			/* (CONST_INT 0) */
rtx const1_rtx;			/* (CONST_INT 1) */
rtx fconst0_rtx;		/* (CONST_DOUBLE:SF 0) */
rtx dconst0_rtx;		/* (CONST_DOUBLE:DF 0) */

/* The ends of the doubly-linked chain of rtl for the current function.
   Both are reset to null at the start of rtl generation for the function.  */

static rtx first_insn = NULL;
static rtx last_insn = NULL;

/* INSN_UID for next insn emitted.
   Reset to 1 for each function compiled.  */

static int cur_insn_uid = 1;

/* Line number and source file of the last line-number NOTE emitted.
   This is used to avoid generating duplicates.  */

static int last_linenum = 0;
static char *last_filename = 0;

/* A vector indexed by pseudo reg number.  The allocated length
   of this vector is regno_pointer_flag_length.  Since this
   vector is needed during the expansion phase when the total
   number of registers in the function is not yet known,
   it is copied and made bigger when necessary.  */

char *regno_pointer_flag;
int regno_pointer_flag_length;

/* Indexed by pseudo register number, gives the rtx for that pseudo.
   Allocated in parallel with regno_pointer_flag.  */

rtx *regno_reg_rtx;

/* Chain of all CONST_DOUBLEs made for this function;
   so we can uniquize them.  */

rtx real_constant_chain;

/* rtx gen_rtx (code, mode, [element1, ..., elementn])
**
**	    This routine generates an RTX of the size specified by
**	<code>, which is an RTX code.   The RTX structure is initialized
**	from the arguments <element1> through <elementn>, which are
**	interpreted according to the specific RTX type's format.   The
**	special machine mode associated with the rtx (if any) is specified
**	in <mode>.
**
**	    gen_rtx() can be invoked in a way which resembles the lisp-like
**	rtx it will generate.   For example, the following rtx structure:
**
**	      (plus:QI (mem:QI (reg:SI 1))
**		       (mem:QI (plusw:SI (reg:SI 2) (reg:SI 3))))
**
**		...would be generated by the following C code:
**
**	    	gen_rtx (PLUS, QImode,
**		    gen_rtx (MEM, QImode,
**			gen_rtx (REG, SImode, 1)),
**		    gen_rtx (MEM, QImode,
**			gen_rtx (PLUS, SImode,
**			    gen_rtx (REG, SImode, 2),
**			    gen_rtx (REG, SImode, 3)))),
*/

/*VARARGS2*/
rtx gen_rtx(enum rtx_code code, enum machine_mode mode, ...)
{
  register int i;		/* Array indices...			*/
  register char *fmt;		/* Current rtx's format...		*/
  register rtx rt_val;		/* RTX to return to caller...		*/

  va_list p;
  va_start(p, 0);

  if (code == CONST_INT)
    {
	  int first = va_arg(p, int);
      if (first == 0)
	return const0_rtx;
      if (first == 1)
	return const1_rtx;
    }
  if (code == CONST_DOUBLE)
    {
	  double first = va_arg(p, double);
      if (first == XINT (fconst0_rtx, 0)
	  && (&first)[1] == XINT (fconst0_rtx, 1))
	return (mode == DFmode ? dconst0_rtx : fconst0_rtx);
    }

  rt_val = rtx_alloc (code);	/* Allocate the storage space.	*/
  rt_val->mode = mode;		/* Store the machine mode...	*/

  fmt = GET_RTX_FORMAT (code);	/* Find the right format...	*/
  for (i = 0; i < GET_RTX_LENGTH (code); i++)
    {
      switch (*fmt)
	{
	case '0':		/* Unused field...			*/
	  break;

	case 'i':		/* An integer?				*/
	  XINT (rt_val, i) = va_arg(p, int);
	  break;

	case 's':		/* A string?				*/
	  XSTR (rt_val, i) = va_arg(p, char *);
	  break;

	case 'e':		/* An expression?			*/
	case 'u':		/* An insn?  Same except when printing.  */
	  XEXP (rt_val, i) = va_arg(p, rtx);
	  break;

	case 'E':		/* An RTX vector?			*/
	  XVEC (rt_val, i) = va_arg(p, rtvec);
	  break;

	default:		/* Invalid format specification...	*/
	  abort();
	}			/* End switch */
    }				/* End for */

  va_end(p);
  return rt_val;		/* Return the new RTX...		*/
}

/* gen_rtvec (n, [rt1, ..., rtn])
**
**	    This routine creates an rtvec and stores within it the
**	pointers to rtx's which are its arguments.
*/

/*VARARGS1*/
rtvec
gen_rtvec (n, first)
     int n;
     rtx first;
{
  if (n == 0)
    return NULL_RTVEC;		/* Don't allocate an empty rtvec...	*/

  return gen_rtvec_v (n, &first);
}

rtvec
gen_rtvec_v (n, argp)
     int n;
     rtx *argp;
{
  register int i;
  register rtvec rt_val;

  if (n == 0)
    return NULL_RTVEC;		/* Don't allocate an empty rtvec...	*/

  rt_val = rtvec_alloc (n);	/* Allocate an rtvec...			*/

  for (i = 0; i < n; i++)
    rt_val->elem[i].rtx = *argp++;

  return rt_val;
}

/* Generate a REG rtx for a new pseudo register of mode MODE.
   This pseudo is assigned the next sequential register number.  */

rtx
gen_reg_rtx (mode)
     enum machine_mode mode;
{
  register rtx val;

  /* Make sure regno_pointer_flag and regno_reg_rtx are large
     enough to have an element for this pseudo reg number.  */

  if (reg_rtx_no == regno_pointer_flag_length)
    {
      rtx *new1;
      char *new =
	(char *) oballoc (regno_pointer_flag_length * 2);
      bzero (new, regno_pointer_flag_length * 2);
      bcopy (regno_pointer_flag, new, regno_pointer_flag_length);
      regno_pointer_flag = new;

      new1 = (rtx *) oballoc (regno_pointer_flag_length * 2 * sizeof (rtx));
      bzero (new1, regno_pointer_flag_length * 2 * sizeof (rtx));
      bcopy (regno_reg_rtx, new1, regno_pointer_flag_length * sizeof (rtx));
      regno_reg_rtx = new1;

      regno_pointer_flag_length *= 2;
    }

  val = gen_rtx (REG, mode, reg_rtx_no);
  regno_reg_rtx[reg_rtx_no++] = val;
  return val;
}

/* Identify REG as a probable pointer register.  */

void
mark_reg_pointer (reg)
     rtx reg;
{
  REGNO_POINTER_FLAG (REGNO (reg)) = 1;
}

/* Return 1 plus largest pseudo reg number used in the current function.  */

int
max_reg_num ()
{
  return reg_rtx_no;
}

/* Assuming that X is an rtx (MEM or REG) for a fixed-point number,
   return a MEM or SUBREG rtx that refers to the least-significant part of X.
   If X is a MEM whose address is a QUEUED, the value may be so also.  */

rtx
gen_lowpart (mode, x)
     enum machine_mode mode;
     register rtx x;
{
  if (GET_CODE (x) == SUBREG)
    {
      /* The code we have is correct only under these conditions.  */
      if (! subreg_lowpart_p (x))
	abort ();
      if (GET_MODE_SIZE (mode) > UNITS_PER_WORD)
	abort ();
      return (GET_MODE (SUBREG_REG (x)) == mode
	      ? SUBREG_REG (x)
	      : gen_rtx (SUBREG, mode, SUBREG_REG (x), SUBREG_WORD (x)));
    }
  if (GET_MODE (x) == mode)
    return x;
  if (GET_CODE (x) == CONST_INT)
    return gen_rtx (CONST_INT, VOIDmode, INTVAL (x) & GET_MODE_MASK (mode));
  if (GET_CODE (x) == MEM)
    {
      register int offset = 0;
#ifdef WORDS_BIG_ENDIAN
      offset = (max (GET_MODE_SIZE (GET_MODE (x)), UNITS_PER_WORD)
		- max (GET_MODE_SIZE (mode), UNITS_PER_WORD));
#endif
#ifdef BYTES_BIG_ENDIAN
      /* Adjust the address so that the address-after-the-data
	 is unchanged.  */
      offset -= (min (UNITS_PER_WORD, GET_MODE_SIZE (mode))
		 - min (UNITS_PER_WORD, GET_MODE_SIZE (GET_MODE (x))));
#endif
      return gen_rtx (MEM, mode,
		      memory_address (mode,
				      plus_constant (XEXP (x, 0),
						     offset)));
    }
  else if (GET_CODE (x) == REG)
    {
#ifdef WORDS_BIG_ENDIAN
      if (GET_MODE_SIZE (GET_MODE (x)) > UNITS_PER_WORD)
	{
	  return get_rtx (SUBREG, mode, x,
			  ((GET_MODE_SIZE (GET_MODE (x))
			    - max (GET_MODE_SIZE (mode), UNITS_PER_WORD))
			   / UNITS_PER_WORD));
	}
#endif
      return gen_rtx (SUBREG, mode, x, 0);
    }
  else
    abort ();
}

/* Like `gen_lowpart', but refer to the most significant part.  */

rtx
gen_highpart (mode, x)
     enum machine_mode mode;
     register rtx x;
{
  if (GET_CODE (x) == MEM)
    {
      register int offset = 0;
#ifndef WORDS_BIG_ENDIAN
      offset = (max (GET_MODE_SIZE (GET_MODE (x)), UNITS_PER_WORD)
		- max (GET_MODE_SIZE (mode), UNITS_PER_WORD));
#endif
#ifndef BYTES_BIG_ENDIAN
      if (GET_MODE_SIZE (mode) < UNITS_PER_WORD)
	offset -= (GET_MODE_SIZE (mode)
		   - min (UNITS_PER_WORD,
			  GET_MODE_SIZE (GET_MODE (x))));
#endif
      return gen_rtx (MEM, mode,
		      memory_address (mode,
				      plus_constant (XEXP (x, 0),
						     offset)));
    }
  else if (GET_CODE (x) == REG)
    {
#ifndef WORDS_BIG_ENDIAN
      if (GET_MODE_SIZE (GET_MODE (x)) > UNITS_PER_WORD)
	{
	  return gen_rtx (SUBREG, mode, x,
			  ((GET_MODE_SIZE (GET_MODE (x))
			    - max (GET_MODE_SIZE (mode), UNITS_PER_WORD))
			   / UNITS_PER_WORD));
	}
#endif
      return gen_rtx (SUBREG, mode, x, 0);
    }
  else
    abort ();
}

/* Return 1 iff X, assumed to be a SUBREG,
   refers to the least significant part of its containing reg.
   If X is not a SUBREG, always return 1 (it is its own low part!).  */

int
subreg_lowpart_p (x)
     rtx x;
{
  if (GET_CODE (x) != SUBREG)
    return 1;
#ifdef WORDS_BIG_ENDIAN
  if (GET_MODE_SIZE (GET_MODE (x)) > UNITS_PER_WORD)
    {
      register enum machine_mode mode = GET_MODE (SUBREG_REG (x));
      return (SUBREG_WORD (x)
	      == ((GET_MODE_SIZE (GET_MODE (x))
		   - max (GET_MODE_SIZE (mode), UNITS_PER_WORD))
		  / UNITS_PER_WORD));
    }
#endif 
  return SUBREG_WORD (x) == 0;
}

/* Return a newly created CODE_LABEL rtx with a unique label number.  */

rtx
gen_label_rtx ()
{
  register rtx label = gen_rtx (CODE_LABEL, VOIDmode, 0, 0, 0, label_no++);
  LABEL_NUSES (label) = 0;
  return label;
}

/* Emission of insns (adding them to the doubly-linked list).  */

/* Return the first insn of the current function.  */

rtx
get_insns ()
{
  return first_insn;
}

/* Return the last insn of the current function.  */

rtx
get_last_insn ()
{
  return last_insn;
}

/* Make and return an INSN rtx, initializing all its slots.
   Store PATTERN in the pattern slots.
   PAT_FORMALS is an idea that never really went anywhere.  */

static rtx
make_insn_raw (pattern, pat_formals)
     rtx pattern;
     rtvec pat_formals;
{
  register rtx insn;

  insn = rtx_alloc(INSN);
  INSN_UID(insn) = cur_insn_uid++;

  PATTERN (insn) = pattern;
  INSN_CODE (insn) = -1;
  LOG_LINKS(insn) = NULL;
  REG_NOTES(insn) = NULL;

  return insn;
}

/* Like `make_insn' but make a JUMP_INSN instead of an insn.  */

static rtx
make_jump_insn_raw (pattern, pat_formals)
     rtx pattern;
     rtvec pat_formals;
{
  register rtx insn;

  insn = rtx_alloc(JUMP_INSN);
  INSN_UID(insn) = cur_insn_uid++;

  PATTERN (insn) = pattern;
  INSN_CODE (insn) = -1;
  LOG_LINKS(insn) = NULL;
  REG_NOTES(insn) = NULL;
  JUMP_LABEL(insn) = NULL;

  return insn;
}

/* Add INSN to the end of the doubly-linked list.
   INSN may be an INSN, JUMP_INSN, CALL_INSN, CODE_LABEL, BARRIER or NOTE.  */

static void
add_insn (insn)
     register rtx insn;
{
  PREV_INSN (insn) = last_insn;
  NEXT_INSN (insn) = 0;

  if (NULL != last_insn)
    NEXT_INSN (last_insn) = insn;

  if (NULL == first_insn)
    first_insn = insn;

  last_insn = insn;
}

/* Add INSN, an rtx of code INSN, into the doubly-linked list
   after insn AFTER.  */

static void
add_insn_after (insn, after)
     rtx insn, after;
{
  NEXT_INSN (insn) = NEXT_INSN (after);
  PREV_INSN (insn) = after;

  if (NEXT_INSN (insn))
    PREV_INSN (NEXT_INSN (insn)) = insn;
  else
    last_insn = insn;
  NEXT_INSN (after) = insn;
}

/* Emit an insn of given code and pattern
   at a specified place within the doubly-linked list.  */

/* Make an instruction with body PATTERN
   and output it before the instruction BEFORE.  */

rtx
emit_insn_before (pattern, before)
     register rtx pattern, before;
{
  register rtx insn = make_insn_raw (pattern, 0);

  PREV_INSN (insn) = PREV_INSN (before);
  NEXT_INSN (insn) = before;

  if (PREV_INSN (insn))
    NEXT_INSN (PREV_INSN (insn)) = insn;
  else
    first_insn = insn;
  PREV_INSN (before) = insn;

  return insn;
}

/* Make an instruction with body PATTERN and code JUMP_INSN
   and output it before the instruction BEFORE.  */

rtx
emit_jump_insn_before (pattern, before)
     register rtx pattern, before;
{
  register rtx insn = make_jump_insn_raw (pattern, 0);

  PREV_INSN (insn) = PREV_INSN (before);
  NEXT_INSN (insn) = before;

  if (PREV_INSN (insn))
    NEXT_INSN (PREV_INSN (insn)) = insn;
  else
    first_insn = insn;
  PREV_INSN (before) = insn;

  return insn;
}

/* Make an insn of code INSN with body PATTERN
   and output it after the insn AFTER.  */

rtx
emit_insn_after (pattern, after)
     register rtx pattern, after;
{
  register rtx insn = make_insn_raw (pattern, 0);

  add_insn_after (insn, after);
  return insn;
}

/* Make an insn of code BARRIER
   and output it after the insn AFTER.  */

rtx
emit_barrier_after (after)
     register rtx after;
{
  register rtx insn = rtx_alloc (BARRIER);

  INSN_UID (insn) = cur_insn_uid++;

  add_insn_after (insn, after);
  return insn;
}

/* Emit the label LABEL after the insn AFTER.  */

void
emit_label_after (label, after)
     rtx label, after;
{
  INSN_UID (label) = cur_insn_uid++;
  add_insn_after (label, after);
}

/* Emit an insn of given code and pattern
   at the end of the doubly-linked list.  */

/* Make an insn of code INSN with pattern PATTERN
   and add it to the end of the doubly-linked list.  */

rtx
emit_insn (pattern)
     rtx pattern;
{
  register rtx insn = make_insn_raw (pattern, NULL);
  add_insn (insn);
  return insn;
}

/* Make an insn of code JUMP_INSN with pattern PATTERN
   and add it to the end of the doubly-linked list.  */

rtx
emit_jump_insn (pattern)
     rtx pattern;
{
  register rtx insn = make_jump_insn_raw (pattern, NULL);
  add_insn (insn);
  return insn;
}

/* Make an insn of code CALL_INSN with pattern PATTERN
   and add it to the end of the doubly-linked list.  */

rtx
emit_call_insn (pattern)
     rtx pattern;
{
  register rtx insn = make_insn_raw (pattern, NULL);
  add_insn (insn);
  PUT_CODE (insn, CALL_INSN);
  return insn;
}

/* Add the label LABEL to the end of the doubly-linked list.  */

void
emit_label (label)
     rtx label;
{
  INSN_UID (label) = cur_insn_uid++;
  add_insn (label);
}

/* Make an insn of code BARRIER
   and add it to the end of the doubly-linked list.  */

void
emit_barrier ()
{
  register rtx barrier = rtx_alloc (BARRIER);
  INSN_UID (barrier) = cur_insn_uid++;
  add_insn (barrier);
}

/* Make an insn of code NOTE
   with data-fields specified by FILE and LINE
   and add it to the end of the doubly-linked list.  */

void
emit_note (file, line)
     char *file;
     int line;
{
  register rtx linenum;

  if (no_line_numbers && line > 0)
    return;

  if (line > 0)
    {
      if (file && last_filename && !strcmp (file, last_filename)
	  && line == last_linenum)
	return;
      last_filename = file;
      last_linenum = line;
    }

  linenum = rtx_alloc (NOTE);
  INSN_UID (linenum) = cur_insn_uid++;
  XSTR (linenum, 3) = file;
  XINT (linenum, 4) = line;
  add_insn (linenum);
}

/* Initialize data structures and variables in this file
   before generating rtl for each function.
   WRITE_SYMBOLS is nonzero if any kind of debugging info
   is to be generated.  */

void
init_emit (write_symbols)
     int write_symbols;
{
  first_insn = NULL;
  last_insn = NULL;
  cur_insn_uid = 1;
  reg_rtx_no = FIRST_PSEUDO_REGISTER;
  last_linenum = 0;
  last_filename = 0;
  real_constant_chain = 0;

  no_line_numbers = ! write_symbols;

  /* Create the unique rtx's for certain rtx codes and operand values.  */

  pc_rtx = gen_rtx (PC, VOIDmode);
  cc0_rtx = gen_rtx (CC0, VOIDmode);

  /* Don't use gen_rtx here since gen_rtx in this case
     tries to use these variables.  */
  const0_rtx = rtx_alloc (CONST_INT);
  INTVAL (const0_rtx) = 0;
  const1_rtx = rtx_alloc (CONST_INT);
  INTVAL (const1_rtx) = 1;

  fconst0_rtx = rtx_alloc (CONST_DOUBLE);
  {
    union { double d; int i[2]; } u;
    u.d = 0;
    XINT (fconst0_rtx, 0) = u.i[0];
    XINT (fconst0_rtx, 1) = u.i[1];
  }
  PUT_MODE (fconst0_rtx, SFmode);

  dconst0_rtx = rtx_alloc (CONST_DOUBLE);
  {
    union { double d; int i[2]; } u;
    u.d = 0;
    XINT (dconst0_rtx, 0) = u.i[0];
    XINT (dconst0_rtx, 1) = u.i[1];
  }
  PUT_MODE (dconst0_rtx, DFmode);

  /* Init the tables that describe all the pseudo regs.  */

  regno_pointer_flag_length = 100;

  regno_pointer_flag 
    = (char *) oballoc (regno_pointer_flag_length);
  bzero (regno_pointer_flag, regno_pointer_flag_length);

  regno_reg_rtx 
    = (rtx *) oballoc (regno_pointer_flag_length * sizeof (rtx));
  bzero (regno_reg_rtx, regno_pointer_flag_length * sizeof (rtx));
}
