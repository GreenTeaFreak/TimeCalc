grammar Time;

root: expr EOF;

expr: brkexpr
    | expr operation
    | expr (TIMES|DIVIDE) DIGIT+
    | timeval
    ;

brkexpr: LB expr RB;

operation: (PLUS | MINUS ) expr;

timeval: hour
    | minute
    | second
    | hms
    ;

hour: DIGIT+ SFX_HOUR;
minute: DIGIT+ SFX_MINUTE;
second: DIGIT+  SFX_SECOND;
hms: DIGIT+ COLON DIGIT+ (DOT DIGIT+)?;

WS: [\n\r\t ] -> channel(HIDDEN);

RB: ')';
LB: '(';
DIGIT: [0-9]+;
COLON: ':';
DOT: '.';
PLUS: '+';
MINUS: '-';
TIMES: '*';
DIVIDE: '/';
SFX_HOUR: 'h';
SFX_MINUTE: 'm';
SFX_SECOND: 's';
INTERVALCMD: 'int';