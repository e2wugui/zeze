------------------------------ MODULE Barging -----------------------------
EXTENDS Naturals
CONSTANT Continuous, DropRelease
VARIABLES owner, victim, competitor, work, last
vars == <<owner,victim,competitor,work,last>>
Init == /\ owner = "competitor" /\ victim = "waiting" /\ competitor = "held"
        /\ work = FALSE /\ last = "init"
Wake == /\ victim = "waiting" /\ owner = "none"
        /\ victim' = "awake" /\ last' = "victimWake"
        /\ UNCHANGED <<owner,competitor,work>>
VictimTry == /\ victim = "awake"
             /\ victim' = IF owner = "none" THEN "held" ELSE "waiting"
             /\ owner' = IF owner = "none" THEN "victim" ELSE owner
             /\ last' = IF owner = "none" THEN "victimAcquire" ELSE "victimMiss"
             /\ UNCHANGED <<competitor,work>>
VictimReturn == /\ victim = "held" /\ owner' = "none" /\ victim' = "returned"
                /\ last' = "victimReturn" /\ UNCHANGED <<competitor,work>>
Release == /\ competitor = "held"
           /\ owner' = IF DropRelease THEN owner ELSE "none"
           /\ competitor' = "returned" /\ work' = TRUE /\ last' = "competitorReturn"
           /\ UNCHANGED victim
Arrive == /\ Continuous /\ competitor = "returned"
          /\ competitor' = "ready" /\ last' = "arrive" /\ UNCHANGED <<owner,victim,work>>
Barge == /\ competitor = "ready" /\ owner = "none"
         /\ owner' = "competitor" /\ competitor' = "held"
         /\ last' = "barge" /\ UNCHANGED <<victim,work>>
Next == Wake \/ VictimTry \/ VictimReturn \/ Release \/ Arrive \/ Barge
SafetySpec == Init /\ [][Next]_vars
LiveSpec == SafetySpec /\ WF_vars(Wake) /\ WF_vars(VictimTry) /\ WF_vars(VictimReturn)
            /\ WF_vars(Release) /\ WF_vars(Arrive) /\ WF_vars(Barge)
VictimReturns == <> (victim = "returned")
SystemProgress == <> work
OngoingProgress == []<>(last = "competitorReturn")
ScheduledVictimReturns == ([]<>(last = "victimMiss")) => VictimReturns
ReturnedReleased == competitor = "returned" => owner # "competitor"
=============================================================================
