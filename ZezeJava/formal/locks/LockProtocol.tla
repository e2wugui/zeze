--------------------------- MODULE LockProtocol ---------------------------
EXTENDS Naturals, FiniteSets, Sequences, TLC

CONSTANTS TxnCount, KeyCount, MaxAttempts, Programs,
          RepairOrder, ReleaseReadBeforeWrite, ReleaseOnReturn
Tx == 1..TxnCount
Keys == 1..KeyCount
Modes == 0..2 \* absent, shared, exclusive
VARIABLES pc, held, program, attempt, todo, target, conflict, result, seen
vars == <<pc, held, program, attempt, todo, target, conflict, result, seen>>
Plan(t) == program[t][attempt[t]]
Empty == [k \in Keys |-> 0]
Min(s) == CHOOSE k \in s : \A j \in s : k <= j
Max(s) == CHOOSE k \in s : \A j \in s : k >= j
Pick(t) == IF ~RepairOrder /\ t = TxnCount THEN Max(todo[t]) ELSE Min(todo[t])
Compatible(t,k,m) == \A u \in Tx \ {t} :
                       held[u][k] = 0 \/ (m = 1 /\ held[u][k] = 1)
Init == /\ pc = [t \in Tx |-> "body"]
        /\ held = [t \in Tx |-> Empty]
        /\ program \in [Tx -> UNION {Programs[t] : t \in Tx}]
        /\ \A t \in Tx : program[t] \in Programs[t]
        /\ attempt = [t \in Tx |-> 1]
        /\ todo = [t \in Tx |-> {}]
        /\ target = [t \in Tx |-> 1]
        /\ conflict = [t \in Tx |-> FALSE]
        /\ result = [t \in Tx |-> "pending"]
        /\ seen = [s \in {"retained","released","upgrade","insert","exhausted","exception","obsolete","trailing"} |-> FALSE]

Body(t) == /\ pc[t] = "body"
           /\ todo' = [todo EXCEPT ![t] = {k \in Keys : Plan(t)[k] # 0}]
           /\ conflict' = [conflict EXCEPT ![t] = FALSE]
           /\ pc' = [pc EXCEPT ![t] = "prepare"]
           /\ UNCHANGED <<held,program,attempt,target,result,seen>>
Prepare(t) ==
  /\ pc[t] = "prepare" /\ todo[t] # {}
  /\ LET k == Pick(t)
         upgrade == held[t][k] = 1 /\ Plan(t)[k] = 2
         earlier == held[t][k] = 0 /\ \E j \in Keys : j > k /\ held[t][j] # 0
         cut == (upgrade /\ ReleaseReadBeforeWrite) \/ (earlier /\ RepairOrder)
         keep == [j \in Keys |-> IF (j < k /\ Plan(t)[j] = 0) \/ (cut /\ j >= k)
                                  THEN 0 ELSE held[t][j]]
     IN /\ held' = [held EXCEPT ![t] = keep]
        /\ target' = [target EXCEPT ![t] = k]
        /\ pc' = [pc EXCEPT ![t] = IF keep[k] >= Plan(t)[k] THEN "check" ELSE "acquire"]
        /\ seen' = [seen EXCEPT !["upgrade"] = @ \/ upgrade,
                               !["insert"] = @ \/ earlier,
                               !["obsolete"] = @ \/ (\E j \in Keys : j < k /\ Plan(t)[j] = 0 /\ held[t][j] # 0)]
  /\ UNCHANGED <<program,attempt,todo,conflict,result>>
Acquire(t) ==
  /\ pc[t] = "acquire"
  /\ LET k == target[t] IN
     /\ ~(held[t][k] = 1 /\ Plan(t)[k] = 2) \* Java RRWL cannot upgrade while owning read
     /\ Compatible(t,k,Plan(t)[k])
     /\ held' = [held EXCEPT ![t][k] = Plan(t)[k]]
  /\ pc' = [pc EXCEPT ![t] = "check"]
  /\ UNCHANGED <<program,attempt,todo,target,conflict,result,seen>>
Check(t) == \* _check_ terminates locally; external acquire obligations are separate
  /\ pc[t] = "check"
  /\ \E outcome \in {"valid","conflict","invalid","exception"} :
     /\ conflict' = [conflict EXCEPT ![t] = @ \/ outcome = "conflict"]
     /\ todo' = [todo EXCEPT ![t] = @ \ {target[t]}]
     /\ pc' = [pc EXCEPT ![t] = CASE outcome = "invalid" -> "retryRelease"
                                   [] outcome = "exception" -> "release"
                                   [] OTHER -> "prepare"]
     /\ result' = [result EXCEPT ![t] = IF outcome = "exception" THEN "failed" ELSE @]
     /\ seen' = [seen EXCEPT !["exception"] = @ \/ outcome = "exception"]
  /\ UNCHANGED <<held,program,attempt,target>>
