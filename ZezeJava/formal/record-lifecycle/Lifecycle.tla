---------------------------- MODULE Lifecycle ----------------------------
EXTENDS Integers, Sequences, FiniteSets, TLC
CONSTANTS Event, Fault, MaxAttempts
VARIABLES cache, states, values, timestamps, beans, disk, serial, sink, phase,
          savedRecord, savedBean, savedTimestamp, observed, attempts, owner,
          eventPhase, nextTimestamp, accepted, valid, retried, staleBlocked
vars == <<cache, states, values, timestamps, beans, disk, serial, sink, phase,
          savedRecord, savedBean, savedTimestamp, observed, attempts, owner,
          eventPhase, nextTimestamp, accepted, valid, retried, staleBlocked>>
R == 1..2
Init == /\ cache = 1 /\ states = <<"Share","Absent">>
        /\ values = <<0,0>> /\ timestamps = <<1,0>> /\ beans = <<1,0>>
        /\ disk = 0 /\ serial = 0 /\ sink = -1 /\ phase = "load"
        /\ savedRecord = 1 /\ savedBean = 1 /\ savedTimestamp = 0
        /\ observed = -1 /\ attempts = 1 /\ owner = FALSE
        /\ eventPhase = 0 /\ nextTimestamp = 2 /\ accepted = FALSE
        /\ valid = TRUE /\ retried = FALSE /\ staleBlocked = FALSE
Load == /\ phase = "load" /\ cache # 0 /\ states[cache] \in {"Share","Modify"}
        /\ savedRecord' = cache /\ savedBean' = beans[cache]
        /\ savedTimestamp' = timestamps[cache] /\ phase' = "read"
        /\ UNCHANGED <<cache,states,values,timestamps,beans,disk,serial,sink,observed,
                        attempts,owner,eventPhase,nextTimestamp,accepted,valid,retried,staleBlocked>>
Read == /\ phase = "read" /\ observed' = values[savedBean] /\ phase' = "validate"
        /\ UNCHANGED <<cache,states,values,timestamps,beans,disk,serial,sink,savedRecord,
                        savedBean,savedTimestamp,attempts,owner,eventPhase,nextTimestamp,
                        accepted,valid,retried,staleBlocked>>
Check == /\ phase = "validate" /\ ~owner
         /\ LET r == IF Fault = "validate-current" THEN cache ELSE savedRecord
                usable == IF Fault = "ignore-state" THEN TRUE
                          ELSE states[r] \in {"Share","Modify"}
                ts == IF Fault = "validate-current" THEN timestamps[r] ELSE savedTimestamp
                ok == r # 0 /\ usable /\ ts = timestamps[r]
            IN IF ok THEN /\ owner' = TRUE /\ phase' = "commit"
                          /\ UNCHANGED <<attempts,retried>>
               ELSE /\ owner' = FALSE /\ attempts' = attempts+1 /\ retried' = TRUE
                    /\ phase' = IF attempts < MaxAttempts THEN "load" ELSE "failed"
         /\ UNCHANGED <<cache,states,values,timestamps,beans,disk,serial,sink,savedRecord,
                         savedBean,savedTimestamp,observed,eventPhase,nextTimestamp,
                         accepted,valid,staleBlocked>>
Commit == /\ phase = "commit" /\ owner
          /\ sink' = observed /\ valid' = (valid /\ observed = serial)
          /\ accepted' = TRUE /\ owner' = FALSE /\ phase' = "done"
          /\ UNCHANGED <<cache,states,values,timestamps,beans,disk,serial,savedRecord,
                          savedBean,savedTimestamp,observed,attempts,eventPhase,nextTimestamp,
                          retried,staleBlocked>>
\* Environment actions have the TableKey write lock while changing a usable record.
\* Eviction removes only a clean record. Load uses the backend's committed value.
Evict == /\ Event = "evict" /\ eventPhase = 0 /\ ~owner
         /\ states' = [states EXCEPT ![1] = "Removed"] /\ cache' = 0
         /\ eventPhase' = 1
         /\ UNCHANGED <<values,timestamps,beans,disk,serial,sink,phase,savedRecord,savedBean,
                         savedTimestamp,observed,attempts,owner,nextTimestamp,accepted,valid,
                         retried,staleBlocked>>
