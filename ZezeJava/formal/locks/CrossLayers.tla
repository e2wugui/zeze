---------------------------- MODULE CrossLayers ---------------------------
EXTENDS Naturals, Sequences
CONSTANT ReduceTakesLockey
VARIABLES pc, owner, key
vars == <<pc,owner,key>>
Actors == {"commit","reduce"}
Resources == {"L1","L2","R1","R2","G"}
Lockey(k) == IF k = 1 THEN "L1" ELSE "L2"
Record(k) == IF k = 1 THEN "R1" ELSE "R2"
Path(a) == IF a = "commit" THEN <<Lockey(key[a]),"G",Record(key[a])>>
           ELSE IF ReduceTakesLockey THEN <<Lockey(key[a]),Record(key[a]),"G">>
           ELSE <<Record(key[a]),"G">>
Init == /\ pc = [a \in Actors |-> 1]
        /\ owner = [r \in Resources |-> "none"]
        /\ key \in [Actors -> 1..2]
Acquire(a) == /\ pc[a] <= Len(Path(a))
              /\ LET r == Path(a)[pc[a]] IN
                 /\ owner[r] = "none" /\ owner' = [owner EXCEPT ![r] = a]
              /\ pc' = [pc EXCEPT ![a] = @ + 1] /\ UNCHANGED key
Release(a) == /\ pc[a] = Len(Path(a)) + 1
              /\ owner' = [r \in Resources |-> IF owner[r] = a THEN "none" ELSE owner[r]]
              /\ pc' = [pc EXCEPT ![a] = @ + 1] /\ UNCHANGED key
Step(a) == Acquire(a) \/ Release(a)
Next == \E a \in Actors : Step(a)
SafetySpec == Init /\ [][Next]_vars
LiveSpec == SafetySpec /\ (\A a \in Actors : WF_vars(Step(a)))
Waits(a,b) == IF pc[a] <= Len(Path(a)) THEN owner[Path(a)[pc[a]]] = b ELSE FALSE
NoCrossLayerCycle == ~(Waits("commit","reduce") /\ Waits("reduce","commit"))
NoBlockedWork == (\E a \in Actors : pc[a] <= Len(Path(a)) + 1) => ENABLED Next
AllReturned == <> (\A a \in Actors : pc[a] = Len(Path(a)) + 2)
=============================================================================
