--------------------------- MODULE Checkpoint ---------------------------
EXTENDS Naturals, FiniteSets, TLC
CONSTANTS Keys, Slots, MaxWrites, MaxFailures, Mode, Bug, AllowCrash
VARIABLES mem, disk, dirty, groups, batches, committed, ack, durable,
          writes, failures, up, crashes, rolledBack, atomicWrites
vars == <<mem,disk,dirty,groups,batches,committed,ack,durable,
          writes,failures,up,crashes,rolledBack,atomicWrites>>
Zero == [k \in Keys |-> 0]
EmptyBatch == [phase |-> "idle", keys |-> {}, snap |-> Zero]
Held == UNION {batches[s].keys : s \in Slots}
Init == /\ mem = Zero /\ disk = Zero /\ dirty = {}
        /\ groups = {{k} : k \in Keys}
        /\ batches = [s \in Slots |-> EmptyBatch]
        /\ committed = {Zero} /\ ack = Zero /\ durable = Zero
        /\ writes = 0 /\ failures = 0 /\ up = TRUE /\ crashes = 0
        /\ rolledBack = FALSE /\ atomicWrites = {}

\* A validated top-level transaction writes an arbitrary nonempty subset,
\* and merges all touched RRS before applying logs. Stable record identities
\* and successful local validation come from S0/S1/S3, not this model.
Commit(touched, changed) ==
  LET merged == UNION {g \in groups : g \cap touched # {}}
      nextMem == [k \in Keys |-> IF k \in changed THEN writes+1 ELSE mem[k]]
  IN /\ up /\ writes < MaxWrites
     /\ changed # {} /\ changed \subseteq touched
     /\ (Bug \in {"unlocked","overlap"} \/ merged \cap Held = {})
     /\ (Mode = "async" \/ (dirty = {} /\ \A s \in Slots : batches[s].phase = "idle"))
     /\ mem' = nextMem /\ writes' = writes+1
     /\ groups' = IF Bug = "splitGroup" THEN groups
                   ELSE (groups \ {g \in groups : g \cap touched # {}}) \cup {merged}
     /\ dirty' = dirty \cup changed
     /\ committed' = committed \cup {nextMem}
     /\ atomicWrites' = atomicWrites \cup {[keys |-> changed, ver |-> writes+1]}
     /\ ack' = IF Mode = "async" \/ Bug = "ackEarly" THEN nextMem ELSE ack
     /\ UNCHANGED <<disk,batches,durable,failures,up,crashes,rolledBack>>
Rollback == /\ up /\ ~rolledBack /\ rolledBack' = TRUE
            /\ mem' = IF Bug = "rollback" THEN [k \in Keys |-> 99] ELSE mem
            /\ dirty' = IF Bug = "rollback" THEN Keys ELSE dirty
            /\ UNCHANGED <<disk,groups,batches,committed,ack,durable,writes,failures,up,crashes,atomicWrites>>

Begin(s,g) == /\ up /\ batches[s].phase = "idle" /\ g \in groups
              /\ g \cap dirty # {} /\ (Bug = "overlap" \/ g \cap Held = {})
              /\ batches' = [batches EXCEPT ![s] = [phase |-> "locked", keys |-> g, snap |-> Zero]]
              /\ UNCHANGED <<mem,disk,dirty,groups,committed,ack,durable,writes,failures,up,crashes,rolledBack,atomicWrites>>
Encode(s) == /\ up /\ batches[s].phase = "locked"
             /\ batches' = [batches EXCEPT ![s].phase = "encoded", ![s].snap = mem]
             /\ dirty' = IF Bug = "earlyClean" THEN dirty \ batches[s].keys ELSE dirty
             /\ UNCHANGED <<mem,disk,groups,committed,ack,durable,writes,failures,up,crashes,rolledBack,atomicWrites>>
Write(s) == /\ up /\ batches[s].phase = "encoded"
            /\ batches' = [batches EXCEPT ![s].phase = "inflight"]
            /\ UNCHANGED <<mem,disk,dirty,groups,committed,ack,durable,writes,failures,up,crashes,rolledBack,atomicWrites>>
DbCommit(s) ==
  LET b == batches[s]
      nd == [k \in Keys |-> IF k \in b.keys THEN b.snap[k] ELSE disk[k]]
  IN /\ up /\ b.phase = "inflight"
     /\ batches' = [batches EXCEPT ![s].phase = "dbdone"]
     /\ disk' = nd
     /\ durable' = [k \in Keys |-> IF nd[k] > durable[k] THEN nd[k] ELSE durable[k]]
     /\ UNCHANGED <<mem,dirty,groups,committed,ack,writes,failures,up,crashes,rolledBack,atomicWrites>>
Finish(s) == /\ up /\ batches[s].phase = "dbdone"
             /\ dirty' = dirty \ batches[s].keys
             /\ groups' = (groups \ {g \in groups : g \cap batches[s].keys # {}})
                           \cup {{k} : k \in batches[s].keys}
             /\ ack' = IF Mode # "async" THEN mem ELSE ack
             /\ batches' = [batches EXCEPT ![s] = EmptyBatch]
             /\ UNCHANGED <<mem,disk,committed,durable,writes,failures,up,crashes,rolledBack,atomicWrites>>
Fail(s) == /\ up /\ batches[s].phase \in {"encoded","inflight"}
           /\ failures < MaxFailures /\ failures' = failures+1
           /\ batches' = [batches EXCEPT ![s] = EmptyBatch]
           /\ UNCHANGED <<mem,disk,dirty,groups,committed,ack,durable,writes,up,crashes,rolledBack,atomicWrites>>
Crash == /\ AllowCrash /\ up /\ crashes < 1
         /\ up' = FALSE /\ crashes' = crashes+1
         /\ disk' = IF Bug = "lostMedia" THEN Zero ELSE disk
         /\ mem' = disk' /\ dirty' = {} /\ groups' = {{k} : k \in Keys}
         /\ batches' = [s \in Slots |-> EmptyBatch]
         /\ UNCHANGED <<committed,ack,durable,writes,failures,rolledBack,atomicWrites>>
Restart == /\ ~up /\ up' = TRUE
           /\ UNCHANGED <<mem,disk,dirty,groups,batches,committed,ack,durable,writes,failures,crashes,rolledBack,atomicWrites>>
Start == \E s \in Slots, g \in groups : Begin(s,g)
Progress(s) == Encode(s) \/ Write(s) \/ (Bug # "neverSuccess" /\ DbCommit(s)) \/ Finish(s)
Next == (\E t \in SUBSET Keys, c \in SUBSET Keys : Commit(t,c)) \/ Rollback \/ Start
        \/ (\E s \in Slots : Progress(s) \/ Fail(s)) \/ Crash \/ Restart
Spec == Init /\ [][Next]_vars
LiveSpec == Spec /\ WF_vars(Start) /\ (\A s \in Slots : WF_vars(Progress(s)))

\* Values written to the medium must come from a committed per-key state.
NoRollbackValue == \A k \in Keys : \E c \in committed : disk[k] = c[k]
CleanSound == up => \A k \in Keys \ dirty : mem[k] = disk[k]
NoOlderOverwrite == \A k \in Keys : disk[k] >= durable[k]
DurableAck == Mode # "async" => \A k \in Keys : disk[k] >= ack[k]
Partition == /\ UNION groups = Keys
             /\ \A g,h \in groups : g = h \/ g \cap h = {}
GroupAtomic == crashes = 0 => \A w \in atomicWrites :
                (\A k \in w.keys : disk[k] >= w.ver) \/ (\A k \in w.keys : disk[k] < w.ver)
\* Each committed write generation must eventually be stored or superseded.
EveryWritePersists == \A k \in Keys, n \in 1..MaxWrites :
                         (mem[k] = n) ~> (disk[k] >= n)
NoRetryWitness == failures = 0
NoConcurrentWitness == Cardinality({s \in Slots : batches[s].phase # "idle"}) < 2
NoPostFlushWriteWitness == ~(writes = MaxWrites /\ dirty # {} /\ \E k \in Keys : disk[k] > 0 /\ disk[k] < mem[k])
NoAsyncLossWitness == ~(crashes = 1 /\ \E k \in Keys : ack[k] > disk[k])
NoDurableCrashWitness == ~(crashes = 1 /\ \E k \in Keys : disk[k] > 0)
NoNonPrefixWitness == disk \in committed
=============================================================================
