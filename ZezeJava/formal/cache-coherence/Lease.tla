----------------------------- MODULE Lease -----------------------------
EXTENDS Naturals, TLC
CONSTANT Pause, FenceOnResume, Fault
VARIABLES time, localA, bookA, localB, paused, resumed, release, flushed, dirty, seen
vars == <<time,localA,bookA,localB,paused,resumed,release,flushed,dirty,seen>>
Init == /\ time=0 /\ localA=TRUE /\ bookA=TRUE /\ localB=FALSE
        /\ paused=FALSE /\ resumed=FALSE /\ release=FALSE
        /\ flushed=FALSE /\ dirty=TRUE /\ seen={}
(* Abstract ticks: client detects at 2, release/kill by 3, global reclaims at 5.
   Tick fairness/bounds are an ENVIRONMENT CONTRACT, not a GCM implementation guarantee. *)
Tick == /\ time<6 /\ (paused \/ time#3 \/ ~localA)
        /\ time'=time+1 /\ UNCHANGED <<localA,bookA,localB,paused,resumed,release,flushed,dirty,seen>>
Suspend == /\ Pause /\ time=1 /\ ~resumed /\ ~paused /\ paused'=TRUE
           /\ UNCHANGED <<time,localA,bookA,localB,resumed,release,flushed,dirty,seen>>
StartRelease == /\ time>=2 /\ ~paused /\ ~release /\ release'=TRUE
               /\ UNCHANGED <<time,localA,bookA,localB,paused,resumed,flushed,dirty,seen>>
Release == /\ release /\ ~paused /\ localA /\ localA'=FALSE /\ dirty'=FALSE /\ flushed'=TRUE
          /\ seen'=seen \cup {"released"}
          /\ UNCHANGED <<time,bookA,localB,paused,resumed,release>>
Kill == /\ time>=3 /\ ~paused /\ localA /\ localA'=FALSE
        /\ dirty'=FALSE /\ seen'=seen \cup {"killed-data-loss-possible"}
        /\ UNCHANGED <<time,bookA,localB,paused,resumed,release,flushed>>
Reclaim == /\ time>=5 /\ bookA /\ bookA'=FALSE
           /\ UNCHANGED <<time,localA,localB,paused,resumed,release,flushed,dirty,seen>>
GrantB == /\ ~bookA /\ ~localB /\ localB'=TRUE
          /\ UNCHANGED <<time,localA,bookA,paused,resumed,release,flushed,dirty,seen>>
Resume == /\ paused /\ time>=5 /\ paused'=FALSE /\ resumed'=TRUE
          /\ localA'=IF FenceOnResume THEN FALSE ELSE localA
          /\ seen'=seen \cup {"resumed"}
          /\ UNCHANGED <<time,bookA,localB,release,flushed,dirty>>
(* Same failure occurs without pause if server clears ownership on disconnect. *)
EarlyReclaim == /\ Fault="disconnect-reclaims" /\ time=1 /\ bookA
                /\ bookA'=FALSE
                /\ UNCHANGED <<time,localA,localB,paused,resumed,release,flushed,dirty,seen>>
Next == Tick \/ Suspend \/ StartRelease \/ Release \/ Kill \/ Reclaim \/ GrantB \/ Resume \/ EarlyReclaim
Spec == Init /\ [][Next]_vars
NoTwoEffectiveWriters == ~(localA /\ ~paused /\ localB)
NoResume == "resumed" \notin seen
NoRelease == "released" \notin seen
NoKill == "killed-data-loss-possible" \notin seen
=============================================================================
