--------------------------- MODULE HaltBudget ---------------------------
EXTENDS Naturals
CONSTANTS GlobalDeadline, ImmediateHalt
VARIABLES time, halting, localA, bookA, localB
vars == <<time,halting,localA,bookA,localB>>
Init == /\ time=0 /\ halting=FALSE /\ localA=TRUE /\ bookA=TRUE /\ localB=FALSE
(* scaled timeline: client release times out at 4; checkpoint-before-halt
   may block for a further 3; old GCM reclaims at 5, corrected budget at 9. *)
Tick == /\ time<10 /\ (time#4 \/ halting) /\ (time#7 \/ ~localA)
        /\ time'=time+1 /\ UNCHANGED <<halting,localA,bookA,localB>>
BeginHalt == /\ time=4 /\ ~halting /\ halting'=TRUE
             /\ localA'=IF ImmediateHalt THEN FALSE ELSE localA
             /\ UNCHANGED <<time,bookA,localB>>
Halt == /\ time>=7 /\ halting /\ localA /\ localA'=FALSE
        /\ UNCHANGED <<time,halting,bookA,localB>>
Reclaim == /\ time>=GlobalDeadline /\ bookA /\ bookA'=FALSE
           /\ UNCHANGED <<time,halting,localA,localB>>
Grant == /\ ~bookA /\ ~localB /\ localB'=TRUE
         /\ UNCHANGED <<time,halting,localA,bookA>>
Next == Tick \/ BeginHalt \/ Halt \/ Reclaim \/ Grant
Spec == Init /\ [][Next]_vars
NoTwoEffectiveWriters == ~(localA /\ localB)
=============================================================================
