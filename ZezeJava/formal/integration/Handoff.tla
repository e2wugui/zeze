----------------------------- MODULE Handoff -----------------------------
EXTENDS Integers, Sequences, FiniteSets, TLC

CONSTANTS Scenario, MaxAttempts, FlushBeforeGrant, ReloadAfterGrant,
          CheckVersions

Nodes == 1..2
Keys == 1..2
I == 0
S == 1
M == 2
Other(n) == 3 - n
Initial == IF Scenario = "skew" THEN 1 ELSE 0
ReadKeys(n) == IF Scenario = "increment" THEN <<1>>
               ELSE IF Scenario = "skew" /\ n = 1 THEN <<2, 1>>
               ELSE <<1, 2>>
Writes(n) == IF Scenario = "increment" THEN {1}
             ELSE IF Scenario = "skew" THEN {n}
             ELSE IF n = 1 THEN Keys ELSE {}

VARIABLES permission, loaded, value, version, dataVersion, dirty,
          disk, diskVersion, committed, commitVersion, pc, cursor, attempt,
          reads, stamps, readSet, captured, nextWrite, req, mode, stage,
          source, encoded, encodedVersion, serialOK, commits, didHandoff,
          didRetry, requested

vars == <<permission, loaded, value, version, dataVersion, dirty,
          disk, diskVersion, committed, commitVersion, pc, cursor, attempt,
          reads, stamps, readSet, captured, nextWrite, req, mode, stage,
          source, encoded, encodedVersion, serialOK, commits, didHandoff,
          didRetry, requested>>

Init ==
 /\ permission = [n \in Nodes |-> [k \in Keys |-> I]]
 /\ loaded = [n \in Nodes |-> [k \in Keys |-> FALSE]]
 /\ value = [n \in Nodes |-> [k \in Keys |-> Initial]]
 /\ version = [n \in Nodes |-> [k \in Keys |-> 0]]
 /\ dataVersion = [n \in Nodes |-> [k \in Keys |-> 0]]
 /\ dirty = [n \in Nodes |-> [k \in Keys |-> FALSE]]
 /\ disk = [k \in Keys |-> Initial]
 /\ diskVersion = [k \in Keys |-> 0]
 /\ committed = disk
 /\ commitVersion = diskVersion
 /\ pc = [n \in Nodes |-> "read"]
 /\ cursor = [n \in Nodes |-> 1]
 /\ attempt = [n \in Nodes |-> 1]
 /\ reads = [n \in Nodes |-> [k \in Keys |-> Initial]]
 /\ stamps = [n \in Nodes |-> [k \in Keys |-> 0]]
 /\ readSet = [n \in Nodes |-> {}]
 /\ captured = [n \in Nodes |-> Initial]
 /\ nextWrite = [n \in Nodes |-> 1]
 /\ req = [k \in Keys |-> 0]
 /\ mode = [k \in Keys |-> S]
 /\ stage = [k \in Keys |-> "idle"]
 /\ source = [k \in Keys |-> 0]
 /\ encoded = [k \in Keys |-> Initial]
 /\ encodedVersion = [k \in Keys |-> 0]
 /\ serialOK = TRUE
 /\ commits = <<>>
 /\ didHandoff = FALSE
 /\ didRetry = FALSE
 /\ requested = [n \in Nodes |-> {}]

NoRequest(n) == \A k \in Keys : req[k] # n
Need(n, k, m) ==
 /\ NoRequest(n)
 /\ IF m = S THEN pc[n] = "read" /\ k = ReadKeys(n)[cursor[n]]
                 /\ permission[n][k] = I
    ELSE pc[n] = "upgrade" /\ k = nextWrite[n] /\ k \in Writes(n)
         /\ permission[n][k] # M
         /\ k \notin requested[n]

