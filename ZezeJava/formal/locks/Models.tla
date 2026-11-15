------------------------------ MODULE Models ------------------------------
EXTENDS LockProtocol
ReadBoth == <<1,1>>
WriteBoth == <<2,2>>
ReadLater == <<0,1>>
WriteEarlierReadLater == <<2,1>>
AllPlans == {p \in [1..KeyCount -> 0..2] : \E k \in 1..KeyCount : p[k] # 0}
GenericPrograms == [t \in 1..TxnCount |-> {<<p>> : p \in AllPlans}]
EvolvingPrograms == [t \in 1..TxnCount |->
  IF t = 1 THEN {<<ReadBoth,WriteBoth>>, <<ReadLater,WriteEarlierReadLater>>,
                 <<ReadBoth,<<0,2>>>>, <<ReadBoth,<<1,0>>>>}
  ELSE {<<WriteBoth,ReadBoth>>}]
CyclePrograms == [t \in 1..TxnCount |-> {<<[k \in 1..KeyCount |-> 2]>>}]
ExhaustPrograms == [t \in 1..TxnCount |-> {<<[k \in 1..KeyCount |-> 1]>>}]
BoundaryPrograms == [t \in 1..TxnCount |-> {[r \in 1..MaxAttempts |-> [k \in 1..KeyCount |-> 1]]}]
=============================================================================