Finish(t) ==
  /\ pc[t] = "prepare" /\ todo[t] = {}
  /\ pc' = [pc EXCEPT ![t] = IF conflict[t] THEN "retryKeep" ELSE "release"]
  /\ result' = [result EXCEPT ![t] = IF conflict[t] THEN @ ELSE "success"]
  /\ seen' = [seen EXCEPT !["trailing"] = @ \/ (\E k \in Keys : Plan(t)[k] = 0 /\ held[t][k] # 0)]
  /\ UNCHANGED <<held,program,attempt,todo,target,conflict>>
Retry(t) ==
  /\ pc[t] \in {"retryKeep","retryRelease"}
  /\ LET exhausted == attempt[t] = MaxAttempts
         releasing == pc[t] = "retryRelease"
     IN /\ held' = [held EXCEPT ![t] = IF releasing THEN Empty ELSE @]
        /\ pc' = [pc EXCEPT ![t] = IF exhausted THEN "release" ELSE "body"]
        /\ attempt' = [attempt EXCEPT ![t] = IF exhausted THEN @ ELSE @ + 1]
        /\ result' = [result EXCEPT ![t] = IF exhausted THEN "failed" ELSE @]
        /\ seen' = [seen EXCEPT !["retained"] = @ \/ ~releasing,
                               !["released"] = @ \/ releasing,
                               !["exhausted"] = @ \/ exhausted]
  /\ UNCHANGED <<program,todo,target,conflict>>
Return(t) ==
  /\ pc[t] = "release"
  /\ held' = [held EXCEPT ![t] = IF ReleaseOnReturn THEN Empty ELSE @]
  /\ pc' = [pc EXCEPT ![t] = "done"]
  /\ UNCHANGED <<program,attempt,todo,target,conflict,result,seen>>
Step(t) == Body(t) \/ Prepare(t) \/ Acquire(t) \/ Check(t) \/ Finish(t) \/ Retry(t) \/ Return(t)
Next == \E t \in Tx : Step(t)
SafetySpec == Init /\ [][Next]_vars
FiniteSpec == SafetySpec /\ (\A t \in Tx : WF_vars(Step(t)))

Waits(t,u) == pc[t] = "acquire" /\
  LET k == target[t] IN
    (u = t /\ held[t][k] = 1 /\ Plan(t)[k] = 2)
    \/ (u # t /\ held[u][k] # 0 /\ (Plan(t)[k] = 2 \/ held[u][k] = 2))
Paths == UNION {[1..n -> Tx] : n \in 1..TxnCount}
NoWaitCycle == ~\E p \in Paths :
  Waits(p[Len(p)],p[1]) /\ \A i \in 1..(Len(p)-1) : Waits(p[i],p[i+1])
MutualExclusion == \A k \in Keys : \A t,u \in Tx :
  (t # u /\ held[t][k] = 2) => held[u][k] = 0
ReturnedReleased == \A t \in Tx : pc[t] = "done" => held[t] = Empty
WellTyped == /\ held \in [Tx -> [Keys -> Modes]] /\ attempt \in [Tx -> 1..MaxAttempts]
             /\ result \in [Tx -> {"pending","success","failed"}]
NoBlockedWork == (\E t \in Tx : pc[t] # "done") => ENABLED Next
AllReturned == <> (\A t \in Tx : pc[t] = "done" /\ result[t] \in {"success","failed"})
SystemProgress == <> (\E t \in Tx : pc[t] = "done")
NeverRetained == ~seen["retained"]
NeverReleased == ~seen["released"]
NeverUpgraded == ~seen["upgrade"]
NeverInserted == ~seen["insert"]
NeverExhausted == ~seen["exhausted"]
NeverException == ~seen["exception"]
NeverObsoleteReleased == ~seen["obsolete"]
NeverTrailingRetained == ~seen["trailing"]
=============================================================================