Request(n, k, m) ==
 /\ Need(n, k, m)
 /\ req[k] = 0
 /\ LET b == Other(n)
        conflict == permission[b][k] = M \/ (m = M /\ permission[b][k] = S)
    IN /\ req' = [req EXCEPT ![k] = n]
       /\ mode' = [mode EXCEPT ![k] = m]
       /\ source' = [source EXCEPT ![k] = IF conflict THEN b ELSE 0]
       /\ stage' = [stage EXCEPT ![k] = IF conflict THEN "reduce" ELSE "grant"]
 /\ requested' = IF m = M THEN [requested EXCEPT ![n] = @ \cup {k}] ELSE requested
 /\ UNCHANGED <<permission, loaded, value, version, dataVersion, dirty,
                disk, diskVersion, committed, commitVersion, pc, cursor,
                attempt, reads, stamps, readSet, captured, nextWrite,
                encoded, encodedVersion, serialOK, commits, didHandoff, didRetry>>

Reduce(k) ==
 /\ stage[k] = "reduce"
 /\ LET b == source[k]
    IN /\ permission' = [permission EXCEPT ![b][k] = IF mode[k] = S THEN S ELSE I]
       /\ loaded' = [loaded EXCEPT ![b][k] = IF mode[k] = S THEN @ ELSE FALSE]
       /\ version' = [version EXCEPT ![b][k] = IF mode[k] = M THEN @ + 1 ELSE @]
       /\ encoded' = [encoded EXCEPT ![k] = value[b][k]]
       /\ encodedVersion' = [encodedVersion EXCEPT ![k] = dataVersion[b][k]]
 /\ stage' = [stage EXCEPT ![k] = "persist"]
 /\ UNCHANGED <<value, dataVersion, dirty, disk, diskVersion, committed,
                commitVersion, pc, cursor, attempt, reads, stamps, readSet,
                captured, nextWrite, req, mode, source, serialOK, commits,
                didHandoff, didRetry, requested>>

Persist(k) ==
 /\ stage[k] = "persist"
 /\ disk' = IF FlushBeforeGrant THEN [disk EXCEPT ![k] = encoded[k]] ELSE disk
 /\ diskVersion' = IF FlushBeforeGrant
                    THEN [diskVersion EXCEPT ![k] = encodedVersion[k]] ELSE diskVersion
 /\ dirty' = IF FlushBeforeGrant
              THEN [dirty EXCEPT ![source[k]][k] = FALSE] ELSE dirty
 /\ stage' = [stage EXCEPT ![k] = "ack"]
 /\ UNCHANGED <<permission, loaded, value, version, dataVersion, committed,
                commitVersion, pc, cursor, attempt, reads, stamps, readSet,
                captured, nextWrite, req, mode, source, encoded, encodedVersion,
                serialOK, commits, didHandoff, didRetry, requested>>

Ack(k) ==
 /\ stage[k] = "ack"
 /\ stage' = [stage EXCEPT ![k] = "grant"]
 /\ didHandoff' = TRUE
 /\ UNCHANGED <<permission, loaded, value, version, dataVersion, dirty,
                disk, diskVersion, committed, commitVersion, pc, cursor,
                attempt, reads, stamps, readSet, captured, nextWrite, req,
                mode, source, encoded, encodedVersion, serialOK, commits, didRetry, requested>>

Grant(k) ==
 /\ stage[k] = "grant"
 /\ LET n == req[k]
        mustLoad == permission[n][k] = I
    IN /\ permission' = [permission EXCEPT ![n][k] = mode[k]]
       /\ loaded' = [loaded EXCEPT ![n][k] = IF mustLoad THEN FALSE ELSE @]
       /\ stage' = [stage EXCEPT ![k] = IF mustLoad THEN "load" ELSE "idle"]
       /\ req' = [req EXCEPT ![k] = IF mustLoad THEN @ ELSE 0]
 /\ UNCHANGED <<value, version, dataVersion, dirty, disk, diskVersion,
                committed, commitVersion, pc, cursor, attempt, reads, stamps,
                readSet, captured, nextWrite, mode, source, encoded,
                encodedVersion, serialOK, commits, didHandoff, didRetry, requested>>

