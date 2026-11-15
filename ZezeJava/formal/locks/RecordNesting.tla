--------------------------- MODULE RecordNesting --------------------------
EXTENDS Naturals, FiniteSets, Sequences
CONSTANT BlockingReverse
VARIABLES pc, keyOwner, fairOwner, copied
vars == <<pc,keyOwner,fairOwner,copied>>
Actors == {"transaction","copy"}
Init == /\ pc = [a \in Actors |-> "start"] /\ keyOwner = "none"
        /\ fairOwner = "none" /\ copied = FALSE
Txn ==
  \/ /\ pc["transaction"] = "start" /\ keyOwner = "none"
     /\ keyOwner' = "transaction" /\ pc' = [pc EXCEPT !["transaction"] = "fair"]
     /\ UNCHANGED <<fairOwner,copied>>
  \/ /\ pc["transaction"] = "fair" /\ fairOwner = "none"
     /\ fairOwner' = "transaction" /\ pc' = [pc EXCEPT !["transaction"] = "release"]
     /\ UNCHANGED <<keyOwner,copied>>
  \/ /\ pc["transaction"] = "release"
     /\ keyOwner' = "none" /\ fairOwner' = "none"
     /\ pc' = [pc EXCEPT !["transaction"] = "done"] /\ UNCHANGED copied
Copy ==
  \/ /\ pc["copy"] = "start" /\ fairOwner = "none"
     /\ fairOwner' = "copy" /\ pc' = [pc EXCEPT !["copy"] = "try"]
     /\ UNCHANGED <<keyOwner,copied>>
  \/ /\ pc["copy"] = "try"
     /\ (~BlockingReverse \/ keyOwner = "none")
     /\ copied' = (keyOwner = "none")
     /\ pc' = [pc EXCEPT !["copy"] = "release"]
     /\ UNCHANGED <<keyOwner,fairOwner>>
  \/ /\ pc["copy"] = "release"
     /\ fairOwner' = "none" /\ pc' = [pc EXCEPT !["copy"] = "done"]
     /\ UNCHANGED <<keyOwner,copied>>
Next == Txn \/ Copy
SafetySpec == Init /\ [][Next]_vars
LiveSpec == SafetySpec /\ WF_vars(Txn) /\ WF_vars(Copy)
NoCrossLockCycle == ~(pc["transaction"] = "fair" /\ fairOwner = "copy"
                     /\ pc["copy"] = "try" /\ keyOwner = "transaction" /\ BlockingReverse)
NoBlockedWork == (\E a \in Actors : pc[a] # "done") => ENABLED Next
AllReturned == <> (\A a \in Actors : pc[a] = "done")
=============================================================================