NewRecord == /\ Event = "evict" /\ eventPhase = 1 /\ ~owner
             /\ disk' = 1 /\ serial' = 1 /\ cache' = 2
             /\ states' = [states EXCEPT ![2] = "Share"]
             /\ beans' = [beans EXCEPT ![2] = 2] /\ values' = [values EXCEPT ![2] = 1]
             /\ timestamps' = [timestamps EXCEPT ![2] = nextTimestamp]
             /\ nextTimestamp' = nextTimestamp+1 /\ eventPhase' = 2
             /\ UNCHANGED <<sink,phase,savedRecord,savedBean,savedTimestamp,observed,
                             attempts,owner,accepted,valid,retried,staleBlocked>>
Invalidate == /\ Event = "invalid" /\ eventPhase = 0 /\ ~owner
              /\ states' = [states EXCEPT ![1] = "Invalid"] /\ disk' = 1 /\ serial' = 1
              /\ eventPhase' = 1
              /\ UNCHANGED <<cache,values,timestamps,beans,sink,phase,savedRecord,savedBean,
                              savedTimestamp,observed,attempts,owner,nextTimestamp,
                              accepted,valid,retried,staleBlocked>>
Reload == /\ Event = "invalid" /\ eventPhase = 1 /\ ~owner
          /\ states' = [states EXCEPT ![1] = "Share"]
          /\ values' = [values EXCEPT ![2] = disk] /\ beans' = [beans EXCEPT ![1] = 2]
          /\ timestamps' = IF Fault = "reuse-version" THEN timestamps
                           ELSE [timestamps EXCEPT ![1] = nextTimestamp]
          /\ nextTimestamp' = nextTimestamp+1 /\ eventPhase' = 2
          /\ UNCHANGED <<cache,disk,serial,sink,phase,savedRecord,savedBean,savedTimestamp,
                          observed,attempts,owner,accepted,valid,retried,staleBlocked>>
Delete == /\ Event = "delete" /\ eventPhase = 0 /\ ~owner
          /\ values' = [values EXCEPT ![2] = -1] /\ beans' = [beans EXCEPT ![1] = 2]
          /\ serial' = -1 /\ disk' = -1 /\ eventPhase' = 1
          /\ timestamps' = [timestamps EXCEPT ![1] = nextTimestamp]
          /\ nextTimestamp' = nextTimestamp+1
          /\ UNCHANGED <<cache,states,sink,phase,savedRecord,savedBean,savedTimestamp,
                          observed,attempts,owner,accepted,valid,retried,staleBlocked>>
Reinsert == /\ Event = "delete" /\ eventPhase = 1 /\ ~owner
            /\ values' = [values EXCEPT ![2] = 1] /\ serial' = 1 /\ disk' = 1
            /\ timestamps' = [timestamps EXCEPT ![1] = nextTimestamp]
            /\ nextTimestamp' = nextTimestamp+1 /\ eventPhase' = 2
            /\ UNCHANGED <<cache,states,beans,sink,phase,savedRecord,savedBean,savedTimestamp,
                            observed,attempts,owner,accepted,valid,retried,staleBlocked>>
\* Probe the write guard separately: old Record must not borrow the new access entry.
ProbeStale == /\ Event = "evict" /\ eventPhase = 2 /\ ~staleBlocked
              /\ staleBlocked' = TRUE
              /\ valid' = (valid /\ (states[1] = "Removed" \/ 1 # cache))
              /\ UNCHANGED <<cache,states,values,timestamps,beans,disk,serial,sink,phase,
                              savedRecord,savedBean,savedTimestamp,observed,attempts,owner,
                              eventPhase,nextTimestamp,accepted,retried>>
Next == Load \/ Read \/ Check \/ Commit \/ Evict \/ NewRecord \/ Invalidate \/ Reload
        \/ Delete \/ Reinsert \/ ProbeStale
Spec == Init /\ [][Next]_vars
CommitOrderSerializable == valid
NeverRetried == ~retried
NeverReloaded == eventPhase # 2
NeverOldReadCommitted == ~(accepted /\ observed = 0 /\ eventPhase = 2)
NeverStaleBlocked == ~staleBlocked
=============================================================================
