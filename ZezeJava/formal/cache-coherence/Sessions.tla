-------------------------- MODULE Sessions --------------------------
EXTENDS Naturals, TLC
CONSTANT Fault
VARIABLES generation, connected, book, local, cleanup, fired, changed, staleTouched, seen
vars == <<generation,connected,book,local,cleanup,fired,changed,staleTouched,seen>>
Init == /\ generation=1 /\ connected=TRUE /\ book=2 /\ local=2
        /\ cleanup=0 /\ fired=FALSE /\ changed=FALSE /\ staleTouched=FALSE /\ seen={}
Disconnect == /\ connected /\ ~changed /\ connected'=FALSE
              /\ UNCHANGED <<generation,book,local,cleanup,fired,changed,staleTouched,seen>>
FireCleanup == /\ ~connected /\ ~fired /\ cleanup'=generation /\ fired'=TRUE
               /\ UNCHANGED <<generation,connected,book,local,changed,staleTouched,seen>>
Kick == /\ ~changed /\ ~fired /\ "kick" \notin seen
        /\ connected'=FALSE /\ generation'=generation+1 /\ seen'=seen \cup {"kick"}
        /\ UNCHANGED <<book,local,cleanup,fired,changed,staleTouched>>
NormalClose == /\ connected /\ ~changed /\ "closed" \notin seen
               /\ connected'=FALSE /\ book'=0 /\ local'=0 /\ seen'=seen \cup {"closed"}
               /\ UNCHANGED <<generation,cleanup,fired,changed,staleTouched>>
ReLogin == /\ ~connected /\ ~changed /\ connected'=TRUE /\ generation'=generation+1
           /\ changed'=TRUE /\ seen'=seen \cup {"relogin"}
           /\ UNCHANGED <<book,local,cleanup,fired,staleTouched>>
(* Fresh Login is only authorized after old incarnation is dead or all local locks
   have been released AND its dirty writes flushed. It does not itself fence it. *)
Login == /\ ~connected /\ ~changed /\ connected'=TRUE /\ generation'=generation+1
         /\ book'=0 /\ local'=0 /\ changed'=TRUE /\ seen'=seen \cup {"login"}
         /\ UNCHANGED <<cleanup,fired,staleTouched>>
(* Explicit alternate action checks the effect, not just whether the comparison ran. *)
Cleanup == /\ cleanup#0
           /\ book'=IF cleanup=generation \/ Fault="ignore-generation" THEN 0 ELSE book
           /\ staleTouched'=(staleTouched \/ (cleanup#generation /\ book'#book))
           /\ cleanup'=0
           /\ UNCHANGED <<generation,connected,local,fired,changed,seen>>
(* Authorized cleanup completion requires a fencing certificate external to GCM. *)
Fence == /\ ~connected /\ local#0 /\ local'=0
         /\ UNCHANGED <<generation,connected,book,cleanup,fired,changed,staleTouched,seen>>
SafeCleanup == /\ (cleanup#generation \/ local=0) /\ Cleanup
Next == Disconnect \/ FireCleanup \/ Kick \/ NormalClose \/ ReLogin \/ Login \/ Fence \/ SafeCleanup
Spec == Init /\ [][Next]_vars
OldSessionIsolated == ~staleTouched
Covered == local <= book
NoPreservingReLogin == ~("relogin" \in seen /\ local=2 /\ book=2 /\ fired /\ cleanup=0)
NoFreshLogin == "login" \notin seen
NoKick == "kick" \notin seen
NoClose == "closed" \notin seen
======================================================================
