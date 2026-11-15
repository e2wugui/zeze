----------------------- MODULE Serializability -----------------------
EXTENDS Integers, Sequences, FiniteSets, TLC

CONSTANTS TxnCount, KeyCount, MaxAttempts, Programs, InitialData, ExplorePrograms,
          CheckReadVersions, HoldLocksUntilCommit, PublishValuesFirst

Txns == 1..TxnCount
Keys == 1..KeyCount
NoVersion == -1
NoWrite == -2
EmptyWrites == [k \in Keys |-> NoWrite]
EmptyVersions == [k \in Keys |-> NoVersion]
EmptyLocks == [k \in Keys |-> "none"]

VARIABLES data, version, pc, pos, seen, writes, events, held,
          conflict, published, stamped, history, commitOrder, attempt, paths, workload
vars == <<data, version, pc, pos, seen, writes, events, held,
          conflict, published, stamped, history, commitOrder, attempt, paths, workload>>
Program(t) == workload[t][attempt[t]]

\* This oracle replays observed reads and buffered writes sequentially.
\* It does not use locks or version numbers from the implementation model.
RECURSIVE Replay(_, _)
Replay(es, store) ==
    IF es = <<>> THEN [ok |-> TRUE, store |-> store]
    ELSE LET e == Head(es)
             next == IF e.kind = "read" THEN store
                     ELSE [store EXCEPT ![e.key] = e.value]
             rest == Replay(Tail(es), next)
         IN [ok |-> (e.kind # "read" \/ store[e.key] = e.value) /\ rest.ok,
             store |-> rest.store]

Init ==
    /\ data = InitialData
    /\ version = [k \in Keys |-> 0]
    /\ pc = [t \in Txns |-> "run"]
    /\ pos = [t \in Txns |-> 1]
    /\ seen = [t \in Txns |-> EmptyVersions]
    /\ writes = [t \in Txns |-> EmptyWrites]
    /\ events = [t \in Txns |-> <<>>]
    /\ held = [t \in Txns |-> EmptyLocks]
    /\ conflict = [t \in Txns |-> FALSE]
    /\ published = [t \in Txns |-> {}]
    /\ stamped = [t \in Txns |-> {}]
    /\ history = <<>>
    /\ commitOrder = <<>>
    /\ attempt = [t \in Txns |-> 1]
    /\ paths = {}
    /\ workload \in IF ExplorePrograms THEN Programs ELSE {Programs}

Load(t) ==
    /\ pc[t] = "run" /\ pos[t] <= Len(Program(t))
    /\ LET k == Program(t)[pos[t]].key
       IN /\ seen[t][k] = NoVersion
          /\ seen' = [seen EXCEPT ![t][k] = version[k]]
    /\ UNCHANGED <<data, version, pc, pos, writes, events, held,
                    conflict, published, stamped, history, commitOrder, attempt, paths, workload>>

Execute(t) ==
    /\ pc[t] = "run" /\ pos[t] <= Len(Program(t))
    /\ LET op == Program(t)[pos[t]]
           k == op.key
           v == CASE op.kind = "read" ->
                         IF writes[t][k] = NoWrite THEN data[k] ELSE writes[t][k]
                  [] op.kind = "increment" -> events[t][Len(events[t])].value + 1
                  [] OTHER -> op.value
       IN /\ seen[t][k] # NoVersion
          /\ writes' = IF op.kind = "read" THEN writes
                       ELSE [writes EXCEPT ![t][k] = v]
          /\ events' = [events EXCEPT ![t] = Append(@,
                 [kind |-> IF op.kind = "read" THEN "read" ELSE "write",
                  key |-> k, value |-> v])]
    /\ pos' = [pos EXCEPT ![t] = @ + 1]
    /\ UNCHANGED <<data, version, pc, seen, held, conflict,
                    published, stamped, history, commitOrder, attempt, paths, workload>>

