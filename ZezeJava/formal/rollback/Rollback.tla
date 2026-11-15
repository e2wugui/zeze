----------------------------- MODULE Rollback -----------------------------
EXTENDS Integers, Sequences, FiniteSets, TLC
CONSTANTS Programs, MaxAttempts, Fault
VARIABLES heap, root, version, pc, atr, stamp, frames, trace, attempt, owner,
          pending, phase, serial, valid, committed, retried, rolled, replaced
vars == <<heap, root, version, pc, atr, stamp, frames, trace, attempt, owner,
          pending, phase, serial, valid, committed, retried, rolled, replaced>>
T == 1..2
F == 1..2
B == 1..3
Zero == [f \in F |-> 0]
EmptyLogs == [b \in B |-> [f \in F |-> -1]]
EmptyFrame == [logs |-> EmptyLogs, put |-> 0]
Op(k, f, v) == <<k, f, v>>
Read(f) == Op("read", f, 0)
Write(f, v) == Op("write", f, v)
Begin == Op("begin", 0, 0)
Merge == Op("merge", 0, 0)
Undo == Op("undo", 0, 0)
Replace == Op("replace", 0, 2)
Abort == Op("abort", 0, 0)
PairRead == <<Read(1), Read(2)>>
NestedUndo == <<Read(1), Write(1,1), Begin, Write(1,2), Write(2,2),
                Read(1), Undo, Read(1), Read(2), Write(2,1)>>
NestedMerge == <<Write(1,1), Begin, Write(1,2), Write(2,2), Merge, Read(1), Read(2)>>
ReplaceUndo == <<Write(1,1), Begin, Replace, Write(2,1), Read(1), Undo,
                 Read(1), Read(2)>>
ReplaceMerge == <<Write(1,1), Begin, Replace, Write(2,1), Merge, Read(1), Read(2)>>
TopAbort == <<Write(1,1), Begin, Write(2,2), Merge, Abort>>
RetryFirst == <<Write(1,2), Read(2)>>
RetrySecond == <<Read(1), Write(2,1)>>
Writer == <<Write(2,2)>>
NestedUndoPrograms == <<NestedUndo, PairRead>>
NestedMergePrograms == <<NestedMerge, PairRead>>
ReplaceUndoPrograms == <<ReplaceUndo, PairRead>>
ReplaceMergePrograms == <<ReplaceMerge, PairRead>>
TopAbortPrograms == <<TopAbort, PairRead>>
RetryPrograms == <<RetryFirst, Writer>>
TwoWritersPrograms == <<NestedUndo, NestedMerge>>
ReplaceWritersPrograms == <<ReplaceMerge, NestedUndo>>
DeepUndo == <<Write(1,1), Begin, Write(1,2), Begin, Write(2,2), Merge,
              Read(2), Undo, Read(1), Read(2)>>
DeepUndoPrograms == <<DeepUndo, PairRead>>
Program(t) == IF Programs = RetryPrograms /\ t = 1 /\ attempt[t] > 1
              THEN RetrySecond ELSE Programs[t]
Top(t) == frames[t][Len(frames[t])]
Ptr(t) == IF Top(t).put = 0 THEN atr[t] ELSE Top(t).put
Value(t,f) == IF Top(t).logs[Ptr(t)][f] = -1 THEN heap[Ptr(t)][f]
              ELSE Top(t).logs[Ptr(t)][f]

