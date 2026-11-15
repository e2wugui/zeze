------------------------- MODULE MapSavepoints -------------------------
EXTENDS Integers, Sequences, FiniteSets, TLC
CONSTANTS MaxSteps, MaxDepth, Fault
VARIABLES maps, frames, reference, allocation, steps, shared, phase, rolled
vars == <<maps,frames,reference,allocation,steps,shared,phase,rolled>>
Keys == 1..2
Empty == [k \in Keys |-> -1]
Init == /\ maps = [i \in 0..MaxSteps |-> Empty]
        /\ frames = <<0>> /\ reference = <<Empty>> /\ allocation = 0
        /\ steps = 0 /\ shared = Empty /\ phase = "run" /\ rolled = FALSE
Begin == /\ phase = "run" /\ steps < MaxSteps /\ Len(frames) < MaxDepth
         /\ frames' = Append(frames,frames[Len(frames)])
         /\ reference' = Append(reference,reference[Len(reference)])
         /\ steps' = steps+1 /\ UNCHANGED <<maps,allocation,shared,phase,rolled>>
Mutate == /\ phase = "run" /\ steps < MaxSteps
          /\ \E key \in Keys, value \in {-1,0,1}:
               LET n == Len(frames)
                   id == IF Fault = "mutable-alias" THEN frames[n] ELSE allocation+1
               IN /\ maps' = [maps EXCEPT ![id] = [maps[frames[n]] EXCEPT ![key] = value]]
                  /\ frames' = [frames EXCEPT ![n] = id]
                  /\ reference' = [reference EXCEPT ![n][key] = value]
          /\ allocation' = allocation+1 /\ steps' = steps+1
          /\ UNCHANGED <<shared,phase,rolled>>
Undo == /\ phase = "run" /\ steps < MaxSteps /\ Len(frames) > 1
        /\ frames' = SubSeq(frames,1,Len(frames)-1)
        /\ reference' = SubSeq(reference,1,Len(reference)-1)
        /\ rolled' = TRUE /\ steps' = steps+1
        /\ UNCHANGED <<maps,allocation,shared,phase>>
Merge == /\ phase = "run" /\ steps < MaxSteps /\ Len(frames) > 1
         /\ frames' = Append(SubSeq(frames,1,Len(frames)-2),frames[Len(frames)])
         /\ reference' = Append(SubSeq(reference,1,Len(reference)-2),reference[Len(reference)])
         /\ steps' = steps+1 /\ UNCHANGED <<maps,allocation,shared,phase,rolled>>
Commit == /\ phase = "run" /\ Len(frames) = 1
          /\ shared' = maps[frames[1]] /\ phase' = "committed"
          /\ UNCHANGED <<maps,frames,reference,allocation,steps,rolled>>
Abort == /\ phase = "run" /\ phase' = "aborted"
         /\ UNCHANGED <<maps,frames,reference,allocation,steps,shared,rolled>>
Next == Begin \/ Mutate \/ Undo \/ Merge \/ Commit \/ Abort
Spec == Init /\ [][Next]_vars
CommitOrderSerializable ==
 /\ \A i \in 1..Len(frames): maps[frames[i]] = reference[i]
 /\ (phase = "committed" => shared = reference[1])
 /\ (phase # "committed" => shared = Empty)
NeverRolled == ~rolled
=============================================================================