Load(k) ==
 /\ stage[k] = "load"
 /\ LET n == req[k]
    IN /\ value' = [value EXCEPT ![n][k] = IF ReloadAfterGrant THEN disk[k] ELSE @]
       /\ dataVersion' = [dataVersion EXCEPT ![n][k] = IF ReloadAfterGrant THEN diskVersion[k] ELSE @]
       /\ version' = [version EXCEPT ![n][k] = @ + 1]
       /\ loaded' = [loaded EXCEPT ![n][k] = TRUE]
 /\ stage' = [stage EXCEPT ![k] = "idle"]
 /\ req' = [req EXCEPT ![k] = 0]
 /\ UNCHANGED <<permission, dirty, disk, diskVersion, committed, commitVersion,
                pc, cursor, attempt, reads, stamps, readSet, captured, nextWrite,
                mode, source, encoded, encodedVersion, serialOK, commits,
                didHandoff, didRetry, requested>>

ReadStamp(n) ==
 /\ pc[n] = "read" /\ NoRequest(n)
 /\ LET k == ReadKeys(n)[cursor[n]]
    IN /\ permission[n][k] # I /\ loaded[n][k]
       /\ stamps' = [stamps EXCEPT ![n][k] = version[n][k]]
       /\ captured' = [captured EXCEPT ![n] = value[n][k]]
 /\ pc' = [pc EXCEPT ![n] = "value"]
 /\ UNCHANGED <<permission, loaded, value, version, dataVersion, dirty,
                disk, diskVersion, committed, commitVersion, cursor, attempt,
                reads, readSet, nextWrite, req, mode, stage, source, encoded,
                encodedVersion, serialOK, commits, didHandoff, didRetry, requested>>

ReadValue(n) ==
 /\ pc[n] = "value"
 /\ LET k == ReadKeys(n)[cursor[n]]
    IN /\ reads' = [reads EXCEPT ![n][k] = captured[n]]
       /\ readSet' = [readSet EXCEPT ![n] = @ \cup {k}]
 /\ pc' = [pc EXCEPT ![n] = IF cursor[n] = Len(ReadKeys(n)) THEN "upgrade" ELSE "read"]
 /\ cursor' = [cursor EXCEPT ![n] = @ + 1]
 /\ UNCHANGED <<permission, loaded, value, version, dataVersion, dirty,
                disk, diskVersion, committed, commitVersion, attempt,
                stamps, captured, nextWrite, req, mode, stage, source, encoded,
                encodedVersion, serialOK, commits, didHandoff, didRetry, requested>>

Advance(n) ==
 /\ pc[n] = "upgrade" /\ NoRequest(n)
 /\ nextWrite[n] \in Keys
 /\ (nextWrite[n] \notin Writes(n) \/ permission[n][nextWrite[n]] = M
      \/ nextWrite[n] \in requested[n])
 /\ nextWrite' = [nextWrite EXCEPT ![n] = @ + 1]
 /\ UNCHANGED <<permission, loaded, value, version, dataVersion, dirty,
                disk, diskVersion, committed, commitVersion, pc, cursor,
                attempt, reads, stamps, readSet, captured, req, mode, stage,
                source, encoded, encodedVersion, serialOK, commits, didHandoff, didRetry, requested>>

Valid(n) == \A k \in readSet[n] :
             permission[n][k] # I /\ loaded[n][k]
             /\ (~CheckVersions \/ stamps[n][k] = version[n][k])
NewValue(n, k) == IF Scenario = "increment" THEN reads[n][k] + 1
                  ELSE IF Scenario = "skew"
                       THEN IF reads[n][Other(n)] = 1 THEN 0 ELSE reads[n][k]
                       ELSE 1

