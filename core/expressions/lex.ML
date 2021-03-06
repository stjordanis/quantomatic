(* A very basic lexer *)

signature SIMPLE_LEXER =
sig
  type elem;
  datatype token = INT of int
                 | ID  of string
                 | LIT of string
                 | SYM of string
                 | NOOP;
  exception MoreTokens of elem list;

  (* Parses out an identifier (ID): a series of letters, digits or
   * quasi-letters, starting with a letter or backslash
   *)
  val ident : elem list -> token * elem list;
  (* Produces a NOOP token *)
  val noop : elem list -> token * elem list;
  (* Parses out a positive integer number (INT) *)
  val num : elem list -> token * elem list;
  (* Discards any number of whitespace characters (token will be NOOP) *)
  val space : elem list -> token * elem list;
  (* Parses a literal: a string surrounded in double-quotes, which may contain
     C-style escaped characters *)
  val string_literal : elem list -> token * elem list;

  (* Parses out the specified text *)
  val sym : elem -> elem list -> token * elem list;
  (* Parse a string using the specified parser; if this fails to parse the whole
     string, MoreTokens will be thrown *)
  val parse_with : (elem list -> 'a * elem list) -> string -> 'a;
end;

structure SimpleLexer : SIMPLE_LEXER =
struct
  type elem = Symbol.symbol;
  datatype token = INT of int | ID of string | LIT of string | SYM of string | NOOP;
  exception MoreTokens of elem list;
  val wordchar = fn c => (Symbol.is_quasi_letter c) orelse (Symbol.is_digit c);

  (* terminal definitions *)
  val space = Scan.many Symbol.is_blank >> (K NOOP);
  fun wrap_term t = space |-- t --| space
  val num = wrap_term (Scan.many1
         Symbol.is_digit
         >> implode
         >> (the o Int.fromString)
         >> INT);

  val ident = wrap_term ((
        (Scan.one Symbol.is_letter || Scan.this_string "\\")
         ::: Scan.many wordchar)
           >> implode
           >> ID);
  val sym = fn str => (wrap_term (Scan.this_string str >> (K (SYM str))));
  val noop = fn toks => (NOOP, toks);

  (* string literal code stolen from isabelle/ml_parse.ML *)
  val scan_escape =
  Scan.one (member (op =) (raw_explode "\"\\abtnvfr")) ||
   $$ "^" ^^ Scan.one (fn s => ord "@" <= ord s andalso ord s <= ord "_") ||
   Scan.one Symbol.is_ascii_digit ^^
   Scan.one Symbol.is_ascii_digit ^^
   Scan.one Symbol.is_ascii_digit;

  val scan_str = Scan.one
         (fn s => Symbol.is_printable s andalso
          s <> "\"" andalso s <> "\\") ||
         ($$ "\\") |-- scan_escape;

  (*val scan_gap = ($$ "\\") ^^ scan_blanks1 ^^ ($$ "\\");
  val scan_gaps = Scan.repeat scan_gap >> implode;*)

  val string_literal = wrap_term ((
          ($$ "\"")
          |-- (Scan.repeat scan_str >> implode)
          --| ($$ "\"")) >> LIT);    

  fun parse_with p str = 
      let val (thing, toks) = p ((Symbol.explode str)@[Symbol.eof])
      in if toks = [Symbol.eof] then thing else raise MoreTokens toks
      end;
end; (* struct: SimpleLexer *)

(* vim:et:sts=2:ts=2:sw=2
*)
