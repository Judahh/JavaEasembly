;STACK
    ;DSEG    AT  03fh
STACK_START:
    DS  040h

;RESET VECTORS
	;CSEG    AT  0h
    SJMP    XRESET  ;Reset vector
    ORG 100h

XRESET:
    MOV SP,#STACK_START  ;Define Stack pointer
	MOV P0,#0ffh  ;All pins are configured as inputs
	MOV P1,#0ffh  ;All pins are configured as inputs

LOOP:
	CPL P0.0        ;Pin P0.0 state is inverted
	CPL P0.1        ;Pin P0.1 state is inverted
	CPL P0.2        ;Pin P0.2 state is inverted
	CPL P0.3        ;Pin P0.3 state is inverted
	CPL P0.4        ;Pin P0.4 state is inverted
	CPL P0.5        ;Pin P0.5 state is inverted
	CPL P0.6        ;Pin P0.6 state is inverted
	CPL P0.7        ;Pin P0.7 state is inverted
	XRL P1, #0ffh
    LCALL   Delay   ;Time delay
	SJMP    LOOP

Delay:
    MOV     R2,#20  ;500 ms time delay
F02:
    MOV     R1,#50  ;25 ms
F01:
    MOV     R0,#230
    DJNZ    R0,$
    DJNZ    R1,F01
    DJNZ    R2,F02
END                 ;End of program