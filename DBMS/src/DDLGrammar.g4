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
	: 'ALTER' 'TABLE' ID 'RENAME' 'TO' ID		#alterTableRename
	| 'ALTER' 'TABLE' ID accion (','accion)*	#alterTableAccion
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
	: 'ADD' 'COLUMN' ID ID (constraintDecl(','constraintDecl)*)?	#accion1
	| 'ADD' constraintDecl											#accion2
	| 'DROP' 'COLUMN' ID											#accion3
	| 'DROP' 'CONSTRAINT' ID										#accion4
	;
	
	
constraintDecl
	: 'CONSTRAINT' ID 'PRIMARY' 'KEY' '(' (ID  (',' ID )*)? ')'												#constraintDecl1
	| 'CONSTRAINT' ID 'FOREIGN' 'KEY' '(' (ID  (',' ID )*)? ')' 'REFERENCES' ID '(' (ID  (',' ID )*)? ')' 	#constraintDecl2
	| 'CONSTRAINT' ID 'CHECK' '(' expression ')' 															#constraingDecl3
	;
	

	
tipo
	: 'INT'					#tipoInt
	| 'FLOAT'				#tipoFloat
	| 'CHAR' '(' NUM ')'	#tipoChar
	| 'DATE' 				#tipoDate
	;

literal
	:	int_literal										#literalInt
	|	char_literal									#literalChar
	|	date_literal									#literalDate
	|	float_literal									#literalFloat
	;
	
int_literal
	:	NUM						
	;

char_literal
	:	CHAR					
	;
	
float_literal
	:	NUM '.' NUM
	;
	
date_literal
	:	DIGIT DIGIT DIGIT DIGIT '-' DIGIT DIGIT '-' DIGIT DIGIT
	;

rel_op
	:	'<'												#relL
	|	'>'												#rekB
	| 	'<='											#relLE
	|	'>='											#relBE
	;
	
eq_op
	:	'='											#eqE
	|	'<>'										#eqNE	
	;
	
cond_op1
	:	'AND'
	;
	
cond_op2
	:	'OR'
	;	

expression							
	: expression cond_op2 expr1		#expression1
	| expr1							#expression2
	;
	
expr1								
	: expr1 cond_op1 expr2		#expr11
	|expr2						#expr12
	;
	
expr2								
	: expr2 eq_op expr3			#expr21
	| expr3						#expr22
	;

expr3								
	: expr3 rel_op unifactor			#expr31
	| unifactor							#expr32
	;

unifactor
	: 'NOT' factor						#uniFactorNot
	| factor							#uniFactorFactor
	;
	
factor 							
	: literal					#factorLiteral
	| '(' expression ')'		#factorExpression
	;