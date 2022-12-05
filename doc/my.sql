
KEY.type = VARBINARY(?)
	3070 porlardb-x
	3072 mysql 8
	8000 sqlserver？

必须的

DROP TABLE IF EXISTS ?
CREATE TABLE IF NOT EXISTS TableName (id VARBINARY(3070) NOT NULL PRIMARY KEY, value LONGBLOB NOT NULL)
SELECT value FROM TableName WHERE id = ?
DELETE FROM TableName WHERE id=?
REPLACE INTO TableName values(?, ?)

这两句也是必须的，但对使用有限制，不会非常频繁。
SELECT id,value FROM TableName
SELECT id,value FROM TableName WHERE id > ? LIMIT ?

可选的（最好提供）

CREATE TABLE IF NOT EXISTS _ZezeDataWithVersion_ (
	id VARBINARY(3070) NOT NULL PRIMARY KEY,
	data LONGBLOB NOT NULL,
	version bigint NOT NULL
	);

DROP PROCEDURE IF EXISTS _ZezeSaveDataWithSameVersion_;

SELECT data,version FROM _ZezeDataWithVersion_ WHERE id=?;

Create procedure _ZezeSaveDataWithSameVersion_ (
	IN    in_id VARBINARY(3070),
	IN    in_data LONGBLOB,
	INOUT inout_version bigint,
	OUT   ReturnValue int
	)
	return_label:
	BEGIN
		DECLARE oldversionexsit BIGINT;
		DECLARE ROWCOUNT int;

		START TRANSACTION;
		set ReturnValue=1;
		select version INTO oldversionexsit from _ZezeDataWithVersion_ where id=in_id;
		select COUNT(*) into ROWCOUNT from _ZezeDataWithVersion_ where id=in_id;
		if ROWCOUNT > 0 then
			if oldversionexsit <> inout_version then
				set ReturnValue=2;
				ROLLBACK;
				LEAVE return_label;
			end if;
			set oldversionexsit = oldversionexsit + 1;
			update _ZezeDataWithVersion_ set data=in_data, version=oldversionexsit where id=in_id;
			/*
			select ROW_COUNT() into ROWCOUNT;
			if ROWCOUNT = 1 then
				set inout_version = oldversionexsit;
				set ReturnValue=0;
				COMMIT;
				LEAVE return_label;
			end if;
			set ReturnValue=3;
			ROLLBACK;
			LEAVE return_label;
			*/
			/* new add */
			set ReturnValue=0;
			COMMIT;
		end if;

		insert into _ZezeDataWithVersion_ values(in_id,in_data,inout_version);
		/*
		select ROW_COUNT() into ROWCOUNT;
		if ROWCOUNT = 1 then
			set ReturnValue=0;
			COMMIT;
			LEAVE return_label;
		end if;
		set ReturnValue=4;
		ROLLBACK;
		LEAVE return_label;
		*/

		/* new add */
		set ReturnValue=0;
		IF 1=1 THEN
			COMMIT;
		END IF;
	END;

CREATE TABLE IF NOT EXISTS _ZezeInstances_ (localid int NOT NULL PRIMARY KEY);

Create procedure _ZezeSetInUse_ (
	in in_localid int,
	in in_global LONGBLOB,
	out ReturnValue int
	)
	return_label:
	begin
		DECLARE currentglobal LONGBLOB;
		declare emptybinary LONGBLOB;
		DECLARE InstanceCount int;
		DECLARE ROWCOUNT int;
		
		START TRANSACTION;
		set ReturnValue=1;
		if exists (select localid from _ZezeInstances_ where localid=in_localid) then
			set ReturnValue=2;
			ROLLBACK;
			LEAVE return_label;
		end if;		
		insert into _ZezeInstances_ values(in_localid);
		/*
		select ROW_COUNT() into ROWCOUNT;
		if ROWCOUNT = 0 then
			set ReturnValue=3;
			ROLLBACK;
			LEAVE return_label;
		end if;
		*/
		set emptybinary = BINARY '';
		select data into currentglobal from _ZezeDataWithVersion_ where id=emptybinary;
		SELECT COUNT(*) into ROWCOUNT from _ZezeDataWithVersion_ where id=emptybinary;
		if ROWCOUNT > 0 then
			if currentglobal <> in_global then
				set ReturnValue=4;
				ROLLBACK;
				LEAVE return_label;
			end if;
		else
			insert into _ZezeDataWithVersion_ values(emptybinary, in_global, 0);
			/*
			select ROW_COUNT() into ROWCOUNT;
			if ROWCOUNT <> 1 then
				set ReturnValue=5;
				ROLLBACK;
				LEAVE return_label;
			end if;
			*/
		end if;
		set InstanceCount=0;
		select count(*) INTO InstanceCount from _ZezeInstances_;
		if InstanceCount = 1 then
			set ReturnValue=0;
			COMMIT;
			LEAVE return_label;
		end if;
		if LENGTH(in_global)=0 then
			set ReturnValue=6;
			ROLLBACK;
			LEAVE return_label;
		end if;
		set ReturnValue=0;
		IF 1=1 THEN
			COMMIT;
		END IF;
		LEAVE return_label;
	end;

Create procedure _ZezeClearInUse_ (
	in in_localid int,
	in in_global LONGBLOB,
	out ReturnValue int
	)
	return_label:
	begin
		DECLARE InstanceCount int;
		declare emptybinary LONGBLOB;
		DECLARE ROWCOUNT INT;

		START TRANSACTION;
		set ReturnValue=1;
		delete from _ZezeInstances_ where localid=in_localid;
		/*
		select ROW_COUNT() into ROWCOUNT;
		if ROWCOUNT = 0 then
			set ReturnValue=2;
			ROLLBACK;
			LEAVE return_label;
		end if;
		*/
		set InstanceCount=0;
		select count(*) INTO InstanceCount from _ZezeInstances_;
		if InstanceCount = 0 then
			set emptybinary = BINARY '';
			delete from _ZezeDataWithVersion_ where id=emptybinary;
		end if;
		set ReturnValue=0;
		IF 1=1 THEN
			COMMIT;
		END IF;
		LEAVE return_label;
	end;
