------------------------------ MODULE Stop ------------------------------
EXTENDS Naturals
CONSTANTS MaxFailures, Bug
VARIABLES phase, pc, mem, disk, dirty, rounds, failures
vars == <<phase,pc,mem,disk,dirty,rounds,failures>>
Init == /\ phase = "running" /\ pc = "idle" /\ mem = 0 /\ disk = 0
        /\ dirty = FALSE /\ rounds = 0 /\ failures = 0
Begin == /\ phase = "running" /\ pc = "idle" /\ pc' = "entered"
         /\ UNCHANGED <<phase,mem,disk,dirty,rounds,failures>>
Apply == /\ pc = "entered" /\ pc' = "applied" /\ mem' = 1
         /\ UNCHANGED <<phase,disk,dirty,rounds,failures>>
Register == /\ pc = "applied" /\ pc' = "registered" /\ dirty' = TRUE
            /\ UNCHANGED <<phase,mem,disk,rounds,failures>>
End == /\ pc = "registered" /\ pc' = "done"
       /\ UNCHANGED <<phase,mem,disk,dirty,rounds,failures>>
Gate == /\ phase = "running" /\ phase' = "stopping"
        /\ UNCHANGED <<pc,mem,disk,dirty,rounds,failures>>
Drain == /\ phase = "stopping" /\ (Bug = "skipWait" \/ pc \in {"idle","done"})
         /\ phase' = "drained" /\ UNCHANGED <<pc,mem,disk,dirty,rounds,failures>>
Success == /\ phase = "drained" /\ dirty /\ rounds < 3
           /\ disk' = mem /\ dirty' = FALSE /\ rounds' = rounds+1
           /\ UNCHANGED <<phase,pc,mem,failures>>
Failure == /\ phase = "drained" /\ dirty /\ rounds < 3 /\ failures < MaxFailures
           /\ rounds' = rounds+1 /\ failures' = failures+1
           /\ UNCHANGED <<phase,pc,mem,disk,dirty>>
Close == /\ phase = "drained" /\ (~dirty \/ rounds = 3) /\ phase' = "closed"
         /\ UNCHANGED <<pc,mem,disk,dirty,rounds,failures>>
Next == Begin \/ Apply \/ Register \/ End \/ Gate \/ Drain \/ Success \/ Failure \/ Close
Spec == Init /\ [][Next]_vars
NoCloseDuringCommit == phase = "closed" => pc \in {"idle","done"}
ClosedSound == (phase = "closed" /\ failures < 3) => mem = disk
NoAbandonedDirty == ~(phase = "closed" /\ dirty)
=============================================================================
