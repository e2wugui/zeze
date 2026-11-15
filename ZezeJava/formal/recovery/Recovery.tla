---------------------------- MODULE Recovery ----------------------------
EXTENDS Checkpoint
\* Crash discards all RRS, dirty references, snapshots and DB transactions.
\* Only committed backend rows survive; no Zeze TransactionLog WAL exists.
=============================================================================
