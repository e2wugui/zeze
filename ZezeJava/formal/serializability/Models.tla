---------------------------- MODULE Models ----------------------------
EXTENDS Serializability

Read(k) == [kind |-> "read", key |-> k, value |-> 0]
Write(k, v) == [kind |-> "write", key |-> k, value |-> v]
Increment(k) == [kind |-> "increment", key |-> k, value |-> 0]

Zeros == [k \in Keys |-> 0]
Ones == [k \in Keys |-> 1]
Absent == [k \in Keys |-> -1]
Twice(program) == <<program, program>>
LostUpdate == [t \in Txns |-> << <<Read(1), Increment(1)>>,
                                <<Read(1), Increment(1)>> >>]
WriteSkew == [t \in Txns |-> Twice(<<Read(3-t), Write(t, 1)>>)]
FracturedRead == [t \in Txns |-> Twice(
    IF t = 1 THEN <<Write(1, 1), Write(2, 1)>> ELSE <<Read(1), Read(2)>>)]
RepeatedRead == [t \in Txns |-> Twice(
    IF t = 1 THEN <<Write(1, 1)>> ELSE <<Read(1), Read(1)>>)]
OwnWrites == [t \in Txns |-> Twice(
    IF t = 1 THEN <<Write(1, 1), Read(1), Increment(1), Read(1), Write(1, -1), Read(1)>>
             ELSE <<Read(1)>>)]
MissingInsert == [t \in Txns |-> Twice(<<Read(1), Write(1, 1)>>)]
DeleteReinsert == [t \in Txns |-> Twice(
    CASE t = 1 -> <<Read(1), Write(1, -1)>>
      [] t = 2 -> <<Read(1), Write(1, 1)>>
      [] OTHER -> <<Read(1), Read(1)>>)]
LockUpgrade == [t \in Txns |->
    IF t = 1 THEN << <<Read(1), Read(2)>>, <<Read(1), Write(1, 1)>> >>
             ELSE Twice(<<Write(2, 1)>>)]
LockInsert == [t \in Txns |->
    IF t = 1 THEN << <<Read(2)>>, <<Read(1), Write(1, 1), Read(2)>> >>
             ELSE Twice(<<Write(2, 1)>>)]
ThreeWayCycle == [t \in Txns |-> Twice(<<Read(t), Write((t % 3) + 1, 1)>>)]

\* All 12 read/write programs plus two read-only and two increment programs.
\* Every transaction independently chooses its program in an initial state.
GenericChoices ==
    {Twice(<<Read(r), Write(w, v)>>) : r \in Keys, w \in Keys, v \in {-1, 0, 1}}
    \cup {Twice(<<Read(1), Read(2)>>), Twice(<<Read(2), Read(1)>>)}
    \cup {Twice(<<Read(k), Increment(k)>>) : k \in Keys}
GenericPrograms == [Txns -> GenericChoices]
=============================================================================
