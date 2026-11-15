------------------------- MODULE Permissions -------------------------
EXTENDS Naturals, FiniteSets, TLC
CONSTANTS Clients, MaxRequests, InitialShared, Fault
VARIABLES book, local, value, disk, latest, requests, phase, wanted,
          timedout, pending, reduce, busy, writes, seen
vars == <<book, local, value, disk, latest, requests, phase, wanted,
          timedout, pending, reduce, busy, writes, seen>>
I == 0
S == 1
M == 2
First == CHOOSE c \in Clients : \A d \in Clients : c <= d
Init == /\ book = [c \in Clients |-> IF InitialShared THEN S ELSE IF c=First THEN M ELSE I]
        /\ local = book /\ value = [c \in Clients |-> 0]
        /\ disk=0 /\ latest=0 /\ requests=[c \in Clients |-> 0]
        /\ phase=[c \in Clients |-> "idle"] /\ wanted=[c \in Clients |-> I]
        /\ timedout=[c \in Clients |-> FALSE] /\ pending=0
        /\ reduce=[c \in Clients |-> "none"] /\ busy=[c \in Clients |-> FALSE]
        /\ writes={} /\ seen={}
Send(c,m) == /\ phase[c] \in {"idle","done"} /\ requests[c]<MaxRequests
             /\ wanted'=[wanted EXCEPT ![c]=m]
             /\ phase'=[phase EXCEPT ![c]="sent"]
             /\ requests'=[requests EXCEPT ![c]=@+1]
             /\ busy'=[busy EXCEPT ![c]=TRUE]
             /\ timedout'=[timedout EXCEPT ![c]=FALSE]
             /\ UNCHANGED <<book,local,value,disk,latest,pending,reduce,writes,seen>>
Conflict(c,d) == c # d /\ book[d] # I /\ (wanted[c]=M \/ book[d]=M)
Start(c) == /\ pending=0 /\ phase[c]="sent"
            /\ pending'=c /\ phase'=[phase EXCEPT ![c]="processing"]
            /\ reduce'=[d \in Clients |-> IF Conflict(c,d) THEN "queued" ELSE "none"]
            /\ UNCHANGED <<book,local,value,disk,latest,requests,wanted,timedout,busy,writes,seen>>
(* Record/Lockey excludes both local transactions and response installation while reducing.
   An acquiring client holds the record lock until response or timeout, so a Reduce
   cannot overtake an unconsumed successful Acquire response. *)
ApplyReduce(c) == /\ reduce[c] \in {"queued","late"} /\ ~busy[c]
                  /\ local'=[local EXCEPT ![c]=I]
                  /\ disk'=IF local[c]=M /\ Fault # "no-flush" THEN value[c] ELSE disk
                  /\ reduce'=[reduce EXCEPT ![c]=IF @="late" THEN "ignored" ELSE "applied"]
                  /\ seen'=seen \cup {"reduce"}
                  /\ UNCHANGED <<book,value,latest,requests,phase,wanted,timedout,pending,busy,writes>>
Ack(c) == /\ reduce[c]="applied"
          /\ reduce'=[reduce EXCEPT ![c]="ok"] /\ book'=[book EXCEPT ![c]=I]
          /\ UNCHANGED <<local,value,disk,latest,requests,phase,wanted,timedout,pending,busy,writes,seen>>
ReduceTimeout(c) == /\ reduce[c] \in {"queued","applied"}
                    /\ reduce'=[reduce EXCEPT ![c]=IF @="queued" THEN "late" ELSE "ignored"]
                    /\ book'=IF Fault="timeout-success" THEN [book EXCEPT ![c]=I] ELSE book
                    /\ seen'=seen \cup {"reduce-timeout"}
                    /\ UNCHANGED <<local,value,disk,latest,requests,phase,wanted,timedout,pending,busy,writes>>
Refuse(c) == /\ reduce[c]="queued" /\ reduce'=[reduce EXCEPT ![c]="refused"]
             /\ UNCHANGED <<book,local,value,disk,latest,requests,phase,wanted,timedout,pending,busy,writes,seen>>
Finish == /\ pending # 0
          /\ \A c \in Clients : reduce[c] \notin {"queued","applied"}
          /\ LET c == pending IN
             /\ book'=IF \E d \in Clients : Conflict(c,d) THEN book
                        ELSE [book EXCEPT ![c]=IF @=M THEN M ELSE wanted[c]]
             /\ phase'=[phase EXCEPT ![c]=IF \E d \in Clients : Conflict(c,d) THEN "failed" ELSE "reply"]
          /\ pending'=0
          /\ UNCHANGED <<local,value,disk,latest,requests,wanted,timedout,reduce,busy,writes,seen>>
Receive(c) == /\ phase[c] \in {"reply","failed"}
              /\ local'=IF phase[c]="reply" /\ ~timedout[c] THEN [local EXCEPT ![c]=book[c]] ELSE local
              /\ value'=IF phase[c]="reply" /\ ~timedout[c] /\ local[c]=I
                         THEN [value EXCEPT ![c]=disk] ELSE value
              /\ phase'=[phase EXCEPT ![c]="done"] /\ busy'=[busy EXCEPT ![c]=FALSE]
              /\ seen'=seen \cup (IF timedout[c] THEN {"late-response"} ELSE {"response"})
              /\ UNCHANGED <<book,disk,latest,requests,wanted,timedout,pending,reduce,writes>>
AcquireTimeout(c) == /\ busy[c] /\ ~timedout[c]
                    /\ timedout'=[timedout EXCEPT ![c]=TRUE] /\ busy'=[busy EXCEPT ![c]=FALSE]
                    /\ UNCHANGED <<book,local,value,disk,latest,requests,phase,wanted,pending,reduce,writes,seen>>
Write(c) == /\ local[c]=M /\ ~busy[c] /\ c \notin writes
            /\ value'=[value EXCEPT ![c]=latest+1] /\ latest'=latest+1 /\ writes'=writes \cup {c}
            /\ UNCHANGED <<book,local,disk,requests,phase,wanted,timedout,pending,reduce,busy,seen>>
Next == (\E c \in Clients : (\E m \in {S,M}:Send(c,m)) \/ Start(c) \/ ApplyReduce(c)
          \/ Ack(c) \/ ReduceTimeout(c) \/ Refuse(c) \/ Receive(c) \/ AcquireTimeout(c) \/ Write(c)) \/ Finish
Spec == Init /\ [][Next]_vars
BookCoherent == \A c,d \in Clients : c#d => ~(book[c]=M /\ book[d]#I)
LocalCoherent == \A c,d \in Clients : c#d => ~(local[c]=M /\ local[d]#I)
Covered == \A c \in Clients : local[c] <= book[c]
Visible == \A c \in Clients : local[c]#I => value[c]=latest
TypeOK == /\ book \in [Clients -> {I,S,M}] /\ local \in [Clients -> {I,S,M}]
          /\ latest <= Cardinality(Clients) /\ disk <= latest
NoHandoff == ~("reduce" \in seen /\ "response" \in seen /\ local[First]=I)
NoLateResponse == "late-response" \notin seen
NoFailedReduction == "reduce-timeout" \notin seen
======================================================================
