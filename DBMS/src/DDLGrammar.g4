grammar DDLGrammar;

fragment LETTER: ( 'a'..'z' | 'A'..'Z') ;
fragment DIGIT: '0'..'9' ;

NUM: DIGIT(DIGIT)* ;
ID : LETTER (LETTER | DIGIT)* ;
COMMENTS: '//' ~('\r' | '\n' )*  -> channel(HIDDEN);
WS: [ \t\r\n\f]+  -> channel(HIDDEN);

CHAR: '\'' (LETTER|DIGIT|' '| '!' | '"' | '#' | '$' | '%' | '&' | '(' | ')' | '*' | '+' 
| ',' | '-' | '.' | '/' | ':' | ';' | '<' | '=' | '>' | '?' | '@' | '[' | '\\' | ']' | '^' | '_' | '`'| '{' | '|' | '}' | '~' 
'\\t'| '\\n' | '\"' | '\'' | '\n')* '\'';

statement
	: createDatabase
	| alterDatabase
	| dropDatabase
	| showDatabases
	| useDatabase
	| createTable
	| alterTable
	| dropTable
	| showTables
	| showColumnsFrom
	;
	
createDatabase
	: 'CREATE' 'DATABASE' ID
	;

alterDatabase
	: 'ALTER' 'DATABASE' ID 'TO' ID
	;

dropDatabase
	: 'DROP' 'DATABASE' ID
	;

showDatabases
	: 'SHOW' 'DATABASES'
	;

useDatabase
	: 'USE' 'DATABASE' ID
	;

createTable
	: 'CREATE' 'TABLE' ID '(' (ID tipo (',' ID tipo)*)?  (constraintDecl(','constraintDecl)*)? ')'
	|
	;

alterTable
	: 'ALTER' 'TABLE' ID 'RENAME' 'TO' ID
	| 'ALTER' 'TABLE' ID accion (','accion)*
	;
	
dropTable
	: 'DROP' 'TABLE' ID
	;
	
showTables
	: 'SHOW' 'TABLES' ID
	;
	
showColumnsFrom
	: 'SHOW' 'COLUMNS' 'FROM' ID
	;

accion
	: 'ADD' 'COLUMN' ID ID (constraintDecl(','constraintDecl)*)?
	| 'ADD' constraintDecl
	| 'DROP' 'COLUMN' ID
	| 'DROP' 'CONSTRAINT' ID
	;
	
	
constraintDecl
	: 'CONSTRAINT' ID 'PRIMARY' 'KEY' '(' (ID  (',' ID )*)? ')'
	| 'CONSTRAINT' ID 'FOREIGN' 'KEY' '(' (ID  (',' ID )*)? ')' 'REFERENCES' ID '(' (ID  (',' ID )*)? ')' 
	| 'CONSTRAINT' ID 'CHECK' '(' expression ')' 
	;
	

	
tipo
	: 'INT'
	| 'FLOAT'
	| 'CHAR' '(' NUM ')'
	| 'DATE' 
	;

literal
	:	int_literal										//check
	|	char_literal									//check
	|	bool_literal									//check
	;
	
int_literal
	:	NUM						//check
	;

char_literal
	:	CHAR					//check
	;
	
bool_literal
	:	'true'					//check				
	|	'false'					//check				
	;
	

rel_op
	:	'<'												#relL
	|	'>'												#rekB
	| 	'<='											#relLE
	|	'>='											#relBE
	;
	
eq_op
	:	'=='											#eqE
	|	'!='											#eqNE	
	;
	
cond_op1
	:	'&&'
	;
	
cond_op2
	:	'||'
	;	

expression							//check
	: expression cond_op2 expr1		#expression1
	| expr1							#expression2
	;
	
expr1								//check
	: expr1 cond_op1 expr2		#expr11
	|expr2						#expr12
	;
	
expr2								//check
	: expr2 eq_op expr3			#expr21
	| expr3						#expr22
	;

expr3								//check
	: expr3 rel_op unifactor			#expr31
	| unifactor						#expr32
	;

unifactor
	: 'NOT' factor
	| factor
	;
	
factor 							//check
	: literal					#factorLiteral
	| '(' expression ')'		#factorExpression
	;