\* Independent sequential interpreter: logical values and a savepoint stack,
\* with no object IDs, physical logs, timestamps, locks or publishing actions.
RECURSIVE Replay(_, _, _)
Replay(es, stack, ok) ==
 IF Len(es) = 0 THEN [value |-> stack[1], ok |-> ok]
 ELSE LET e == Head(es)
          n == Len(stack)
          cur == stack[n]
          next == CASE e[1] = "read" -> stack
                    [] e[1] = "write" -> [stack EXCEPT ![n][e[2]] = e[3]]
                    [] e[1] = "replace" -> [stack EXCEPT ![n] = [f \in F |-> e[3]]]
                    [] e[1] = "begin" -> Append(stack,cur)
                    [] e[1] = "undo" -> SubSeq(stack,1,n-1)
                    [] e[1] = "merge" -> Append(SubSeq(stack,1,n-2),cur)
                    [] OTHER -> stack
      IN Replay(Tail(es), next, ok /\ (e[1] # "read" \/ cur[e[2]] = e[3]))

Init == /\ heap = [b \in B |-> Zero] /\ root = 1 /\ version = 0
        /\ pc = [t \in T |-> 0] /\ atr = [t \in T |-> 1]
        /\ stamp = [t \in T |-> 0] /\ frames = [t \in T |-> <<EmptyFrame>>]
        /\ trace = [t \in T |-> <<>>] /\ attempt = [t \in T |-> 1]
        /\ owner = 0 /\ pending = {} /\ phase = [t \in T |-> "run"]
        /\ serial = Zero /\ valid = TRUE /\ committed = {}
        /\ retried = {} /\ rolled = {} /\ replaced = {}
Load(t) == /\ phase[t] = "run" /\ pc[t] = 0
           /\ atr' = [atr EXCEPT ![t] = root] /\ stamp' = [stamp EXCEPT ![t] = version]
           /\ pc' = [pc EXCEPT ![t] = 1]
           /\ UNCHANGED <<heap,root,version,frames,trace,attempt,owner,pending,phase,
                           serial,valid,committed,retried,rolled,replaced>>
Step(t) ==
 /\ phase[t] = "run" /\ pc[t] \in 1..Len(Program(t))
 /\ LET e == Program(t)[pc[t]]
        n == Len(frames[t])
        fr == Top(t)
        actual == IF e[1] = "read" THEN Op("read",e[2],Value(t,e[2])) ELSE e
        newfr == CASE e[1] = "write" -> [fr EXCEPT !.logs[Ptr(t)][e[2]] = e[3]]
                   [] e[1] = "replace" -> [fr EXCEPT !.put = t+1,
                                                !.logs[t+1] = [f \in F |-> e[3]]]
                   [] OTHER -> fr
        next == CASE e[1] = "begin" -> Append(frames[t],fr)
                  [] e[1] = "undo" -> IF Fault = "skip-undo" THEN
                              Append(SubSeq(frames[t],1,n-2),fr) ELSE SubSeq(frames[t],1,n-1)
                  [] e[1] = "merge" -> IF Fault = "bad-merge" THEN SubSeq(frames[t],1,n-1)
                              ELSE Append(SubSeq(frames[t],1,n-2),fr)
                  [] OTHER -> [frames[t] EXCEPT ![n] = newfr]
    IN /\ frames' = [frames EXCEPT ![t] = next]
       /\ trace' = [trace EXCEPT ![t] = Append(@,actual)]
       /\ phase' = [phase EXCEPT ![t] = IF e[1] = "abort" THEN "aborted" ELSE @]
       /\ rolled' = IF e[1] = "undo" THEN rolled \cup {t} ELSE rolled
       /\ replaced' = IF e[1] = "replace" THEN replaced \cup {t} ELSE replaced
 /\ pc' = [pc EXCEPT ![t] = @+1]
 /\ UNCHANGED <<heap,root,version,atr,stamp,attempt,owner,pending,serial,valid,committed,retried>>
Validate(t) ==
 /\ phase[t] = "run" /\ pc[t] > Len(Program(t)) /\ owner = 0
 /\ IF stamp[t] = version THEN
       /\ owner' = t /\ phase' = [phase EXCEPT ![t] = "publish"]
       /\ pending' = {<<b,f>> \in B \X F : Top(t).logs[b][f] # -1}
       /\ UNCHANGED <<pc,atr,stamp,frames,trace,attempt,retried>>
    ELSE
       /\ attempt' = [attempt EXCEPT ![t] = @+1]
       /\ phase' = [phase EXCEPT ![t] = IF attempt[t] < MaxAttempts THEN "run" ELSE "aborted"]
       /\ pc' = [pc EXCEPT ![t] = 0] /\ trace' = [trace EXCEPT ![t] = <<>>]
       /\ frames' = IF Fault = "reuse-logs" THEN frames ELSE [frames EXCEPT ![t] = <<EmptyFrame>>]
       /\ retried' = retried \cup {t} /\ UNCHANGED <<owner,pending,atr,stamp>>
 /\ UNCHANGED <<heap,root,version,serial,valid,committed,rolled,replaced>>
Publish(t) == /\ owner = t /\ phase[t] = "publish" /\ pending # {}
              /\ \E x \in pending:
                  /\ heap' = [heap EXCEPT ![x[1]][x[2]] = Top(t).logs[x[1]][x[2]]]
                  /\ pending' = pending \ {x}
              /\ UNCHANGED <<root,version,pc,atr,stamp,frames,trace,attempt,owner,phase,
                              serial,valid,committed,retried,rolled,replaced>>
PublishRoot(t) == /\ owner = t /\ phase[t] = "publish" /\ pending = {}
                  /\ root' = IF Top(t).put = 0 THEN root ELSE Top(t).put
                  /\ phase' = [phase EXCEPT ![t] = "finish"]
                  /\ UNCHANGED <<heap,version,pc,atr,stamp,frames,trace,attempt,owner,pending,
                                  serial,valid,committed,retried,rolled,replaced>>
Finish(t) == /\ owner = t /\ phase[t] = "finish"
             /\ LET r == Replay(trace[t],<<serial>>,TRUE)
                IN /\ serial' = r.value /\ valid' = (valid /\ r.ok /\ heap[root] = r.value)
             /\ version' = IF Top(t).put # 0 \/ \E b \in B, f \in F: Top(t).logs[b][f] # -1
                           THEN version+1 ELSE version
             /\ owner' = 0
             /\ phase' = [phase EXCEPT ![t] = "done"] /\ committed' = committed \cup {t}
             /\ UNCHANGED <<heap,root,pc,atr,stamp,frames,trace,attempt,pending,retried,rolled,replaced>>
Discard(t) == /\ phase[t] = "aborted" /\ phase' = [phase EXCEPT ![t] = "done"]
              /\ heap' = IF Fault = "abort-leak" THEN [heap EXCEPT ![root][1] = 1] ELSE heap
              /\ UNCHANGED <<root,version,pc,atr,stamp,frames,trace,attempt,owner,pending,
                              serial,valid,committed,retried,rolled,replaced>>
Next == \E t \in T: Load(t) \/ Step(t) \/ Validate(t) \/ Publish(t) \/ PublishRoot(t) \/ Finish(t) \/ Discard(t)
Spec == Init /\ [][Next]_vars
CommitOrderSerializable == valid /\ (owner # 0 \/ heap[root] = serial)
NeverRetried == retried = {}
NeverRolled == rolled = {}
NeverReplaced == replaced = {}
NeverAllCommitted == committed # T
=============================================================================
