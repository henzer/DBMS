grammar DDLGrammar;

fragment LETTER: ( 'a'..'z' | 'A'..'Z') ;
fragment DIGIT: '0'..'9' ;

NULL: 'NULL';
ASC: 'ASC';
DESC: 'DESC';
NUM: ('-')?  DIGIT(DIGIT)*;
DATE: DIGIT DIGIT DIGIT DIGIT '-' DIGIT DIGIT '-' DIGIT DIGIT;
FLOAT:  NUM '.' (DIGIT)*;
ID : LETTER (LETTER | DIGIT)* ;
TABLEID : ID'.'ID;



COMMENTS: '//' ~('\r' | '\n' )*  -> channel(HIDDEN);
WS: [ \t\r\n\f]+  -> channel(HIDDEN);

CHAR: '\''~('\r'|'\n'|'\'')* '\'';

root:
	(statement) (';' statement)* ';'
	;

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
	| dmlstatement
	;
	
createDatabase
	: 'CREATE' 'DATABASE' ID 
	;

alterDatabase
	: 'ALTER' 'DATABASE' ID 'RENAME' 'TO' ID
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
	: 'CREATE' 'TABLE' ID '(' (ID tipo (',' ID tipo)*)? (','constraintDecl)* ')'
	;

alterTable
	: 'ALTER' 'TABLE' ID 'RENAME' 'TO' ID		#alterTableRename
	| 'ALTER' 'TABLE' ID accion (','accion)*	#alterTableAccion
	;
	
dropTable
	: 'DROP' 'TABLE' ID
	;
	
showTables
	: 'SHOW' 'TABLES'
	;
	
showColumnsFrom
	: 'SHOW' 'COLUMNS' 'FROM' ID
	;

accion
	: 'ADD' 'COLUMN' ID tipo (','constraintDecl)*					#accion1
	| 'ADD' constraintDecl											#accion2
	| 'DROP' 'COLUMN' ID											#accion3
	| 'DROP' 'CONSTRAINT' ID										#accion4
	;
	
	
constraintDecl
	: 'CONSTRAINT' ID 'PRIMARY' 'KEY' '(' (ID  (',' ID )*)? ')'												#constraintDecl1
	| 'CONSTRAINT' ID 'FOREIGN' 'KEY' '(' (ID  (',' ID )*)? ')' 'REFERENCES' ID '(' (ID  (',' ID )*)? ')' 	#constraintDecl2
	| 'CONSTRAINT' ID 'CHECK' '(' expression ')' 															#constraintDecl3
	;
	

	
tipo
	: 'INT'					#tipoInt
	| 'FLOAT'				#tipoFloat
	| 'CHAR' '(' (NUM) ')'	#tipoChar
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
	:	FLOAT
	;
	
date_literal
	:	DATE
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
	| (ID|TABLEID)   			#factorID
	| NULL						#factorNull
	;
	

dmlstatement
	:	insert (';' insert)*				#dmlInsert
	|	(update)*				#dmlUpdate
	|	(delete)*				#dmlDelete
	|	(select)*				#dmlSelect
	;

insert
	: 'INSERT' 'INTO' ID ( '(' ((ID)(','ID)*)? ')' )? 'VALUES' '('(literal (',' literal)* )?')'
	;
	
update
	: 'UPDATE' ID 'SET' ID '=' literal (','ID '=' literal)* ('WHERE' expression)?
	;

delete
	: 'DELETE' 'FROM' ID ('WHERE' expression)?
	;

select
	: 'SELECT' part_select  'FROM' from  ('WHERE' where)? ('ORDER' 'BY' order_by)?
	;
	
part_select:
	('*'|(TABLEID|ID) (',' (TABLEID|ID))*)
	;

from:
	ID (',' ID)*
	;
where:
	expression
	;
	
order_by:
	criterion (','criterion)*
	;
	
criterion:
	(TABLEID|ID) (ASC|DESC)
	;

