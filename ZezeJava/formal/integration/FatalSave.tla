----------------------------- MODULE FatalSave -----------------------------
EXTENDS Naturals
CONSTANTS CloseGate, DrainCommits, KillDeadline, BoundedWatchdog, BlockIO, RemotePending
VARIABLES time, phase, accepting, pending, admitted, mem, disk, snapshot,
          failed, retried, cleanExit, reclaimed, granted, oldWrite, queued
vars == <<time,phase,accepting,pending,admitted,mem,disk,snapshot,
          failed,retried,cleanExit,reclaimed,granted,oldWrite,queued>>
Alive == phase \notin {"exited", "killed"}
Init == /\ time=0 /\ phase="run" /\ accepting=TRUE
        /\ pending=TRUE /\ admitted=FALSE /\ mem=1 /\ disk=0 /\ snapshot=0
        /\ failed=FALSE /\ retried=FALSE /\ cleanExit=FALSE
        /\ reclaimed=FALSE /\ granted=FALSE /\ oldWrite=FALSE /\ queued=FALSE
(* Environment contract, not fairness: the independent kill thread runs by its
   deadline. Removing this bound intentionally admits arbitrary process pauses. *)
Tick == /\ time<12
        /\ (time#3 \/ phase#"run")
        /\ (~BoundedWatchdog \/ time#KillDeadline \/ ~Alive)
        /\ time'=time+1
        /\ UNCHANGED <<phase,accepting,pending,admitted,mem,disk,snapshot,
                        failed,retried,cleanExit,reclaimed,granted,oldWrite,queued>>
Admit == /\ pending /\ ~admitted /\ accepting /\ Alive
         /\ admitted'=TRUE
         /\ UNCHANGED <<time,phase,accepting,pending,mem,disk,snapshot,
                         failed,retried,cleanExit,reclaimed,granted,oldWrite,queued>>
Commit == /\ admitted /\ Alive /\ admitted'=FALSE /\ pending'=FALSE /\ mem'=2
          /\ UNCHANGED <<time,phase,accepting,disk,snapshot,failed,retried,
                          cleanExit,reclaimed,granted,oldWrite,queued>>
Fatal == /\ time=3 /\ phase="run" /\ phase'="quiesce"
         /\ accepting'=IF CloseGate THEN FALSE ELSE accepting
         /\ UNCHANGED <<time,pending,admitted,mem,disk,snapshot,failed,retried,
                         cleanExit,reclaimed,granted,oldWrite,queued>>
Collect == /\ phase="quiesce" /\ (~DrainCommits \/ ~admitted)
           /\ snapshot'=mem /\ phase'="snapshot"
           /\ UNCHANGED <<time,accepting,pending,admitted,mem,disk,failed,retried,
                           cleanExit,reclaimed,granted,oldWrite,queued>>
Fail == /\ phase="snapshot" /\ ~failed /\ failed'=TRUE /\ phase'="quiesce"
        /\ UNCHANGED <<time,accepting,pending,admitted,mem,disk,snapshot,retried,
                        cleanExit,reclaimed,granted,oldWrite,queued>>
Queue == /\ phase="snapshot" /\ queued'=TRUE /\ phase'="waiting"
         /\ UNCHANGED <<time,accepting,pending,admitted,mem,disk,snapshot,failed,
                         retried,cleanExit,reclaimed,granted,oldWrite>>
(* Default models a synchronous backend whose unfinished request dies with its
   caller. RemotePending explicitly removes that extra backend contract. *)
Flush == /\ queued /\ (~BlockIO \/ ~Alive) /\ (Alive \/ RemotePending)
         /\ disk'=snapshot /\ phase'=(IF Alive THEN "saved" ELSE phase) /\ queued'=FALSE
         /\ oldWrite'=(oldWrite \/ granted) /\ retried'=failed
         /\ UNCHANGED <<time,accepting,pending,admitted,mem,snapshot,failed,
                         cleanExit,reclaimed,granted>>
Exit == /\ phase="saved" /\ phase'="exited" /\ cleanExit'=TRUE
        /\ UNCHANGED <<time,accepting,pending,admitted,mem,disk,snapshot,failed,
                        retried,reclaimed,granted,oldWrite,queued>>
Kill == /\ time>=KillDeadline /\ Alive /\ phase'="killed"
        /\ UNCHANGED <<time,accepting,pending,admitted,mem,disk,snapshot,failed,
                        retried,cleanExit,reclaimed,granted,oldWrite,queued>>
Reclaim == /\ time>=9 /\ ~reclaimed /\ reclaimed'=TRUE
           /\ UNCHANGED <<time,phase,accepting,pending,admitted,mem,disk,snapshot,
                           failed,retried,cleanExit,granted,oldWrite,queued>>
Grant == /\ reclaimed /\ ~granted /\ granted'=TRUE /\ disk'=3
         /\ UNCHANGED <<time,phase,accepting,pending,admitted,mem,snapshot,
                         failed,retried,cleanExit,reclaimed,oldWrite,queued>>
Next == Tick \/ Admit \/ Commit \/ Fatal \/ Collect \/ Fail \/ Queue \/ Flush \/ Exit \/ Kill \/ Reclaim \/ Grant
Spec == Init /\ [][Next]_vars
NoOldWriteAfterGrant == ~oldWrite
SavedOnCleanExit == cleanExit => (disk>=mem)
NoSavingWitness == ~(cleanExit /\ disk=2)
NoRetryWitness == ~(cleanExit /\ retried)
NoTakeoverWitness == ~granted
NoBlockedExitWitness == ~(BlockIO /\ phase="killed")
=============================================================================