BeginValidation(t) ==
    /\ pc[t] = "run" /\ pos[t] > Len(Program(t))
    /\ pc' = [pc EXCEPT ![t] = "lock"]
    /\ pos' = [pos EXCEPT ![t] = 1]
    /\ UNCHANGED <<data, version, seen, writes, events, held, conflict,
                    published, stamped, history, commitOrder, attempt, paths, workload>>

SkipKey(t) ==
    /\ pc[t] = "lock" /\ pos[t] \in Keys
    /\ seen[t][pos[t]] = NoVersion
    /\ pos' = [pos EXCEPT ![t] = @ + 1]
    /\ held' = [held EXCEPT ![t][pos[t]] = "none"]
    /\ UNCHANGED <<data, version, pc, seen, writes, events,
                    conflict, published, stamped, history, commitOrder, attempt, paths, workload>>

NeedsRepair(t, k) ==
    (held[t][k] = "read" /\ writes[t][k] # NoWrite) \/
    (held[t][k] = "none" /\ \E later \in Keys : later > k /\ held[t][later] # "none")

RepairLocks(t) ==
    /\ pc[t] = "lock" /\ pos[t] \in Keys
    /\ LET k == pos[t]
       IN /\ seen[t][k] # NoVersion /\ NeedsRepair(t, k)
          /\ paths' = paths \cup {IF held[t][k] = "read" THEN "upgrade" ELSE "insert"}
          /\ held' = [held EXCEPT ![t] =
                        [j \in Keys |-> IF j >= k THEN "none" ELSE held[t][j]]]
    /\ UNCHANGED <<data, version, pc, pos, seen, writes, events, conflict,
                    published, stamped, history, commitOrder, attempt, workload>>

Acquire(t) ==
    /\ pc[t] = "lock" /\ pos[t] \in Keys
    /\ LET k == pos[t]
           mode == IF writes[t][k] = NoWrite THEN "read" ELSE "write"
       IN /\ seen[t][k] # NoVersion
          /\ ~NeedsRepair(t, k)
          /\ \A other \in Txns \ {t} :
                 held[other][k] = "none" \/
                 (mode = "read" /\ held[other][k] = "read")
          /\ held' = [held EXCEPT ![t][k] = mode]
    /\ pc' = [pc EXCEPT ![t] = "check"]
    /\ UNCHANGED <<data, version, pos, seen, writes, events, conflict,
                    published, stamped, history, commitOrder, attempt, paths, workload>>

Validate(t) ==
    /\ pc[t] = "check"
    /\ conflict' = [conflict EXCEPT ![t] = @ \/
           ((CheckReadVersions \/ writes[t][pos[t]] # NoWrite) /\
            seen[t][pos[t]] # version[pos[t]])]
    /\ pos' = [pos EXCEPT ![t] = @ + 1]
    /\ pc' = [pc EXCEPT ![t] = "lock"]
    /\ UNCHANGED <<data, version, seen, writes, events, held,
                    published, stamped, history, commitOrder, attempt, paths, workload>>

FinishValidation(t) ==
    /\ pc[t] = "lock" /\ pos[t] > KeyCount
    /\ pc' = [pc EXCEPT ![t] = IF conflict[t] THEN "retry" ELSE "apply"]
    /\ held' = IF HoldLocksUntilCommit THEN held
               ELSE [held EXCEPT ![t] = EmptyLocks]
    /\ UNCHANGED <<data, version, pos, seen, writes, events, conflict,
                    published, stamped, history, commitOrder, attempt, paths, workload>>

Retry(t, keep) ==
    /\ pc[t] = "retry" /\ attempt[t] < MaxAttempts
    /\ pc' = [pc EXCEPT ![t] = "run"]
    /\ attempt' = [attempt EXCEPT ![t] = @ + 1]
    /\ paths' = paths \cup {IF keep THEN "retained" ELSE "released"}
    /\ pos' = [pos EXCEPT ![t] = 1]
    /\ seen' = [seen EXCEPT ![t] = EmptyVersions]
    /\ writes' = [writes EXCEPT ![t] = EmptyWrites]
    /\ events' = [events EXCEPT ![t] = <<>>]
    /\ conflict' = [conflict EXCEPT ![t] = FALSE]
    /\ held' = IF keep THEN held ELSE [held EXCEPT ![t] = EmptyLocks]
    /\ UNCHANGED <<data, version, published, stamped, history, commitOrder, workload>>

Publish(t, k) ==
    /\ pc[t] = "apply" /\ writes[t][k] # NoWrite /\ k \notin published[t]
    /\ data' = [data EXCEPT ![k] = writes[t][k]]
    /\ published' = [published EXCEPT ![t] = @ \cup {k}]
    /\ UNCHANGED <<version, pc, pos, seen, writes, events, held, conflict,
                    stamped, history, commitOrder, attempt, paths, workload>>

Stamp(t, k) ==
    /\ pc[t] = "apply" /\ writes[t][k] # NoWrite /\ k \notin stamped[t]
    /\ ~PublishValuesFirst \/ k \in published[t]
    /\ version' = [version EXCEPT ![k] = @ + 1]
    /\ stamped' = [stamped EXCEPT ![t] = @ \cup {k}]
    /\ UNCHANGED <<data, pc, pos, seen, writes, events, held, conflict,
                    published, history, commitOrder, attempt, paths, workload>>

FinishCommit(t) ==
    /\ pc[t] = "apply"
    /\ \A k \in Keys : writes[t][k] = NoWrite \/
                             (k \in published[t] /\ k \in stamped[t])
    /\ history' = history \o events[t]
    /\ commitOrder' = Append(commitOrder, t)
    /\ held' = [held EXCEPT ![t] = EmptyLocks]
    /\ pc' = [pc EXCEPT ![t] = "done"]
    /\ UNCHANGED <<data, version, pos, seen, writes, events, conflict,
                    published, stamped, attempt, paths, workload>>

Abort(t) ==
    /\ pc[t] = "retry" /\ attempt[t] = MaxAttempts
    /\ held' = [held EXCEPT ![t] = EmptyLocks]
    /\ pc' = [pc EXCEPT ![t] = "done"]
    /\ UNCHANGED <<data, version, pos, seen, writes, events, conflict,
                    published, stamped, history, commitOrder, attempt, paths, workload>>

Next == \E t \in Txns :
    Load(t) \/ Execute(t) \/ BeginValidation(t) \/ SkipKey(t) \/
    RepairLocks(t) \/ Acquire(t) \/ Validate(t) \/ FinishValidation(t) \/ FinishCommit(t) \/
    (\E keep \in BOOLEAN : Retry(t, keep)) \/
    Abort(t) \/ (\E k \in Keys : Publish(t, k) \/ Stamp(t, k))

Spec == Init /\ [][Next]_vars
CommitOrderSerializable == Replay(history, InitialData).ok
QuiescentDataMatches ==
    (\A t \in Txns : pc[t] # "apply") =>
        data = Replay(history, InitialData).store

TypeOK ==
    /\ data \in [Keys -> Int] /\ version \in [Keys -> Nat]
    /\ pc \in [Txns -> {"run", "lock", "check", "retry", "apply", "done"}]
    /\ pos \in [Txns -> Nat] /\ attempt \in [Txns -> 1..MaxAttempts]
    /\ held \in [Txns -> [Keys -> {"none", "read", "write"}]]
    /\ seen \in [Txns -> [Keys -> Int]] /\ writes \in [Txns -> [Keys -> Int]]
    /\ conflict \in [Txns -> BOOLEAN]
    /\ published \in [Txns -> SUBSET Keys] /\ stamped \in [Txns -> SUBSET Keys]
    /\ commitOrder \in Seq(Txns)
    /\ Cardinality({commitOrder[i] : i \in 1..Len(commitOrder)}) = Len(commitOrder)

NeverAllCommitted == Len(commitOrder) < TxnCount
NeverRetained == "retained" \notin paths
NeverReleased == "released" \notin paths
NeverUpgraded == "upgrade" \notin paths
NeverInsertedBeforeHeld == "insert" \notin paths
=============================================================================