Commit(n) ==
 /\ pc[n] = "upgrade" /\ nextWrite[n] = 3 /\ NoRequest(n)
 /\ Valid(n)
 /\ \A k \in Writes(n) : permission[n][k] = M
 /\ value' = [value EXCEPT ![n] = [k \in Keys |-> IF k \in Writes(n) THEN NewValue(n,k) ELSE value[n][k]]]
 /\ version' = [version EXCEPT ![n] = [k \in Keys |-> IF k \in Writes(n) THEN version[n][k] + 1 ELSE version[n][k]]]
 /\ dataVersion' = [dataVersion EXCEPT ![n] = [k \in Keys |-> IF k \in Writes(n) THEN Len(commits) + 1 ELSE dataVersion[n][k]]]
 /\ dirty' = [dirty EXCEPT ![n] = [k \in Keys |-> IF k \in Writes(n) THEN TRUE ELSE dirty[n][k]]]
 /\ committed' = [k \in Keys |-> IF k \in Writes(n) THEN NewValue(n,k) ELSE committed[k]]
 /\ commitVersion' = [k \in Keys |-> IF k \in Writes(n) THEN Len(commits) + 1 ELSE commitVersion[k]]
 /\ serialOK' = (serialOK /\ (\A k \in readSet[n] : reads[n][k] = committed[k]))
 /\ commits' = Append(commits, n)
 /\ pc' = [pc EXCEPT ![n] = "done"]
 /\ UNCHANGED <<permission, loaded, disk, diskVersion, cursor, attempt, reads,
                stamps, readSet, captured, nextWrite, req, mode, stage, source,
                encoded, encodedVersion, didHandoff, didRetry, requested>>

Retry(n) ==
 /\ pc[n] = "upgrade" /\ NoRequest(n)
 /\ (~Valid(n) \/ (nextWrite[n] = 3 /\ \E k \in Writes(n) : permission[n][k] # M))
 /\ pc' = [pc EXCEPT ![n] = IF attempt[n] < MaxAttempts THEN "read" ELSE "failed"]
 /\ attempt' = [attempt EXCEPT ![n] = IF @ < MaxAttempts THEN @ + 1 ELSE @]
 /\ cursor' = [cursor EXCEPT ![n] = 1]
 /\ nextWrite' = [nextWrite EXCEPT ![n] = 1]
 /\ readSet' = [readSet EXCEPT ![n] = {}]
 /\ didRetry' = TRUE
 /\ requested' = [requested EXCEPT ![n] = {}]
 /\ UNCHANGED <<permission, loaded, value, version, dataVersion, dirty,
                disk, diskVersion, committed, commitVersion, reads, stamps,
                captured, req, mode, stage, source, encoded, encodedVersion,
                serialOK, commits, didHandoff>>

Next == (\E n \in Nodes : ReadStamp(n) \/ ReadValue(n) \/ Advance(n) \/ Commit(n) \/ Retry(n))
        \/ (\E n \in Nodes, k \in Keys, m \in {S,M} : Request(n,k,m))
        \/ (\E k \in Keys : Reduce(k) \/ Persist(k) \/ Ack(k) \/ Grant(k) \/ Load(k))

Spec == Init /\ [][Next]_vars
TypeOK ==
 /\ permission \in [Nodes -> [Keys -> {I,S,M}]]
 /\ loaded \in [Nodes -> [Keys -> BOOLEAN]]
 /\ dirty \in [Nodes -> [Keys -> BOOLEAN]]
 /\ disk \in [Keys -> Int]
 /\ req \in [Keys -> 0..2]
 /\ pc \in [Nodes -> {"read","value","upgrade","done","failed"}]
 /\ \A n \in Nodes : readSet[n] \subseteq Keys /\ attempt[n] \in 1..MaxAttempts
PermissionExclusive == \A k \in Keys, n \in Nodes :
                         permission[n][k] = M => permission[Other(n)][k] = I
CacheFresh == \A n \in Nodes, k \in Keys :
               loaded[n][k] /\ permission[n][k] # I =>
                 value[n][k] = committed[k] /\ dataVersion[n][k] = commitVersion[k]
CommitOrderSerializable == serialOK
DiskNeverAhead == \A k \in Keys : diskVersion[k] <= commitVersion[k]
NeverBothCommit == Len(commits) < 2
NeverHandoff == ~didHandoff
NeverRetry == ~didRetry
=============================================================================
